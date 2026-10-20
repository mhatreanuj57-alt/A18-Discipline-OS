package com.a18.discipline.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.time.*
import java.util.UUID

val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
fun uid() = UUID.randomUUID().toString()
val statuses = listOf("Not Started", "In Progress", "Completed", "Missed", "Skipped", "Overdue", "Postponed")
val categories = listOf("Academic", "Coding", "Project", "Personal", "Health")
val subjects = listOf("Engineering Mathematics", "AEC", "DLD", "Data Structures", "DevOps", "Agile", "Design Thinking")
val skills = listOf("Git", "GitHub", "CI/CD", "GitHub Actions", "Python", "AI", "Machine learning", "Software engineering")
val logKinds = listOf("Study", "Coding", "Gaming", "Social media", "Sleep", "Exercise", "Project")
val challengeGoals = listOf("College revision", "Practical coding", "Scheduled project work", "Physical activity", "Controlled entertainment", "Sufficient sleep", "Honest reflection")

@Serializable data class Subtask(val id: String = uid(), val title: String = "", val done: Boolean = false)
@Serializable data class History(val at: String, val status: String, val due: String)
@Serializable data class TaskAlert(val id: String = uid(), val at: String, val message: String = "Task reminder")
@Serializable data class Task(
    val id: String = uid(), val title: String = "", val category: String = "Academic", val priority: String = "Medium",
    val due: String = LocalDate.now().toString(), val start: String = "", val end: String = "", val scheduleId: String? = null,
    val deadline: String = "", val reminder: String = "", val status: String = "Not Started",
    val estimate: Double = 25.0, val actual: Double = 0.0, val notes: String = "", val subtasks: List<Subtask> = emptyList(),
    val repeat: String = "none", val series: String = id, val originalDue: String? = null, val originalDeadline: String? = null,
    val created: String = Instant.now().toString(), val completed: String? = null, val history: List<History> = emptyList(),
    val archived: Boolean = false, val alerts: List<TaskAlert> = emptyList()
)
@Serializable data class Activity(val id: String = uid(), val date: String = LocalDate.now().toString(), val kind: String = "Study",
    val title: String = "", val minutes: Double = 0.0, val start: String = "", val end: String = "", val subject: String = "",
    val taskId: String = "", val notes: String = "", val source: String = "manual", val sessionId: String? = null)
@Serializable data class Item(val id: String = uid(), val title: String = "", val kind: String = "Chapter", val group: String = "Engineering Mathematics",
    val due: String = LocalDate.now().toString(), val progress: Double = 0.0, val target: Double = 1.0, val notes: String = "", val checks: List<String> = emptyList())
@Serializable data class Schedule(val id: String = uid(), val date: String = LocalDate.now().toString(), val title: String = "",
    val start: String = "09:00", val end: String = "10:00", val kind: String = "Study", val done: Boolean = false, val taskId: String? = null, val archived: Boolean = false)
@Serializable data class Challenge(val id: String, val checks: List<Int> = emptyList(), val review: String = "")
@Serializable data class Journal(val id: String, val accomplished: String = "", val unfinished: String = "", val distractions: String = "",
    val learned: String = "", val mood: String = "Okay", val improve: String = "")
@Serializable data class PrivateLog(val id: String = uid(), val date: String = LocalDate.now().toString(), val viewed: Boolean = false,
    val minutes: Double = 0.0, val frequency: Int? = null, val urges: Int? = null, val impact: String = "none", val trigger: String = "",
    val effect: String = "", val alternative: String = "")
@Serializable data class Settings(val id: String = "main", val planVersion: Int = 1, val name: String = "A18",
    val mission: String = "Small actions. Kept promises. A better version of you.", val strict: Boolean = false,
    val privateEnabled: Boolean = false, val focus: Int = 25, val `break`: Int = 5, val longBreak: Int = 15,
    val gamingLimit: Int = 60, val socialLimit: Int = 30, val studyTarget: Int = 60, val codingTarget: Int = 45,
    val weeklyStudy: Int = 10, val notifications: Boolean = false)
