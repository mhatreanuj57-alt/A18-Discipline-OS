package com.a18.discipline

import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import com.a18.discipline.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class AlarmDeliveryTest {
    @Test fun scheduledStartsAndEndsPostOnceWithDifferentCustomSounds() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue("Allow Android notifications before this test",Reminders.permitted(context))
        assertTrue("Allow Alarms & reminders before this test",Reminders.precise(context))
        val repo=(context.applicationContext as DisciplineApp).repository
        assertTrue("Enable study & task notifications before this test",repo.settings.first().notifications)
        val time=ZonedDateTime.now().plusSeconds(8).withNano(0)
        val task=Task(title="A18 notification test",due=time.toLocalDate().toString(),start=time.toLocalTime().toString(),end=time.plusSeconds(8).toLocalTime().toString())
        val manager=context.getSystemService(NotificationManager::class.java)
        val alerts=plannedAlerts(listOf(task))
        try {
            repo.saveTask(task);Reminders.rebuild(context,force=true)
            for(alert in alerts) {
                val until=System.currentTimeMillis()+25000
                while(System.currentTimeMillis()<until && manager.activeNotifications.none{it.tag==alert.key})delay(250)
                val delivered=manager.activeNotifications.filter{it.tag==alert.key}
                assertEquals(1,delivered.size)
                assertEquals(task.title,delivered.single().notification.extras.getString("android.title"))
                val channel=manager.getNotificationChannel(if(alert.ending)Reminders.END_CHANNEL else Reminders.START_CHANNEL)
                assertEquals(channel.id,delivered.single().notification.channelId)
                assertEquals(NotificationManager.IMPORTANCE_HIGH,channel.importance)
                val uri=android.net.Uri.parse("android.resource://${context.packageName}/raw/${if(alert.ending)"task_end" else "task_start"}")
                assertEquals(uri,channel.sound)
                context.contentResolver.openInputStream(uri)!!.use{assertTrue(it.read()!=-1)}
                Reminders.receive(context,alert.key)
                assertEquals(1,manager.activeNotifications.count{it.tag==alert.key})
            }
        } finally {
            repo.delete("tasks",task.id);alerts.forEach{manager.cancel(it.key,0)};Reminders.rebuild(context,force=true)
        }
    }
}
