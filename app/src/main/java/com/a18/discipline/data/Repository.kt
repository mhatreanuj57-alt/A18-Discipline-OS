package com.a18.discipline.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.time.*

private val Context.preferences by preferencesDataStore("settings")
data class Snapshot(val tasks: List<Task> = emptyList(), val logs: List<Activity> = emptyList(), val items: List<Item> = emptyList(),
    val schedules: List<Schedule> = emptyList(), val challenge: List<Challenge> = emptyList(), val timer: Timer = Timer(), val ready: Boolean = false)

class Repository(private val context: Context, val database: DisciplineDatabase, private val vault: PrivateVault = PrivateVault()) {
    private val dao = database.entries()
    private val settingsKey = stringPreferencesKey("settings-v1")
    val settings = context.preferences.data.map { json.decodeFromString<Settings>(it[settingsKey] ?: json.encodeToString(Settings())) }
    val snapshot: Flow<Snapshot> = dao.observe().map { rows ->
        fun <T> read(kind: String, decode: (String) -> T) = rows.filter { it.kind == kind }.map { decode(it.payload) }
        Snapshot(read("tasks") { json.decodeFromString<Task>(it) }, read("logs") { json.decodeFromString<Activity>(it) },
            read("items") { json.decodeFromString<Item>(it) },read("schedules") { json.decodeFromString<Schedule>(it) },
            read("challenge") { json.decodeFromString<Challenge>(it) },rows.firstOrNull { it.kind == "timer" }?.let { json.decodeFromString<Timer>(it.payload) } ?: Timer(), rows.any { it.kind == "meta" })
    }.flowOn(Dispatchers.IO)

    suspend fun saveSettings(value: Settings) { Backup(settings=value).validate(); context.preferences.edit { it[settingsKey] = json.encodeToString(value) } }
    private fun entry(kind: String, id: String, date: String, value: String, secure: Boolean = false) =
        Entry(kind,id,if (secure) "" else date,if (secure) vault.encrypt(value,"$kind:$id") else projectLabels(kind,value),secure)
    private fun plain(row: Entry) = if (row.encrypted) vault.decrypt(row.payload,"${row.kind}:${row.id}") else row.payload

    suspend fun initialize() = withContext(Dispatchers.IO) {
        database.withTransaction {
            if (dao.find("meta","plan-v1") == null) {
                val plan = json.decodeFromString<Backup>(context.assets.open("plan.json").bufferedReader().use { it.readText() }).validate()
                dao.insert(rows(plan,false))
                dao.put(Entry("meta","plan-v1","","1"))
            }
            if (dao.find("meta","project-label-v1") == null) {
                dao.all().filter { !it.encrypted && it.kind in setOf("tasks","schedules","items","logs") }.forEach { old ->
                    val updated = projectLabels(old.kind,old.payload)
                    if(updated != old.payload) dao.put(old.copy(payload=updated))
                }
                dao.put(Entry("meta","project-label-v1","","1"))
            }
            if (dao.find("meta","weekend-retired-v1") == null) {
                unusedWeekendDefaults(dao.all()).forEach{dao.delete(it.kind,it.id)}
                dao.put(Entry("meta","weekend-retired-v1","","1"))
            }
            if(dao.find("meta","evening-plan-v1")==null) {
                dao.all().filter{it.kind=="tasks"}.forEach { row ->
                    val task=json.decodeFromString<Task>(row.payload)
                    if(task.due in planStart..planEnd && !task.id.startsWith("evening-v1:")) dao.put(row.copy(payload=json.encodeToString(task.copy(archived=true))))
                }
                dao.all().filter{it.kind=="schedules"}.forEach { row ->
                    val block=json.decodeFromString<Schedule>(row.payload)
                    if(block.date in planStart..planEnd && !block.id.startsWith("evening-slot-v1:") && !block.id.startsWith("routine-v1:")) dao.put(row.copy(payload=json.encodeToString(block.copy(archived=true))))
                }
                val plan=json.decodeFromString<Backup>(context.assets.open("plan.json").bufferedReader().use{it.readText()}).validate()
                dao.insert(rows(plan,false));dao.put(Entry("meta","evening-plan-v1","","1"))
            }
            materializeRecurring(LocalDate.now())
        }
        finishTimer()
    }

