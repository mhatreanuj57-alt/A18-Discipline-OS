package com.a18.discipline

import com.a18.discipline.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class AlertsTest {
    private val now=ZonedDateTime.of(2026,10,19,18,0,0,0,ZoneId.of("Asia/Kolkata"))
    @Test fun tonightHasStartsEndsAndTwoRequestedCheckIns() {
        val tasks=listOf(
            Task(title="Assignment Work",due="2026-10-19",start="20:00",end="21:45",alerts=listOf(TaskAlert("warning","2026-10-19T21:40","Switch soon"))),
            Task(title="GitHub Work",due="2026-10-19",start="21:45",end="23:00",alerts=listOf(TaskAlert("warning","2026-10-19T22:55","Save work")))
        )
        val alerts=plannedAlerts(tasks,now)
        assertEquals(listOf("20:00","21:40","21:45","21:45","22:55","23:00"),alerts.map{Instant.ofEpochMilli(it.at).atZone(now.zone).toLocalTime().toString()})
        assertEquals(6,alerts.map{it.key}.distinct().size)
        assertEquals(2,alerts.count{it.ending})
        assertEquals(4,plannedAlerts(tasks,now.withHour(21).withMinute(40)).size)
    }
    @Test fun dailyStartsAreUniqueAndArchivedOrCompletedTasksDoNotAlert() {
        val starts=listOf("19:00","19:45","20:30","20:50","21:35","22:05","22:30")
        val ends=listOf("19:45","20:30","20:50","21:35","22:05","22:30","23:00")
        val tasks=starts.mapIndexed{i,start->Task(title="Task $start",due="2026-10-20",start=start,end=ends[i])}
        assertEquals(14,plannedAlerts(tasks,now).size)
        assertEquals(10,plannedAlerts(tasks.mapIndexed{i,t->when(i){0->t.copy(archived=true);1->t.copy(status="Completed");else->t}},now).size)
        assertTrue(plannedAlerts(tasks.map{it.copy(due="2026-11-20")},now).isEmpty())
    }
    @Test fun overnightEndingUsesTheNextDateAndSkippedTasksAreSilent() {
        val task=Task(title="Late work",due="2026-10-19",start="23:30",end="00:15")
        val end=plannedAlerts(listOf(task),now).single{it.ending}
        assertEquals("2026-10-20T00:15",Instant.ofEpochMilli(end.at).atZone(now.zone).toLocalDateTime().toString())
        assertTrue(plannedAlerts(listOf(task.copy(status="Skipped")),now).isEmpty())
    }
    @Test fun extraAlertsMoveWithAPostponedTaskAndIdsCannotCollide() {
        val t=Task(id="a:b",title="Work",due="2026-10-19",start="20:00",alerts=listOf(TaskAlert("x:y","2026-10-19T21:40","Check in")))
        assertEquals("2026-10-20T21:40",t.transition("Postponed","2026-10-20").alerts.single().at)
        val alerts=plannedAlerts(listOf(t,t.copy(id="a",alerts=listOf(TaskAlert("b:x:y","2026-10-19T21:40","Check in")))),now)
        assertEquals(alerts.size,alerts.map{it.key}.distinct().size)
    }
}
