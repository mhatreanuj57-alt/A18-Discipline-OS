package com.a18.discipline.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.a18.discipline.DisciplineViewModel
import com.a18.discipline.data.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable fun Dashboard(data: Snapshot,settings: Settings,date: String,change: (String) -> Unit,edit: (Task) -> Unit,focus: () -> Unit,tasks: () -> Unit,openPlan: () -> Unit) {
    val today=data.tasks.filter{it.due==date && !it.archived}; val logs=data.logs.filter{it.date==date}
    LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=24.dp)) {
        item { Text(LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),shape=MaterialTheme.shapes.large) { Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("Make room for progress,\n${settings.name}.",style=MaterialTheme.typography.headlineMedium)
            Text(settings.mission,style=MaterialTheme.typography.bodyMedium)
            val completed=today.count{it.status=="Completed"}
            LinearProgressIndicator(progress={if(today.isEmpty())0f else completed.toFloat()/today.size},modifier=Modifier.fillMaxWidth(),trackColor=MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.12f))
            Text("$completed of ${today.size} tasks completed",style=MaterialTheme.typography.labelLarge)
            if(settings.strict && today.any{it.effectiveStatus()=="Overdue"}) Text("Officer mode: Review the unfinished work. Choose one responsibility and begin a realistic focus block.")
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=focus){Text("Start focus")};TextButton(onClick=tasks){Text("View tasks")}}
        } } }
        item { FilledTonalButton(onClick=openPlan,modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.DateRange,null);Spacer(Modifier.width(8.dp));Text("Open prebuilt to-do list")} }
        item { Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Card(Modifier.weight(1f),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.padding(16.dp)){Text("${streak(data.challenge)}",style=MaterialTheme.typography.headlineMedium);Text("Day streak",style=MaterialTheme.typography.labelLarge)} }
            Card(Modifier.weight(1f),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.tertiaryContainer)) { Column(Modifier.padding(16.dp)){Text("${logs.filter{it.kind in listOf("Study","Coding","Project")}.sumOf{it.minutes}.toInt()}",style=MaterialTheme.typography.headlineMedium);Text("Focus minutes logged",style=MaterialTheme.typography.labelLarge)} }
        } }
        item { DateButton(date,change) }
        item { Text("Your next tasks",style=MaterialTheme.typography.titleLarge) }
        if(today.isEmpty())item{Empty("No tasks for this date. Open your plan or add a task.")}
        items(today.filter{it.status !in listOf("Completed","Skipped","Missed")}.sortedBy{it.start}.take(8),key={it.id}) { t -> TaskCard(t,{edit(t)}) }
        item { Panel("Time recorded","Only your saved activity logs count toward these totals.") {
            listOf("Study","Coding","Project","Gaming","Social media","Sleep","Exercise").forEach { kind -> Text("$kind · ${logs.filter{it.kind==kind}.sumOf{it.minutes}.toInt()} min") }
        } }
        item { Panel("Today's timetable") { val blocks=data.schedules.filter{it.date==date && !it.archived}.sortedBy{it.start}; if(blocks.isEmpty()) Empty("No activities on this date. Add a block in Calendar."); blocks.forEach { Text("${it.start}–${it.end}  ${it.title}",style=MaterialTheme.typography.bodyMedium) } } }
        item { Panel("Assignments & revision") { val assignments=data.items.filter{it.kind in listOf("Assignment","Exam preparation","Weak topic") && it.progress<100}; if(assignments.isEmpty()) Empty("No pending planner items. Add your actual assignments in Study planner.");assignments.take(5).forEach{Text("${it.title} · ${it.due}")} } }
    }
}
@Composable fun TaskCard(task: Task,edit: () -> Unit,status: ((String) -> Unit)?=null) {
    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)) { Row(Modifier.padding(16.dp)) {
        if(status!=null) Checkbox(task.status=="Completed",{status(if(it) "Completed" else "Not Started")})
        Column(Modifier.weight(1f).clickable(onClick=edit).padding(4.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            if(task.start.isNotBlank())Text("${task.start}${if(task.end.isBlank()) "" else " – ${task.end}"}",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
            Text(task.title,style=MaterialTheme.typography.titleMedium)
            Text("${task.due} · ${task.category} · ${task.priority}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if(task.archived) "Previous plan · ${task.status}" else task.effectiveStatus(),color=if(task.status=="Completed") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelMedium)
            if(task.subtasks.isNotEmpty()) Text("${task.subtasks.count{it.done}} / ${task.subtasks.size} subtasks",style=MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick=edit){Icon(Icons.Outlined.Edit,"Edit ${task.title}")}
    } }
}
@Composable fun TasksScreen(data: Snapshot,date: String,change: (String) -> Unit,edit: (Task) -> Unit,vm: DisciplineViewModel,showPlan: Boolean=false) {
    var filter by rememberSaveable(showPlan) { mutableStateOf(if(showPlan) "Four-month plan" else "Selected date") }; var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }
    val today=LocalDate.now().toString()
    val tasks=data.tasks.filter { t -> (!t.archived || filter in listOf("Previous plan","Completed")) && (when(filter){
        "Previous plan" -> t.archived
        "Today" -> t.due==today; "Tomorrow" -> t.due==LocalDate.now().plusDays(1).toString(); "Upcoming" -> t.due>today
        "Completed" -> t.status=="Completed"; "Pending" -> t.status !in listOf("Completed","Missed","Skipped"); "Overdue" -> t.effectiveStatus()=="Overdue"
        "Missed" -> t.status=="Missed"; "Four-month plan" -> t.due in planStart..planEnd; else -> t.due==date
    }) && (category=="All" || t.category==category) && t.title.contains(query,true) }.sortedWith(compareBy({it.due},{it.start},{it.title}))
    LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=24.dp)) {
        item { Panel("Prebuilt to-do list","19 October 2026 – 18 February 2027 · 123 days") {
            Text(if(date=="2026-10-19") "Tonight: assignments 20:00–21:45; GitHub 21:45–23:00. Start and end notifications, plus check-ins at 21:40 and 22:55." else "Seven evening tasks, 19:00–23:00. Starting and ending notifications use your two custom sounds.")
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={change(planStart);filter="Selected date";category="All";query=""}){Text("Open 19 October")}
                OutlinedButton(onClick={filter="Four-month plan";category="All";query=""}){Text("Show full plan")}
            }
        } }
        item { DateButton(date,{change(it);filter="Selected date"}); Button(onClick={edit(Task(due=date))},modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.Add,null);Text("Add task")} }
        item { Choice("View",filter,listOf("Selected date","Four-month plan","Today","Tomorrow","Upcoming","Completed","Pending","Overdue","Missed","Previous plan"),{filter=it}) }
        item { Choice("Category",category,listOf("All")+categories,{category=it}); Field("Search tasks",query,{query=it}) }
        item { Text("${tasks.size} tasks · Changes save on this device") }
        if(tasks.isEmpty()) item{Empty("No matching tasks. Choose another date or add a task.")}
        items(tasks,key={it.id}) { t -> TaskCard(t,{edit(t)},{vm.status(t,it)}) }
    }
}