    suspend fun saveTask(value: Task) = database.withTransaction {
        Backup(tasks=listOf(value)).validate()
        dao.put(entry("tasks",value.id,value.due,json.encodeToString(value)))
        value.scheduleId?.let { id -> dao.find("schedules",id)?.let { row ->
            val block = json.decodeFromString<Schedule>(row.payload)
            val updated = block.copy(date=value.due,title=value.title,start=value.start.ifBlank { block.start },end=value.end.ifBlank { block.end },done=value.status=="Completed",archived=value.archived)
            Backup(schedules=listOf(updated)).validate(); dao.put(entry("schedules",id,updated.date,json.encodeToString(updated)))
        } }
    }
    suspend fun status(task: Task, status: String, date: String = task.due) = saveTask(task.transition(status,date))
    suspend fun saveLog(value: Activity) { Backup(logs=listOf(value)).validate(); dao.put(entry("logs",value.id,value.date,json.encodeToString(value))) }
    suspend fun saveItem(value: Item) { Backup(items=listOf(value)).validate(); dao.put(entry("items",value.id,value.due,json.encodeToString(value))) }
    suspend fun saveSchedule(value: Schedule) = database.withTransaction {
        Backup(schedules=listOf(value)).validate()
        dao.put(entry("schedules",value.id,value.date,json.encodeToString(value)))
        value.taskId?.let { id -> dao.find("tasks",id)?.let { row ->
            val task = json.decodeFromString<Task>(row.payload)
            val updated = task.copy(title=value.title,due=value.date,start=value.start,end=value.end,deadline="${value.date}T${value.end}",archived=value.archived)
            dao.put(entry("tasks",id,updated.due,json.encodeToString(updated)))
        } }
    }
    suspend fun toggleSchedule(value: Schedule) {
        val task = value.taskId?.let { dao.find("tasks",it) }?.let { json.decodeFromString<Task>(it.payload) }
        if (task != null) status(task,if (value.done) "Not Started" else "Completed") else saveSchedule(value.copy(done=!value.done))
    }
    suspend fun saveChallenge(value: Challenge) { Backup(challenge=listOf(value)).validate(); dao.put(entry("challenge",value.id,value.id,json.encodeToString(value))) }
    suspend fun saveJournal(value: Journal) { Backup(journals=listOf(value)).validate(); dao.put(entry("journals",value.id,value.id,json.encodeToString(value),true)) }
    suspend fun journal(date: String) = dao.find("journals",date)?.let { json.decodeFromString<Journal>(plain(it)) } ?: Journal(date)
    suspend fun privateLogs(): List<PrivateLog> = dao.all().filter { it.kind == "privateLogs" }.map { json.decodeFromString<PrivateLog>(plain(it)) }.sortedByDescending { it.date }
    suspend fun savePrivate(value: PrivateLog) { Backup(privateLogs=listOf(value)).validate(); dao.put(entry("privateLogs",value.id,value.date,json.encodeToString(value),true)) }
    suspend fun deletePrivate() = dao.deletePrivate()
    suspend fun delete(kind: String, id: String) = database.withTransaction {
        if (kind == "tasks") dao.find(kind,id)?.let { json.decodeFromString<Task>(it.payload).scheduleId?.let { schedule -> dao.delete("schedules",schedule) } }
        dao.delete(kind,id)
    }

