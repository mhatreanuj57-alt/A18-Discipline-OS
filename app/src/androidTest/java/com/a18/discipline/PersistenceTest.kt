package com.a18.discipline

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.a18.discipline.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private fun db(name: String)=Room.databaseBuilder(context,DisciplineDatabase::class.java,name).build()

    @Test fun everyPlanDateIsSeededOnceAndCompletedTasksSurviveDatabaseReopen() = runBlocking {
        val name="test-plan-${uid()}.db"
        var database=db(name)
        try {
            var repo=Repository(context,database);repo.initialize();repo.initialize()
            val original=repo.snapshot.first{it.ready}
            assertEquals(856,original.tasks.size);assertEquals(1386,original.schedules.size);assertEquals(30,original.challenge.size)
            var date=LocalDate.parse(planStart)
            while(date<=LocalDate.parse("2027-02-18")){assertTrue("Missing $date",original.tasks.any{it.due==date.toString()});date=date.plusDays(1)}
            assertTrue(original.tasks.all{it.status=="Not Started" && it.actual==0.0})
            assertFalse(original.tasks.any{it.title.contains("AgroVision",true) || it.notes.contains("AgroVision",true)})
            val task=original.tasks.first();repo.status(task,"Completed")
            database.close();database=db(name);repo=Repository(context,database);repo.initialize()
            val saved=repo.snapshot.first{it.ready}.tasks.find{it.id==task.id}!!
            assertEquals("Completed",saved.status);assertNotNull(saved.completed);assertEquals(1,saved.history.size)
            repo.restore(json.encodeToString(Backup(tasks=listOf(task))),false,"")
            assertEquals("Completed",repo.snapshot.first{it.ready}.tasks.find{it.id==task.id}!!.status)
            val pristine=original.tasks.first{it.id!=task.id}
            repo.restore(json.encodeToString(Backup(tasks=listOf(pristine.transition("Completed")))),false,"")
            assertEquals("Completed",repo.snapshot.first{it.ready}.tasks.find{it.id==pristine.id}!!.status)
            repo.delete("tasks",task.id);repo.initialize();assertNull(database.entries().find("tasks",task.id))
        } finally {database.close();context.deleteDatabase(name)}
    }

    @Test fun removingPreparationWeekendPreservesEditedAndUserCreatedRecords() = runBlocking {
        val name="test-upgrade-${uid()}.db";val database=db(name)
        try {
            val repo=Repository(context,database);repo.initialize()
            val before=repo.snapshot.first{it.ready}
            val legacy=json.decodeFromString<Backup>(context.assets.open("retired-weekend-v1.json").bufferedReader().use{it.readText()})
            legacy.tasks.forEach{repo.saveTask(it)};legacy.schedules.forEach{repo.saveSchedule(it)};legacy.items.forEach{repo.saveItem(it)}
            val saved=legacy.tasks.first().copy(notes="Keep this note",actual=42.0).transition("Completed")
            repo.saveTask(saved)
            val manual=Task(title="My personal record",due="2026-10-18");repo.saveTask(manual)
            database.entries().delete("meta","weekend-retired-v1")
            repo.initialize();repo.initialize()
            val after=repo.snapshot.first{it.ready}
            assertEquals(858,after.tasks.size);assertEquals(1387,after.schedules.size)
            assertEquals(saved,after.tasks.find{it.id==saved.id});assertEquals(manual,after.tasks.find{it.id==manual.id})
            assertTrue(after.tasks.containsAll(before.tasks))
            assertEquals(30,after.challenge.size);assertEquals("2026-10-19",after.challenge.minOf{it.id})
            repo.restore(json.encodeToString(legacy),false,"")
            assertEquals(858,repo.snapshot.first{it.ready}.tasks.size)
            val restoredDb=Room.inMemoryDatabaseBuilder(context,DisciplineDatabase::class.java).build()
            try {
                val restoredRepo=Repository(context,restoredDb);restoredRepo.initialize()
                val noted=legacy.tasks[1].copy(notes="User notes without a status change")
                val block=legacy.schedules.first{it.taskId==noted.id}
                restoredRepo.restore(json.encodeToString(Backup(tasks=listOf(noted,manual),schedules=listOf(block))),false,"")
                assertEquals(noted,json.decodeFromString<Task>(restoredDb.entries().find("tasks",noted.id)!!.payload))
                assertNotNull(restoredDb.entries().find("schedules",block.id))
            } finally {restoredDb.close()}
        } finally {database.close();context.deleteDatabase(name)}
    }

    @Test fun focusCompletionIsLoggedExactlyOnce() = runBlocking {
        val name="test-timer-${uid()}.db";val database=db(name)
        try {
            val repo=Repository(context,database);val task=Task(title="Device timer test",due="2026-10-19");repo.saveTask(task)
            repo.saveTimer(Timer(duration=60,remaining=60,endAt=System.currentTimeMillis()-1000,taskId=task.id,started=true))
            repo.finishTimer();repo.finishTimer()
            val rows=database.entries().all();assertEquals(1,rows.count{it.kind=="logs"})
            val saved=json.decodeFromString<Task>(database.entries().find("tasks",task.id)!!.payload)
            assertEquals(1.0,saved.actual,0.001)
        } finally {database.close();context.deleteDatabase(name)}
    }

    @Test fun privateRowsAreEncryptedAndOrdinaryExportsExcludeThem() = runBlocking {
        val name="test-private-${uid()}.db";val database=db(name)
        try {
            val repo=Repository(context,database);val record=PrivateLog(trigger="fixture-secret-trigger",date="2026-10-19")
            repo.savePrivate(record);repo.saveJournal(Journal("2026-10-19",accomplished="fixture-private-journal"))
            val row=database.entries().find("privateLogs",record.id)!!
            assertTrue(row.encrypted);assertEquals("",row.date);assertFalse(row.payload.contains("fixture-secret-trigger"))
            assertEquals(record,repo.privateLogs().single())
            val ordinary=json.decodeFromString<Backup>(repo.export(false,""));assertNull(ordinary.privateLogs);assertNull(ordinary.journals)
            val sensitive=repo.export(true,"test passphrase for device")
            assertFalse(sensitive.contains("fixture-secret-trigger"))
            val decoded=json.decodeFromString<Backup>(BackupCrypto.decode(sensitive,"test passphrase for device"));assertEquals(record,decoded.privateLogs!!.single())
            repo.deletePrivate();assertTrue(repo.privateLogs().isEmpty());assertEquals("fixture-private-journal",repo.journal("2026-10-19").accomplished)
        } finally {database.close();context.deleteDatabase(name)}
    }

    @Test fun recurringInstancesRemainUniqueAndNeverCopyCompletion() = runBlocking {
        val name="test-repeat-${uid()}.db";val database=db(name)
        try {
            val repo=Repository(context,database);repo.initialize();val task=Task(title="Recurring test",due=LocalDate.now().toString(),repeat="daily",status="Completed",actual=25.0)
            repo.saveTask(task);repo.initialize();repo.initialize()
            val copies=repo.snapshot.first{it.ready}.tasks.filter{it.series==task.series && it.id!=task.id}
            assertEquals(30,copies.size);assertEquals(copies.size,copies.map{it.id}.distinct().size);assertTrue(copies.all{it.status=="Not Started" && it.actual==0.0})
        } finally {database.close();context.deleteDatabase(name)}
    }
}