@Composable fun CalendarScreen(data: Snapshot,date: String,change: (String) -> Unit,edit: (Schedule) -> Unit,vm: DisciplineViewModel) {
    var view by rememberSaveable { mutableStateOf("Month") }; val selected=LocalDate.parse(date); val month=YearMonth.from(selected)
    val first=if(view=="Month") month.atDay(1).minusDays((month.atDay(1).dayOfWeek.value-1).toLong()) else selected.minusDays((selected.dayOfWeek.value-1).toLong())
    val days=if(view=="Month") 42 else 7
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)) {
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            IconButton(onClick={change((if(view=="Month") selected.minusMonths(1) else selected.minusWeeks(1)).toString())}){Icon(Icons.Outlined.ChevronLeft,"Previous $view")}
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),Modifier.padding(top=12.dp))
            IconButton(onClick={change((if(view=="Month") selected.plusMonths(1) else selected.plusWeeks(1)).toString())}){Icon(Icons.Outlined.ChevronRight,"Next $view")}
        }; Choice("Calendar",view,listOf("Month","Week"),{view=it}) }
        item { Row { listOf("M","T","W","T","F","S","S").forEach { Box(Modifier.weight(1f).padding(8.dp)){Text(it)} } }
            repeat(days/7) { week -> Row { repeat(7) { day -> val d=first.plusDays((week*7+day).toLong()); val key=d.toString(); val n=data.tasks.count{it.due==key && !it.archived};
                Surface(onClick={change(key)},color=if(key==date) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,contentColor=if(key==date) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,modifier=Modifier.weight(1f).padding(2.dp),shape=MaterialTheme.shapes.small) {
                    Column(Modifier.padding(vertical=10.dp,horizontal=3.dp)){Text("${d.dayOfMonth}");Text(if(n>0) "$n tasks" else "—",style=MaterialTheme.typography.labelSmall)}
                }
            } } }
        }
        item { DateButton(date,change); Button(onClick={edit(Schedule(date=date))},modifier=Modifier.fillMaxWidth()){Text("Add timetable block")} }
        val blocks=data.schedules.filter{it.date==date && !it.archived}.sortedBy{it.start}
        if(blocks.isEmpty()) item{Empty("No timetable blocks on this date.")}
        items(blocks,key={it.id}) { block -> Card { Row(Modifier.fillMaxWidth().padding(10.dp)) {
            Checkbox(block.done,{vm.run{vm.repo.toggleSchedule(block)}})
            Column(Modifier.weight(1f).clickable{edit(block)}.padding(6.dp)){Text("${block.start}–${block.end}",color=MaterialTheme.colorScheme.primary);Text(block.title);Text(block.kind,style=MaterialTheme.typography.bodySmall)}
            IconButton(onClick={edit(block)}){Icon(Icons.Outlined.Edit,"Edit timetable block")}
        } } }
    }
}

