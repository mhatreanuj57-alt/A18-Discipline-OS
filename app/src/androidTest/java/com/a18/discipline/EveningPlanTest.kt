package com.a18.discipline

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.a18.discipline.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class EveningPlanTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun everyDayHasSevenSlotsExceptTonightsTwoWorkSessions() {
        val plan=json.decodeFromString<Backup>(context.assets.open("plan.json").bufferedReader().use{it.readText()}).validate()
        assertEquals(856,plan.tasks.size);assertEquals(856,plan.tasks.map{it.id}.distinct().size)
        var date=LocalDate.parse(planStart)
        val times=listOf("19:00","19:45","20:30","20:50","21:35","22:05","22:30")
        while(date<=LocalDate.parse(planEnd)) {
            val tasks=plan.tasks.filter{it.due==date.toString()}.sortedBy{it.start}
            assertEquals(if(date.toString()=="2026-10-19") listOf("20:00","21:45") else times,tasks.map{it.start})
            date=date.plusDays(1)
        }
        val start=ZonedDateTime.of(2026,10,19,18,0,0,0,ZoneId.of("Asia/Kolkata"))
        val alerts=plannedAlerts(plan.tasks,start).filter{Instant.ofEpochMilli(it.at).atZone(start.zone).toLocalDate()==start.toLocalDate()}
        assertEquals(listOf("20:00","21:40","21:45","21:45","22:55","23:00"),alerts.map{Instant.ofEpochMilli(it.at).atZone(start.zone).toLocalTime().toString()})
    }
    @Test fun upgradeArchivesEarlierTasksWithoutLosingOutcomesOrPrivateData() = runBlocking {
        val database=Room.inMemoryDatabaseBuilder(context,DisciplineDatabase::class.java).build()
        try {
            val repo=Repository(context,database);repo.initialize()
            val old=Task(id="plan-v1:2026-10-19:legacy",title="Earlier responsibility",due="2026-10-19",start="18:00",end="19:00",deadline="2026-10-19T19:00",scheduleId="legacy-block",notes="Keep my note",actual=37.0).transition("Completed")
            repo.saveTask(old);repo.saveSchedule(Schedule(id="legacy-block",taskId=old.id,title=old.title,date=old.due,start=old.start,end=old.end,done=true))
            val personal=Task(title="Previously added task",due="2026-10-20");repo.saveTask(personal)
            val log=PrivateLog(trigger="private fixture");repo.savePrivate(log)
            val encrypted=database.entries().find("privateLogs",log.id)
            database.entries().delete("meta","evening-plan-v1")
            repo.initialize();repo.initialize()
            val data=repo.snapshot.first{it.ready}
            assertEquals(856,data.tasks.count{!it.archived})
            assertEquals(old.copy(archived=true),data.tasks.find{it.id==old.id})
            assertEquals(personal.copy(archived=true),data.tasks.find{it.id==personal.id})
            assertTrue(data.schedules.find{it.id=="legacy-block"}!!.archived)
            assertEquals(encrypted,database.entries().find("privateLogs",log.id))
            val backup=json.decodeFromString<Backup>(repo.export(false,""))
            assertEquals(old.copy(archived=true),backup.tasks.find{it.id==old.id})
        } finally{database.close()}
    }
}
