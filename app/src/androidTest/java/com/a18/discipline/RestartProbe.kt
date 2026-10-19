package com.a18.discipline

import androidx.test.platform.app.InstrumentationRegistry
import com.a18.discipline.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime

// Explicitly invoked in two separate instrumentation processes; no personal records are changed.
class RestartProbe {
    @Test fun probe() = runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val repo=(instrumentation.targetContext.applicationContext as DisciplineApp).repository
        val mode=InstrumentationRegistry.getArguments().getString("probe")
        if(mode=="notify-write" || mode=="notify-cleanup") {
            val context=instrumentation.targetContext
            val id="a18-internal-notification-probe"
            val manager=context.getSystemService(android.app.NotificationManager::class.java)
            if(mode=="notify-write") {
                assertNull(repo.database.entries().find("tasks",id))
                val time=ZonedDateTime.now().plusSeconds(30).withNano(0)
                repo.saveTask(Task(id=id,title="A18 sound check",due=time.toLocalDate().toString(),start=time.toLocalTime().toString(),end=time.plusSeconds(15).toLocalTime().toString()))
                Reminders.rebuild(context,force=true)
            } else try {
                // A new instrumentation process clears notifications; inspect background delivery with adb first.
                assertNotNull(repo.database.entries().find("tasks",id))
            } finally {
                repo.delete("tasks",id)
                manager.activeNotifications.filter{it.tag?.contains(id)==true}.forEach{manager.cancel(it.tag,it.id)}
                Reminders.rebuild(context,force=true)
            }
            return@runBlocking
        }
        if(mode==null) {
            assertTrue(repo.database.entries().all().filter{it.kind in listOf("privateLogs","journals")}.all{it.encrypted && it.date.isEmpty()})
            assertNull(json.decodeFromString<Backup>(repo.export(false,"")).privateLogs)
            return@runBlocking
        }
        val id="a18-internal-restart-probe"
        if(mode=="write") {
            assertNull(repo.database.entries().find("tasks",id))
            repo.saveTask(Task(id=id,title="Temporary persistence verification",due="2000-01-01").transition("Missed"))
        } else {
            try { val row=repo.database.entries().find("tasks",id)
                assertNotNull("Record must survive process restart",row)
                val task=json.decodeFromString<Task>(row!!.payload)
                assertEquals("Missed",task.status);assertEquals("Missed",task.history.single().status)
            } finally {repo.delete("tasks",id)}
        }
    }
}
