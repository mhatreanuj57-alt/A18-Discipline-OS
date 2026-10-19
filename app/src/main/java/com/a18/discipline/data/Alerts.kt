package com.a18.discipline.data

import java.time.*
import kotlinx.serialization.encodeToString

data class PlannedAlert(val key: String, val taskId: String, val at: Long, val title: String, val message: String, val ending: Boolean = false)
fun plannedAlerts(tasks: List<Task>, now: ZonedDateTime = ZonedDateTime.now()): List<PlannedAlert> {
    val from=now.toInstant().toEpochMilli();val horizon=now.toLocalDate().plusDays(8).atStartOfDay(now.zone).toInstant().toEpochMilli()
    return tasks.filter{!it.archived && it.status !in listOf("Completed","Missed","Skipped")}.flatMap { task ->
        val reminders=buildList {
            if(task.start.isNotBlank()) add(TaskAlert("start","${task.due}T${task.start}","Time to start ${task.title}. ${task.start}–${task.end}"))
            if(task.end.isNotBlank()) {
                val date=LocalDate.parse(task.due).plusDays(if(task.start.isNotBlank() && LocalTime.parse(task.end)<=LocalTime.parse(task.start)) 1 else 0)
                add(TaskAlert("end","${date}T${task.end}","Time to finish ${task.title}. Record your progress or reschedule unfinished work."))
            }
            if(task.reminder.isNotBlank()) add(TaskAlert("reminder",task.reminder,"Reminder: ${task.title}"))
            addAll(task.alerts)
        }
        reminders.map { alert -> PlannedAlert(json.encodeToString(listOf(task.id,alert.id)),task.id,parseDateTime(alert.at,now.zone).toInstant().toEpochMilli(),task.title,alert.message,alert.id=="end") }
    }.filter{it.at>from && it.at<horizon}.sortedBy{it.at}
}
