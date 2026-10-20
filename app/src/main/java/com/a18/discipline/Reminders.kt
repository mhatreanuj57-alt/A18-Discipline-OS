package com.a18.discipline

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.*
import com.a18.discipline.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import java.time.*
import java.util.concurrent.TimeUnit

private val Context.alarmStore by preferencesDataStore("scheduled-task-alerts")
object Reminders {
    private const val FIRE="com.a18.discipline.TASK_ALERT"
    private const val REFRESH="daily-refresh"
    const val START_CHANNEL="a18-task-start-v3"
    const val END_CHANNEL="a18-task-end-v3"
    private val registered=stringPreferencesKey("registered")
    private val lock=Mutex()
    fun permitted(context: Context)=Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
    fun precise(context: Context)=Build.VERSION.SDK_INT<31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    private fun intent(context: Context,key: String)=Intent(context,TaskAlarmReceiver::class.java).setAction(FIRE).setData(Uri.parse("a18://task-alert/${Uri.encode(key)}"))
    private fun pending(context: Context,key: String)=PendingIntent.getBroadcast(context,0,intent(context,key),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private suspend fun registry(context: Context)=json.decodeFromString<Map<String,Long>>(context.alarmStore.data.first()[registered] ?: "{}")
    suspend fun reschedule(context: Context,tasks: List<Task>,enabled: Boolean,force: Boolean=false) = lock.withLock {
        prepareChannel(context)
        val old=registry(context);val manager=context.getSystemService(AlarmManager::class.java)
        val desired=if(enabled && permitted(context)) plannedAlerts(tasks).associate{it.key to it.at}.toMutableMap() else mutableMapOf()
        if(enabled && permitted(context)) desired[REFRESH]=LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        old.forEach{(key,at)->if(force || desired[key]!=at)manager.cancel(pending(context,key))}
        desired.forEach{(key,at)->if(force || old[key]!=at) {
            val operation=pending(context,key)
            try {
                if(key==REFRESH)manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,operation)
                else if(precise(context))manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,operation)
                else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,operation)
            } catch(_: SecurityException) { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,operation) }
        }}
        context.alarmStore.edit{it[registered]=json.encodeToString(desired.toMap())}
        // Retire the older WorkManager task jobs so an upgrade cannot double-notify.
        WorkManager.getInstance(context).cancelAllWorkByTag("a18-task-reminder")
    }
    suspend fun rebuild(context: Context,force: Boolean=false) {
        val repo=(context.applicationContext as DisciplineApp).repository
        val tasks=repo.database.entries().all().filter{it.kind=="tasks"}.map{json.decodeFromString<Task>(it.payload)}
        reschedule(context,tasks,repo.settings.first().notifications,force)
    }
    suspend fun receive(context: Context,key: String) {
        val at=lock.withLock {
            val saved=registry(context);val time=saved[key] ?: return
            if(time>System.currentTimeMillis()+1000) return
            context.alarmStore.edit{it[registered]=json.encodeToString(saved-key)}
            time
        }
        val repo=(context.applicationContext as DisciplineApp).repository
        if(key!=REFRESH && repo.settings.first().notifications && permitted(context)) {
            val taskId=json.decodeFromString<List<String>>(key).first()
            val row=repo.database.entries().find("tasks",taskId)
            if(row!=null) {
                val task=json.decodeFromString<Task>(row.payload)
                val alert=plannedAlerts(listOf(task),Instant.ofEpochMilli(at-1).atZone(ZoneId.systemDefault())).find{it.key==key && it.at==at}
                if(alert!=null) notify(context,key,alert.title,alert.message,alert.ending)
            }
        }
        rebuild(context)
    }
    fun prepareChannel(context: Context) {
        val manager=context.getSystemService(NotificationManager::class.java)
        listOf(Triple(START_CHANNEL,"Task starting",R.raw.task_start),Triple(END_CHANNEL,"Task ending",R.raw.task_end)).forEach { (id,name,sound) ->
            val channel=NotificationChannel(id,name,NotificationManager.IMPORTANCE_HIGH).apply {
                description="${name} notifications with your custom sound";enableVibration(true)
                setSound(Uri.parse("android.resource://${context.packageName}/raw/${if(sound==R.raw.task_start) "task_start" else "task_end"}"),AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            }
            manager.createNotificationChannel(channel)
        }
    }
    fun notify(context: Context,key: String,title: String,message: String,ending: Boolean=false) {
        if(!permitted(context))return
        prepareChannel(context)
        val manager=context.getSystemService(NotificationManager::class.java)
        val content=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val channel=if(ending)END_CHANNEL else START_CHANNEL
        val public=NotificationCompat.Builder(context,channel).setSmallIcon(R.drawable.ic_task).setContentTitle("Daymark reminder").setContentText("Open your daily plan.").build()
        manager.notify(key,0,NotificationCompat.Builder(context,channel).setSmallIcon(R.drawable.ic_task).setContentTitle(title).setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)).setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(public).setContentIntent(content).setAutoCancel(true).build())
    }
    fun focus(context: Context,timer: Timer) {
        val work=WorkManager.getInstance(context)
        if(timer.endAt==null){work.cancelUniqueWork("focus");return}
        work.enqueueUniqueWork("focus",ExistingWorkPolicy.REPLACE,OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay((timer.endAt-System.currentTimeMillis()).coerceAtLeast(0),TimeUnit.MILLISECONDS).build())
    }
}
class TaskAlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        val result=goAsync()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            try {
                val key=intent.data?.lastPathSegment
                if(key!=null && intent.action=="com.a18.discipline.TASK_ALERT")Reminders.receive(context,key)
                else Reminders.rebuild(context,force=true)
            } finally {result.finish()}
        }
    }
}
class ReminderWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val repo=(applicationContext as DisciplineApp).repository
        if(inputData.getString("taskId")!=null) return Result.success()
        repo.finishTimer()
        if(repo.settings.first().notifications)Reminders.notify(applicationContext,"focus","Daymark focus timer","Your focus interval has ended.")
        return Result.success()
    }
}
