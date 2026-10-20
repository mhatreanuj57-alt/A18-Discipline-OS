package com.a18.discipline

import com.a18.discipline.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class TaskProgressTest {
    private val now = ZonedDateTime.of(2026, 10, 19, 21, 45, 0, 0, ZoneId.of("Asia/Kolkata"))

    @Test fun scheduleLabelsNeverRecordActualProgress() {
        val task = Task(title="GitHub", due="2026-10-19", start="21:45", end="23:00")
        assertNull(task.scheduleLabel(now.minusSeconds(1)))
        assertEquals("Scheduled now", task.scheduleLabel(now))
        assertEquals("Scheduled time ended", task.scheduleLabel(now.withHour(23)))
        assertEquals("Not Started", task.status)
        assertTrue(task.history.isEmpty())
        assertNull(task.completed)
    }

    @Test fun startIsAvailableForSecondTaskAndFutureDailyTasksOnly() {
        val task = Task(title="GitHub", due="2026-10-19", start="21:45", end="23:00")
        assertTrue(task.canStart(now))
        assertFalse(task.copy(start="20:00", end="21:45").canStart(now.minusMinutes(30)))
        assertFalse(task.canStart(now.withHour(23)))
        assertTrue(task.copy(due="2026-10-20", start="19:00").canStart(now))
        listOf("Completed", "Missed", "Skipped", "In Progress").forEach {
            assertFalse(task.copy(status=it).canStart(now))
        }
        assertFalse(task.copy(archived=true).canStart(now))
        val started = task.transition("In Progress", at=now.toInstant())
        assertEquals("In Progress", started.status)
        assertNull(started.completed)
        assertEquals("In Progress", started.history.single().status)
        assertEquals("Completed", started.transition("Completed").status)
    }

    @Test fun overnightAndUntimedTasksHaveAccurateScheduleLabels() {
        val task = Task(due="2026-10-19", start="23:30", end="00:30")
        assertEquals("Scheduled now", task.scheduleLabel(now.plusDays(1).withHour(0).withMinute(0)))
        assertEquals("Scheduled time ended", task.scheduleLabel(now.plusDays(1).withHour(0).withMinute(30)))
        assertNull(task.copy(start="", end="").scheduleLabel(now))
    }
}
