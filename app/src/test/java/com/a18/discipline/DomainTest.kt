package com.a18.discipline

import com.a18.discipline.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*
import kotlinx.serialization.encodeToString

class DomainTest {
    @Test fun postponementPreservesHistoryAndOriginalDeadline() {
        val original=Task(title="Revision",due="2026-10-19",deadline="2026-10-19T19:30")
        val changed=original.transition("Postponed","2026-10-20",Instant.parse("2026-10-19T15:00:00Z"))
        assertEquals("2026-10-19",changed.originalDue);assertEquals("2026-10-20T19:30",changed.deadline)
        assertEquals("Postponed",changed.history.single().status);assertNull(changed.completed)
        assertEquals("Completed",changed.transition("Completed").status)
    }
    @Test fun overdueUsesLocalDeadlineAndKeepsTerminalStates() {
        val t=Task(title="Code",due="2026-10-19",deadline="2026-10-19T20:45")
        val now=ZonedDateTime.of(2026,10,19,21,0,0,0,ZoneId.of("Asia/Kolkata"))
        assertEquals("Overdue",t.effectiveStatus(now));assertEquals("Missed",t.copy(status="Missed").effectiveStatus(now))
    }
    @Test fun missedDayDoesNotEraseEarlierAchievements() {
        val days=(19..21).map{Challenge("2026-10-$it",if(it!=20)(0..6).toList() else emptyList())}
        assertEquals(1,streak(days,LocalDate.parse("2026-10-21")));assertEquals(2,days.count{it.checks.size==7})
    }
    @Test fun timerUsesSavedWallClockEndAcrossRestart() {
        val t=Timer(duration=1500,remaining=1500,endAt=100000L)
        assertEquals(10,timerRemaining(t,90000));assertEquals(0,timerRemaining(t,110000));assertEquals(85,timerRemaining(t.copy(endAt=null,remaining=85),110000))
    }
    @Test fun invalidImportsAreRejectedBeforeWriting() {
        assertThrows(IllegalArgumentException::class.java){Backup(tasks=listOf(Task(title="",due="2026-10-19"))).validate()}
        assertThrows(Exception::class.java){Backup(tasks=listOf(Task(title="Bad date",due="2026-02-30"))).validate()}
        assertThrows(IllegalArgumentException::class.java){Backup(privateLogs=listOf(PrivateLog(urges=11))).validate()}
        assertThrows(IllegalArgumentException::class.java){Backup(logs=listOf(Activity(minutes=Double.NaN))).validate()}
    }
    @Test fun encryptedBackupRoundTripsAndRejectsTampering() {
        val original="{\"app\":\"A18 Discipline OS\",\"tasks\":[]}"
        val encrypted=BackupCrypto.encrypt(original,"a careful private passphrase")
        assertFalse(encrypted.contains("tasks"));assertEquals(original,BackupCrypto.decode(encrypted,"a careful private passphrase"))
        assertThrows(Exception::class.java){BackupCrypto.decode(encrypted,"incorrect passphrase")}
    }
    @Test fun optionalBackupFieldsAreOmittedRatherThanNullForWebCompatibility() {
        val value=json.encodeToString(Backup(tasks=listOf(Task(title="Portable task")),settings=Settings()))
        assertFalse(value.contains("\"scheduleId\":null"));assertFalse(value.contains("\"privateLogs\":null"))
    }
    @Test fun planOpensOnTodayWithinTheNewRange() {
        assertEquals("2026-10-19",planOpeningDate(LocalDate.parse("2026-10-16")))
        assertEquals("2026-10-19",planOpeningDate(LocalDate.parse("2026-10-17")))
        assertEquals("2026-12-01",planOpeningDate(LocalDate.parse("2026-12-01")))
        assertEquals("2027-02-18",planOpeningDate(LocalDate.parse("2027-02-19")))
        assertEquals(123L,java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(planStart),LocalDate.parse(planEnd))+1)
    }
    @Test fun projectRenameKeepsHistoryAndNeverTouchesPrivateRecords() {
        val original=Task(title="AgroVision AI work",notes="Saved note",actual=20.0,subtasks=listOf(Subtask(title="AgroVision docs"))).transition("Completed")
        val payload=json.encodeToString(original)
        val renamed=json.decodeFromString<Task>(projectLabels("tasks",payload))
        assertEquals(original.copy(title="Personal project work",subtasks=original.subtasks.map{it.copy(title="Personal project docs")}),renamed)
        assertEquals(payload,projectLabels("journals",payload))
        assertEquals(payload,projectLabels("privateLogs",payload))
    }
}