@Composable fun Workspace(data: Snapshot,workspace: String,date: String,vm: DisciplineViewModel) {
    val groups=when(workspace){"Study" -> subjects; "Coding" -> skills; "Project" -> listOf("Personal project");else -> listOf("Daily habits","Weekly review","Short-term","Long-term")}
    val kinds=when(workspace){"Study" -> listOf("Chapter","Assignment","Exam preparation","Revision","Weak topic");"Coding" -> listOf("Milestone","Documentation","Improvement");"Project" -> listOf("Milestone","Bug","Commit","Documentation","Improvement");else -> listOf("Habit","Short-term goal","Long-term goal")}
    var group by rememberSaveable(workspace){mutableStateOf(groups.first())};var editor by remember{mutableStateOf<Item?>(null)}
    val entries=data.items.filter{it.group==group || (workspace=="Study" && it.group==when(group){"AEC"->"Analog Electronics";"DLD"->"Digital Logic Design";"Agile"->"Agile Methodology";else->group})}
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)) {
        item { Panel(when(workspace){"Study"->"Semester III · CSMU";"Coding"->"Build one skill at a time";"Project"->"Personal projects workspace";else->"Goals & habits"}) {
            Text(if(workspace=="Study") "Suggested study order is editable. Add chapters, weak topics, assignments and revision dates from your actual syllabus." else "Track actual progress, notes and deliverables. Scheduled practice is already in your daily to-do list.")
            Choice("Workspace",group,groups,{group=it});Button(onClick={editor=Item(group=group,kind=kinds.first(),due=date)}){Text("Add ${if(workspace=="Goals") "goal or habit" else "planner item"}")}
        } }
        if(entries.isEmpty()) item{Empty("No recorded items in $group. Add your first item above.")}
        items(entries,key={it.id}){entry -> Panel(entry.title,"${entry.kind} · Due ${entry.due}") {
            LinearProgressIndicator(progress={ (entry.progress/100).toFloat() },modifier=Modifier.fillMaxWidth());Text("${entry.progress.toInt()}% complete")
            if(entry.notes.isNotBlank())Text(entry.notes)
            if(entry.kind=="Habit") Check("Checked in on $date",date in entry.checks,{done -> vm.run{vm.repo.saveItem(entry.copy(checks=if(done) (entry.checks+date).distinct() else entry.checks-date))}})
            OutlinedButton(onClick={editor=entry}){Text("Edit details")}
        } }
    }
    editor?.let { e -> ItemEditor(e,groups,kinds,{editor=null},{vm.run{vm.repo.saveItem(it)};editor=null},{vm.run{vm.repo.delete("items",e.id)};editor=null}) }
}