@Serializable data class Timer(val id: String = "active", val sessionId: String = uid(), val mode: String = "focus",
    val duration: Int = 1500, val remaining: Int = 1500, val endAt: Long? = null, val taskId: String = "",
    val kind: String = "Study", val subject: String = "", val cycles: Int = 0, val started: Boolean = false)
@Serializable data class Backup(val app: String = "A18 Discipline OS", val version: Int = 1, val exported: String = Instant.now().toString(),
    val tasks: List<Task> = emptyList(), val logs: List<Activity> = emptyList(), val items: List<Item> = emptyList(),
    val schedules: List<Schedule> = emptyList(), val challenge: List<Challenge> = emptyList(), val settings: Settings? = null,
    val privateLogs: List<PrivateLog>? = null, val journals: List<Journal>? = null, val drafts: List<JsonObject>? = null)

fun Task.scheduleLabel(now: ZonedDateTime = ZonedDateTime.now()): String? {
    if (start.isBlank() || archived || status in listOf("Completed", "Missed", "Skipped")) return null
    val beginning = LocalDate.parse(due).atTime(LocalTime.parse(start)).atZone(now.zone)
    val ending = if (end.isBlank()) beginning.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        else beginning.toLocalDate().plusDays(if (LocalTime.parse(end) <= LocalTime.parse(start)) 1 else 0)
            .atTime(LocalTime.parse(end)).atZone(now.zone)
    return when {
        !now.isBefore(ending) -> "Scheduled time ended"
        !now.isBefore(beginning) -> "Scheduled now"
        else -> null
    }
}
fun Task.canStart(now: ZonedDateTime = ZonedDateTime.now()): Boolean =
    !archived && status in listOf("Not Started", "Postponed") &&
        (due > "2026-10-19" || (due == "2026-10-19" && start >= "21:40")) &&
        due >= now.toLocalDate().toString() && scheduleLabel(now) != "Scheduled time ended"