    private suspend fun materializeRecurring(today: LocalDate) {
        val tasks = dao.all().filter { it.kind == "tasks" }.map { json.decodeFromString<Task>(it.payload) }
        tasks.filter { it.repeat != "none" && !it.archived }.groupBy { it.series }.forEach { (_, series) ->
            val last = series.maxBy { it.due }
            var next = LocalDate.parse(last.due).plusDays(if (last.repeat == "daily") 1 else 7)
            val horizon = today.plusDays(30)
            val created = mutableListOf<Entry>()
            // Bound recovery after long absences; no original or completed record is overwritten.
            while (next <= horizon && created.size < 4000) {
                val date = next.toString(); val id = "repeat:${last.series}:$date"
                val t = last.copy(id=id,due=date,status="Not Started",actual=0.0,completed=null,history=emptyList(),scheduleId=null,
                    originalDue=null,originalDeadline=null,subtasks=last.subtasks.map { it.copy(done=false) },
                    deadline=if(last.deadline.isBlank()) "" else date+last.deadline.substring(10),reminder=if(last.reminder.isBlank()) "" else date+last.reminder.substring(10),alerts=last.alerts.map{it.copy(at=date+it.at.substring(10))})
                created += entry("tasks",id,date,json.encodeToString(t)); next = next.plusDays(if (last.repeat == "daily") 1 else 7)
            }
            dao.insert(created)
        }
    }
    suspend fun saveTimer(timer: Timer) { dao.put(entry("timer","active","",json.encodeToString(timer))) }
    suspend fun finishTimer(now: Long = System.currentTimeMillis()) = database.withTransaction {
        val timer = dao.find("timer","active")?.let { json.decodeFromString<Timer>(it.payload) } ?: return@withTransaction
        if (timer.endAt == null || timer.endAt > now) return@withTransaction
        if (timer.mode == "focus") {
            val id = "timer:${timer.sessionId}"
            if (dao.find("logs",id) == null) {
                val task = dao.find("tasks",timer.taskId)?.let { json.decodeFromString<Task>(it.payload) }
                val end = Instant.ofEpochMilli(timer.endAt)
                val log = Activity(id=id,date=end.atZone(ZoneId.systemDefault()).toLocalDate().toString(),kind=timer.kind,title=task?.title ?: "Focus session",
                    minutes=timer.duration/60.0,start=end.minusSeconds(timer.duration.toLong()).toString(),end=end.toString(),taskId=timer.taskId,source="timer",sessionId=timer.sessionId,subject=timer.subject)
                saveLog(log)
                task?.let { dao.put(entry("tasks",it.id,it.due,json.encodeToString(it.copy(actual=it.actual+log.minutes)))) }
            }
        }
        saveTimer(timer.copy(remaining=0,endAt=null,started=false,cycles=timer.cycles+if(timer.mode=="focus") 1 else 0))
    }

