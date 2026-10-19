package com.a18.discipline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.a18.discipline.data.*
import java.time.*

@Composable fun TaskEditor(seed: Task,close: () -> Unit,save: (Task) -> Unit,delete: () -> Unit) {
    var t by remember { mutableStateOf(seed) }; var estimate by rememberSaveable { mutableStateOf(seed.estimate.toString()) }
    var actual by rememberSaveable { mutableStateOf(seed.actual.toString()) }; var sub by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Editor("Task details",close,{
        runCatching { val candidate=t.copy(estimate=number(estimate),actual=number(actual)); Backup(tasks=listOf(candidate)).validate(); candidate }
            .onSuccess { save(it) }.onFailure { error=it.message }
    },delete,error) {
        Field("Task title",t.title,{t=t.copy(title=it)})
        DateButton(t.due,{t=t.transition(t.status,it)})
        Choice("Category",t.category,categories,{t=t.copy(category=it)})
        Choice("Priority",t.priority,listOf("High","Medium","Low"),{t=t.copy(priority=it)})
        Choice("Status",t.status,statuses,{t=t.transition(it)})
        Choice("Repeats",t.repeat,listOf("none","daily","weekly"),{t=t.copy(repeat=it)})
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { TimeButton("Start",t.start,{t=t.copy(start=it)}); TimeButton("End",t.end,{t=t.copy(end=it)}) }
        TimeButton("Deadline",t.deadline.takeIf{it.length>=16}?.substring(11,16) ?: "",{t=t.copy(deadline="${t.due}T$it")})
        TimeButton("Reminder",t.reminder.takeIf{it.length>=16}?.substring(11,16) ?: "",{t=t.copy(reminder="${t.due}T$it")})
        if(t.reminder.isNotBlank()) TextButton(onClick={t=t.copy(reminder="")}){Text("Clear reminder")}
        Text("Start and end notifications play your two custom sounds. A separate reminder adds another notification; it does not replace the start.",style=MaterialTheme.typography.bodySmall)
        t.alerts.forEach{alert ->
            TimeButton("Extra alert",alert.at.substring(11,16),{time->t=t.copy(alerts=t.alerts.map{if(it.id==alert.id)it.copy(at="${t.due}T$time") else it})})
            Field("Alert message",alert.message,{text->t=t.copy(alerts=t.alerts.map{if(it.id==alert.id)it.copy(message=text) else it})})
            TextButton(onClick={t=t.copy(alerts=t.alerts.filterNot{it.id==alert.id})}){Text("Remove extra alert")}
        }
        TextButton(onClick={t=t.copy(alerts=t.alerts+TaskAlert(at="${t.due}T${t.start.ifBlank{"20:00"}}"))}){Text("Add extra alert")}
        Toggle("Keep in Previous plan",t.archived,{t=t.copy(archived=it)})
        Field("Estimated minutes",estimate,{estimate=it}); Field("Actual minutes",actual,{actual=it})
        Field("Notes",t.notes,{t=t.copy(notes=it)},3)
        t.subtasks.forEach { task -> Row { Box(Modifier.weight(1f)){ Check(task.title,task.done,{done -> t=t.copy(subtasks=t.subtasks.map { if(it.id==task.id) it.copy(done=done) else it }) }) }; TextButton(onClick={t=t.copy(subtasks=t.subtasks.filterNot{it.id==task.id})}){Text("Remove")} } }
        Field("New subtask",sub,{sub=it}); TextButton(onClick={ if(sub.isNotBlank()) { t=t.copy(subtasks=t.subtasks+Subtask(title=sub.trim())); sub="" } }){Text("Add subtask")}
        if(t.history.isNotEmpty()) { Text("Activity history",style=MaterialTheme.typography.titleSmall); t.history.takeLast(10).reversed().forEach { Text("${it.at.take(16)} · ${it.status} · ${it.due}",style=MaterialTheme.typography.bodySmall) } }
    }
}