fun Task.effectiveStatus(now: ZonedDateTime = ZonedDateTime.now()): String {
    if (status in listOf("Completed", "Missed", "Skipped")) return status
    val limit = if (deadline.isBlank()) LocalDate.parse(due).atTime(23, 59, 59).atZone(now.zone)
        else parseDateTime(deadline, now.zone)
    return if (limit.isBefore(now)) "Overdue" else status
}
fun parseDateTime(value: String, zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
    runCatching { Instant.parse(value).atZone(zone) }.getOrElse {
        runCatching { OffsetDateTime.parse(value).atZoneSameInstant(zone) }.getOrElse { LocalDateTime.parse(value).atZone(zone) }
    }
fun Task.transition(status: String, date: String = due, at: Instant = Instant.now()): Task {
    require(status in statuses)
    LocalDate.parse(date)
    return copy(status = status, due = date, originalDue = originalDue ?: due, originalDeadline = originalDeadline ?: deadline,
        deadline = if (date != due && deadline.isNotBlank()) date + deadline.substring(10) else deadline,
        reminder = if (date != due && reminder.isNotBlank()) date + reminder.substring(10) else reminder,
        alerts = if(date!=due) alerts.map{it.copy(at=date+it.at.substring(10))} else alerts,
        completed = if (status == "Completed") at.toString() else null,
        history = history + History(at.toString(), status, date))
}
fun streak(days: List<Challenge>, today: LocalDate = LocalDate.now()): Int {
    val complete = days.filter { it.checks.distinct().size == 7 }.map { it.id }.toSet()
    var date = if (today.toString() in complete) today else today.minusDays(1)
    var count = 0
    while (date.toString() in complete) { count++; date = date.minusDays(1) }
    return count
}
fun timerRemaining(timer: Timer, now: Long = System.currentTimeMillis()) =
    timer.endAt?.let { ((it - now + 999) / 1000).toInt().coerceIn(0, timer.duration) } ?: timer.remaining

fun Backup.validate(): Backup {
    require(app == "A18 Discipline OS" && version == 1) { "Unsupported backup format" }
    fun id(v: String) { require(v.isNotBlank() && v.length <= 200) { "Invalid record ID" } }
    fun title(v: String) { require(v.isNotBlank() && v.length <= 300) { "Title must contain 1–300 characters" } }
    fun date(v: String) { require(LocalDate.parse(v).toString() == v) { "Invalid date" } }
    fun text(v: String) { require(v.length <= 20000) { "Notes are too long" } }
    fun time(v: String) { if (v.isNotBlank()) LocalTime.parse(v) }
    fun range(v: Double, max: Double) { require(v.isFinite() && v in 0.0..max) { "Number outside allowed range" } }
    require(tasks.size + logs.size + items.size + schedules.size < 150000) { "Backup too large" }
    tasks.forEach { t ->
        id(t.id); id(t.series); title(t.title); date(t.due); time(t.start); time(t.end); text(t.notes)
        require(t.status in statuses && t.category in categories + "AgroVision AI" && t.priority in listOf("High", "Medium", "Low"))
        require(t.repeat in listOf("none", "daily", "weekly")); range(t.estimate,100000.0); range(t.actual,100000.0)
        if (t.deadline.isNotBlank()) parseDateTime(t.deadline)
        if (t.reminder.isNotBlank()) parseDateTime(t.reminder)
        require(t.alerts.size<=20 && t.alerts.map{it.id}.distinct().size==t.alerts.size)
        t.alerts.forEach{ id(it.id); require(it.id !in listOf("start","end","reminder")){"Additional alert ID is reserved"}; parseDateTime(it.at); text(it.message) }
        Instant.parse(t.created); t.completed?.let { Instant.parse(it) }; t.originalDue?.let { date(it) }
        require(t.subtasks.size <= 1000 && t.history.size <= 20000)
        t.subtasks.forEach { id(it.id); title(it.title) }; t.history.forEach { Instant.parse(it.at); date(it.due); require(it.status in statuses) }
    }
    logs.forEach { id(it.id); date(it.date); require(it.kind in logKinds); range(it.minutes,1440.0); text(it.notes); text(it.title)
        if (it.start.isNotBlank()) Instant.parse(it.start); if (it.end.isNotBlank()) Instant.parse(it.end) }
    items.forEach { id(it.id); title(it.title); date(it.due); range(it.progress,100.0); require(it.target.isFinite() && it.target > 0); text(it.notes); it.checks.forEach { d -> date(d) } }
    schedules.forEach { id(it.id); date(it.date); title(it.title); time(it.start); time(it.end); require(it.end > it.start) { "Schedule end must follow start" } }
    challenge.forEach { date(it.id); require(it.checks.size <= 7 && it.checks.all { n -> n in 0..6 } && it.checks.distinct().size == it.checks.size); text(it.review) }
    privateLogs?.forEach { id(it.id); date(it.date); range(it.minutes,1440.0); require(it.frequency == null || it.frequency in 0..100); require(it.urges == null || it.urges in 0..10); require(it.impact in listOf("none","studies","sleep","both","unsure")); text(it.trigger); text(it.effect); text(it.alternative) }
    journals?.forEach { date(it.id); listOf(it.accomplished,it.unfinished,it.distractions,it.learned,it.improve,it.mood).forEach { v -> text(v) } }
    settings?.let { require(it.focus in 1..180 && it.`break` in 1..60 && it.longBreak in 1..90 && it.gamingLimit in 0..1440 && it.socialLimit in 0..1440 && it.weeklyStudy in 1..168 && it.studyTarget in 1..1440 && it.codingTarget in 1..1440); title(it.name); text(it.mission) }
    drafts?.forEach { id(it["id"]?.jsonPrimitive?.content ?: ""); require(it.toString().length <= 100000) }
    return this
}