    private fun rows(backup: Backup, sensitive: Boolean): List<Entry> = buildList {
        backup.tasks.forEach { add(entry("tasks",it.id,it.due,json.encodeToString(if(it.id.startsWith("plan-v1:") && it.due in planStart..planEnd) it.copy(archived=true) else it))) }
        backup.logs.forEach { add(entry("logs",it.id,it.date,json.encodeToString(it))) }
        backup.items.forEach { add(entry("items",it.id,it.due,json.encodeToString(it))) }
        backup.schedules.forEach { add(entry("schedules",it.id,it.date,json.encodeToString(if(it.taskId?.startsWith("plan-v1:")==true && it.date in planStart..planEnd) it.copy(archived=true) else it))) }
        backup.challenge.forEach { add(entry("challenge",it.id,it.id,json.encodeToString(it))) }
        if (sensitive) {
            backup.privateLogs?.forEach { add(entry("privateLogs",it.id,it.date,json.encodeToString(it),true)) }
            backup.journals?.forEach { add(entry("journals",it.id,it.id,json.encodeToString(it),true)) }
        }
        backup.drafts?.forEach { d ->
            val id = d.getValue("id").jsonPrimitive.content
            val secure = d["sensitive"]?.jsonPrimitive?.booleanOrNull == true || id.startsWith("private:") || id.startsWith("journal:")
            if (!secure || sensitive) add(entry("drafts",id,"",d.toString(),secure))
        }
    }
    private fun retiredWeekendRows() = rows(json.decodeFromString<Backup>(context.assets.open("retired-weekend-v1.json").bufferedReader().use{it.readText()}).validate(),false)
    private fun unusedWeekendDefaults(entries: List<Entry>): Set<Entry> {
        val retired=retiredWeekendRows().associateBy{"${it.kind}:${it.id}"}
        val current=entries.associateBy{"${it.kind}:${it.id}"}
        val usedTasks=entries.filter{it.kind=="logs"}.map{json.decodeFromString<Activity>(it.payload).taskId}.toSet() +
            current["timer:active"]?.let{json.decodeFromString<Timer>(it.payload).taskId}.orEmpty()
        val keptKeys=mutableSetOf<String>()
        retired.values.filter{it.kind=="tasks"}.forEach { row ->
            val task=json.decodeFromString<Task>(row.payload)
            val linked=setOf("tasks:${task.id}","schedules:${task.scheduleId}")
            if(task.id in usedTasks || linked.any{current[it]!=null && current[it]!=retired[it]}) keptKeys.addAll(linked)
        }
        // Remove only exact bundled defaults; edits, outcomes and linked activity stay intact.
        return retired.filter{(key,row)->key !in keptKeys && current[key]==row}.values.toSet()
    }
    suspend fun export(sensitive: Boolean, password: String): String = withContext(Dispatchers.IO) {
        val rows = dao.all()
        inlineDecode(rows,sensitive).let { backup ->
            val value = json.encodeToString(backup.copy(settings=settings.first().copy(privateEnabled=false,notifications=false)))
            if (sensitive || password.isNotBlank()) BackupCrypto.encrypt(value,password) else value
        }
    }
    private fun inlineDecode(rows: List<Entry>, sensitive: Boolean): Backup {
        fun <T> read(kind: String, decode: (String) -> T) = rows.filter { it.kind == kind }.map { decode(plain(it)) }
        return Backup(tasks=read("tasks") { json.decodeFromString<Task>(it) },logs=read("logs") { json.decodeFromString<Activity>(it) },
            items=read("items") { json.decodeFromString<Item>(it) },schedules=read("schedules") { json.decodeFromString<Schedule>(it) },
            challenge=read("challenge") { json.decodeFromString<Challenge>(it) },
            privateLogs=if(sensitive) read("privateLogs") { json.decodeFromString<PrivateLog>(it) } else null,
            journals=if(sensitive) read("journals") { json.decodeFromString<Journal>(it) } else null,
            drafts=rows.filter { it.kind=="drafts" && (sensitive || (!it.encrypted && !it.id.startsWith("private:") && !it.id.startsWith("journal:"))) }.map { json.parseToJsonElement(plain(it)).jsonObject })
    }
    suspend fun restore(value: String, sensitive: Boolean, password: String) = withContext(Dispatchers.IO) {
        require(value.length <= 50_000_000) { "Maximum backup size is 50 MB" }
        val backup = json.decodeFromString<Backup>(BackupCrypto.decode(value,password)).validate()
        val imported = rows(backup,sensitive)
        val retired = unusedWeekendDefaults(imported)
        val incoming = imported.filterNot{it in retired}
        // Replace only untouched bundled defaults; user-edited rows always win conflicts.
        val defaults = json.decodeFromString<Backup>(context.assets.open("plan.json").bufferedReader().use { it.readText() })
        val defaultTasks = defaults.tasks.associateBy { it.id }
        val defaultBlocks = defaults.schedules.associateBy { it.id }
        val defaultItems = defaults.items.associateBy { it.id }
        database.withTransaction {
            incoming.forEach { row ->
                val old = dao.find(row.kind,row.id)
                val untouched = old != null && when(row.kind) {
                    "tasks" -> defaultTasks[row.id]?.let { original -> json.decodeFromString<Task>(old.payload).copy(created=original.created)==original } ?: false
                    "schedules" -> defaultBlocks[row.id]?.let { json.decodeFromString<Schedule>(old.payload)==it } ?: false
                    "items" -> defaultItems[row.id]?.let { json.decodeFromString<Item>(old.payload)==it } ?: false
                    "challenge" -> json.decodeFromString<Challenge>(old.payload).let { it.checks.isEmpty() && it.review.isBlank() }
                    else -> false
                }
                if(old==null || untouched) dao.put(row)
            }
            dao.put(Entry("meta","plan-v1","","1"))
        }
        backup.settings?.let { imported -> saveSettings(imported.copy(privateEnabled=false,notifications=false)) }
    }
}