@Composable fun ActivityEditor(seed: Activity,close: () -> Unit,save: (Activity) -> Unit,delete: () -> Unit,tasks: List<Task>) {
    var v by remember { mutableStateOf(seed) }; var minutes by rememberSaveable { mutableStateOf(seed.minutes.toString()) }
    var start by rememberSaveable { mutableStateOf(seed.start.takeIf{it.isNotBlank()}?.let{parseDateTime(it).toLocalTime().toString().take(5)} ?: "") }
    var end by rememberSaveable { mutableStateOf(seed.end.takeIf{it.isNotBlank()}?.let{parseDateTime(it).toLocalTime().toString().take(5)} ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    Editor("Activity log",close,{
        runCatching {
            val begin=if(start.isBlank()) null else LocalDate.parse(v.date).atTime(LocalTime.parse(start)).atZone(ZoneId.systemDefault())
            var stop=if(end.isBlank()) null else LocalDate.parse(v.date).atTime(LocalTime.parse(end)).atZone(ZoneId.systemDefault())
            if(begin!=null && stop!=null && stop<begin) stop=stop.plusDays(1)
            val candidate=v.copy(minutes=if(begin!=null && stop!=null) Duration.between(begin,stop).toMinutes().toDouble() else number(minutes,1440.0),start=begin?.toInstant()?.toString() ?: "",end=stop?.toInstant()?.toString() ?: "")
            Backup(logs=listOf(candidate)).validate(); candidate
        }.onSuccess(save).onFailure { error=it.message }
    },delete,error) {
        DateButton(v.date,{v=v.copy(date=it)})
        Choice("Activity",v.kind,logKinds,{v=v.copy(kind=it)})
        Field("Title / game / app",v.title,{v=v.copy(title=it)})
        Field("Minutes (or set start and stop)",minutes,{minutes=it})
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){TimeButton("Started",start,{start=it});TimeButton("Stopped",end,{end=it})}
        if(start.isNotBlank() || end.isNotBlank()) TextButton(onClick={start="";end=""}){Text("Use minutes instead")}
        Field("Subject or skill",v.subject,{v=v.copy(subject=it)})
        val options=tasks.filter{it.due==v.date}
        Choice("Related / delayed task",options.find{it.id==v.taskId}?.title ?: "None",listOf("None")+options.map{it.title},{ title -> v=v.copy(taskId=options.find{it.title==title}?.id ?: "") })
        Field("Notes / responsibilities affected",v.notes,{v=v.copy(notes=it)},3)
        Text("Manual entry only. This app does not detect phone or game usage.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable fun ItemEditor(seed: Item,groups: List<String>,kinds: List<String>,close: () -> Unit,save: (Item) -> Unit,delete: () -> Unit) {
    var v by remember { mutableStateOf(seed) }; var progress by rememberSaveable { mutableStateOf(seed.progress.toString()) }; var target by rememberSaveable { mutableStateOf(seed.target.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    Editor("Planner details",close,{
        runCatching { val candidate=v.copy(progress=number(progress,100.0),target=number(target)); Backup(items=listOf(candidate)).validate(); candidate }.onSuccess(save).onFailure{error=it.message}
    },delete,error) {
        Field("Title",v.title,{v=v.copy(title=it)})
        Choice("Type",v.kind,kinds,{v=v.copy(kind=it)})
        Choice("Workspace",v.group,groups,{v=v.copy(group=it)})
        DateButton(v.due,{v=v.copy(due=it)})
        Field("Completion percentage",progress,{progress=it}); Field("Target (sessions / repetitions)",target,{target=it})
        Field("Notes / commit hash / revision plan",v.notes,{v=v.copy(notes=it)},4)
        Text("Recorded check-ins: ${v.checks.size}")
    }
}

@Composable fun ScheduleEditor(seed: Schedule,close: () -> Unit,save: (Schedule) -> Unit,delete: () -> Unit) {
    var v by remember { mutableStateOf(seed) }; var error by remember { mutableStateOf<String?>(null) }
    Editor("Edit timetable",close,{runCatching{Backup(schedules=listOf(v)).validate()}.onSuccess{save(v)}.onFailure{error=it.message}},delete,error) {
        Field("Activity",v.title,{v=v.copy(title=it)}); DateButton(v.date,{v=v.copy(date=it)})
        TimeButton("Start",v.start,{v=v.copy(start=it)}); TimeButton("End",v.end,{v=v.copy(end=it)})
        Choice("Category",v.kind,listOf("Routine","College","Study","Coding","Project","Health","Break"),{v=v.copy(kind=it)})
    }
}

@Composable fun PrivateEditor(seed: PrivateLog,close: () -> Unit,save: (PrivateLog) -> Unit,delete: () -> Unit) {
    var v by remember { mutableStateOf(seed) }; var minutes by rememberSaveable { mutableStateOf(seed.minutes.toString()) }
    var frequency by rememberSaveable { mutableStateOf(seed.frequency?.toString() ?: "") }; var urges by rememberSaveable { mutableStateOf(seed.urges?.toString() ?: "") }; var error by remember { mutableStateOf<String?>(null) }
    Editor("Private check-in",close,{
        runCatching { val candidate=v.copy(minutes=number(minutes,1440.0),frequency=if(frequency.isBlank()) null else frequency.toInt(),urges=if(urges.isBlank()) null else urges.toInt()); Backup(privateLogs=listOf(candidate)).validate(); candidate }.onSuccess(save).onFailure{error=it.message}
    },delete,error) {
        Text("Optional, private and nonjudgmental. Masturbation is not inherently harmful. Track effects on responsibilities and wellbeing, without diagnoses.")
        DateButton(v.date,{v=v.copy(date=it)}); Toggle("Viewed pornography",v.viewed,{v=v.copy(viewed=it)})
        Field("Approximate minutes",minutes,{minutes=it}); Field("Masturbation frequency (optional)",frequency,{frequency=it}); Field("Urges 0–10 (optional)",urges,{urges=it})
        Choice("Effect on responsibilities",v.impact,listOf("none","studies","sleep","both","unsure"),{v=v.copy(impact=it)})
        Field("Triggers (optional)",v.trigger,{v=v.copy(trigger=it)}); Field("Effect on sleep / studies",v.effect,{v=v.copy(effect=it)},2); Field("Healthier alternatives tried",v.alternative,{v=v.copy(alternative=it)},2)
    }
}
