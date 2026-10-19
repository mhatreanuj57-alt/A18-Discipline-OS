package com.a18.discipline.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.a18.discipline.DisciplineViewModel
import com.a18.discipline.data.*
import kotlinx.coroutines.delay
import java.time.*

@Composable fun ActivitiesScreen(data: Snapshot,settings: Settings,date: String,change: (String) -> Unit,edit: (Activity) -> Unit) {
    var kind by rememberSaveable { mutableStateOf("All") }
    val entries=data.logs.filter{it.date==date && (kind=="All" || it.kind==kind)}
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)) {
        item{DateButton(date,change);Button(onClick={edit(Activity(date=date))},modifier=Modifier.fillMaxWidth()){Text("Log an activity")};Choice("Activity",kind,listOf("All")+logKinds,{kind=it})}
        item{Panel("Entertainment budget","Manual logs only; no automatic app usage monitoring.") {
            listOf("Gaming" to settings.gamingLimit,"Social media" to settings.socialLimit).forEach{(name,limit)->val total=data.logs.filter{it.date==date && it.kind==name}.sumOf{it.minutes}.toInt();Text("$name: $total / $limit min");if(total>limit)Text(if(settings.strict) "Officer mode: Planned limit exceeded. Record the effect and choose your next responsibility." else "Above your planned limit. Review which responsibilities need attention.",color=MaterialTheme.colorScheme.secondary)}
        }}
        if(entries.isEmpty())item{Empty("No activity logs on this date.")}
        items(entries,key={it.id}) { log -> Panel(log.title.ifBlank{log.kind},"${log.kind} · ${log.minutes.toInt()} minutes · ${log.subject}") {
            if(log.notes.isNotBlank())Text(log.notes)
            log.taskId.takeIf{it.isNotBlank()}?.let{id -> data.tasks.find{it.id==id}?.let{Text("Related task: ${it.title}")}}
            OutlinedButton(onClick={edit(log)}){Text("Edit log")}
        } }
    }
}

@Composable fun FocusScreen(data: Snapshot,settings: Settings,vm: DisciplineViewModel) {
    val timer=data.timer;var now by remember{mutableLongStateOf(System.currentTimeMillis())}
    var task by rememberSaveable{mutableStateOf(timer.taskId)};var kind by rememberSaveable{mutableStateOf(timer.kind)};var subject by rememberSaveable{mutableStateOf(timer.subject)}
    var mode by rememberSaveable{mutableStateOf(timer.mode)};var minutes by rememberSaveable{mutableStateOf((if(timer.mode=="focus")settings.focus else settings.`break`).toString())}
    var reset by remember{mutableStateOf(false)}
    LaunchedEffect(timer.endAt) { while(timer.endAt!=null) { now=System.currentTimeMillis();if(timerRemaining(timer,now)==0){vm.run{vm.repo.finishTimer()};break};delay(1000) } }
    val remaining=timerRemaining(timer,now)
    Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Panel("Focus is a practice","Completed focus sessions are logged once, against the selected task. Paused intervals stay saved.") {
            Text("%02d:%02d".format(remaining/60,remaining%60),style=MaterialTheme.typography.displayLarge,color=MaterialTheme.colorScheme.primary)
            Text("${timer.mode} · ${timer.cycles} focus sessions completed")
            if(!timer.started && timer.endAt==null) {
                Choice("Interval",mode,listOf("focus","break","longBreak"),{mode=it;minutes=(when(it){"focus"->settings.focus;"break"->settings.`break`;else->settings.longBreak}).toString()})
                Field("Duration in minutes",minutes,{minutes=it})
                Choice("Activity",kind,listOf("Study","Coding","Project"),{kind=it});Field("Subject / skill",subject,{subject=it})
                val options=data.tasks.filter{it.due==LocalDate.now().toString() && it.status!="Completed" && !it.archived}
                Choice("Task",data.tasks.find{it.id==task}?.title ?: "Unassigned",listOf("Unassigned")+options.map{it.title},{title -> task=options.find{it.title==title}?.id ?: ""})
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={
                    if(timer.endAt!=null) vm.timer(timer.copy(remaining=timerRemaining(timer),endAt=null))
                    else if(timer.started) vm.timer(timer.copy(endAt=System.currentTimeMillis()+timer.remaining*1000L))
                    else { val amount=minutes.toIntOrNull();if(amount==null || amount !in 1..180) vm.error.value="Choose 1–180 minutes." else vm.timer(Timer(mode=mode,duration=amount*60,remaining=amount*60,endAt=System.currentTimeMillis()+amount*60000L,taskId=task,kind=kind,subject=subject,cycles=timer.cycles,started=true)) }
                }){Text(if(timer.endAt!=null)"Pause" else if(timer.started)"Resume" else "Start")}
                OutlinedButton(onClick={reset=true}){Text("Reset")}
            }
            Text("Background completion and reminders may be delayed by Android battery management. Session times are reconciled on reopening.",style=MaterialTheme.typography.bodySmall)
        }
    }
    if(reset)AlertDialog(onDismissRequest={reset=false},title={Text("Reset this interval?")},text={Text("Unfinished focus time will not count as a completed session.")},confirmButton={TextButton(onClick={vm.timer(Timer(duration=settings.focus*60,remaining=settings.focus*60,cycles=timer.cycles));reset=false}){Text("Reset")}},dismissButton={TextButton(onClick={reset=false}){Text("Keep")}})
}

@Composable fun ChallengeScreen(data: Snapshot,date: String,change: (String) -> Unit,vm: DisciplineViewModel) {
    val today=LocalDate.now().toString();val days=data.challenge.sortedBy{it.id};val selected=days.find{it.id==date}
    val elapsed=days.count{it.id<=today};val completed=days.count{it.id<=today && it.checks.size==7}
    var review by rememberSaveable(selected?.id,selected?.review){mutableStateOf(selected?.review ?: "")}
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)) {
        item{Panel("30 days. Honest progress.","19 October – 17 November 2026") {
            Text("$completed completed · ${days.count{it.id<today && it.checks.size<7}} missed / incomplete · ${streak(days)} day streak")
            Text("Consistency: ${if(elapsed==0)0 else completed*100/elapsed}% of elapsed challenge days")
            Text("A missed day keeps every earlier achievement intact.")
            days.chunked(5).forEach { week -> Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){week.forEach { day -> Surface(onClick={change(day.id)},color=if(day.checks.size==7)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,contentColor=if(day.checks.size==7)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,modifier=Modifier.weight(1f),shape=MaterialTheme.shapes.small){Column(Modifier.padding(8.dp)){Text(day.id.takeLast(2));Text("${day.checks.size}/7",style=MaterialTheme.typography.labelSmall)}}} } }
        }}
        item{DateButton(date,change)}
        if(selected!=null)item{Panel("Daily check-in · $date") {
            if(date>today)Text("This is a future challenge day. Check-ins unlock on that date.")
            challengeGoals.forEachIndexed{i,goal -> Row{Checkbox(i in selected.checks,{checked -> if(date<=today) vm.run{vm.repo.saveChallenge(selected.copy(checks=if(checked)(selected.checks+i).distinct().sorted() else selected.checks-i))}},enabled=date<=today);Text(goal,Modifier.padding(top=12.dp))}}
            Field("Daily / weekly review",review,{review=it},4)
            Button(onClick={vm.run{vm.repo.saveChallenge(selected.copy(review=review))}}){Text("Save review")}
        }} else item{Empty("Choose one of the 30 challenge dates above.")}
        item{Panel(if(today>="2026-11-18")"Final challenge report" else "Challenge report so far") {
            challengeGoals.forEachIndexed{i,goal -> Text("$goal · ${days.count{it.id<=today && i in it.checks}} / $elapsed days")}
            Text("Completed $completed of 30 challenge days. Future days do not count as missed.")
        }}
    }
}

@Composable fun JournalScreen(date: String,change: (String) -> Unit,vm: DisciplineViewModel) {
    var value by remember(date){mutableStateOf<Journal?>(null)};var saved by remember(date){mutableStateOf(false)}
    LaunchedEffect(date){vm.run{val journal=vm.repo.journal(date);kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main){value=journal}}}
    Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        DateButton(date,change);Text("Your reflection is encrypted locally and excluded from ordinary exports.")
        value?.let{v ->
            Field("What I accomplished",v.accomplished,{value=v.copy(accomplished=it);saved=false},3)
            Field("What stayed unfinished",v.unfinished,{value=v.copy(unfinished=it);saved=false},3)
            Field("What distracted me",v.distractions,{value=v.copy(distractions=it);saved=false},3)
            Field("What I learned",v.learned,{value=v.copy(learned=it);saved=false},3)
            Choice("How I felt",v.mood,listOf("Great","Good","Okay","Low","Stressed"),{value=v.copy(mood=it);saved=false})
            Field("One improvement for tomorrow",v.improve,{value=v.copy(improve=it);saved=false},3)
            Button(onClick={vm.run{vm.repo.saveJournal(v);kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main){saved=true}}},modifier=Modifier.fillMaxWidth()){Text(if(saved)"Saved on this device" else "Save reflection")}
        } ?: CircularProgressIndicator()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable fun PrivateScreen(date: String,vm: DisciplineViewModel) {
    var records by remember{mutableStateOf<List<PrivateLog>>(emptyList())};var loaded by remember{mutableStateOf(false)};var edit by remember{mutableStateOf<PrivateLog?>(null)};var refresh by remember{mutableIntStateOf(0)};var clear by remember{mutableStateOf(false)}
    LaunchedEffect(refresh){vm.run{val logs=vm.repo.privateLogs();kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main){records=logs;loaded=true}}}
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)) {
        item{Panel("Private space","Optional. Nonjudgmental. Device-only.") {
            Text("Keystore encryption protects these records at rest. Screen capture and recent-app previews are blocked. No private content enters notifications or dashboard statistics.")
            Button(onClick={edit=PrivateLog(date=date)}){Text("Add optional check-in")}
            OutlinedButton(onClick={clear=true}){Text("Delete all private habit records")}
        }}
        if(!loaded)item{CircularProgressIndicator()} else if(records.isEmpty())item{Empty("No private habit records saved.")}
        if(records.isNotEmpty())item{Panel("Patterns you recorded") {
            Text("${records.size} check-ins · ${records.count{it.impact in listOf("studies","sleep","both")}} reported an effect on studies or sleep")
            Text("These are self-reported observations, not an addiction diagnosis or a judgment about sexual behavior.")
        }}
        items(records,key={it.id}){entry -> Panel(entry.date,"Urges: ${entry.urges?.toString() ?: "not recorded"} · Impact: ${entry.impact}") {
            if(entry.trigger.isNotBlank())Text("Triggers: ${entry.trigger}")
            if(entry.alternative.isNotBlank())Text("Alternatives: ${entry.alternative}")
            OutlinedButton(onClick={edit=entry}){Text("View / edit privately")}
        }}
    }
    edit?.let{v -> PrivateEditor(v,{edit=null},{vm.run{vm.repo.savePrivate(it);kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main){refresh++}};edit=null},{vm.run{vm.repo.delete("privateLogs",v.id);kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main){refresh++}};edit=null})}
    if(clear)AlertDialog(onDismissRequest={clear=false},title={Text("Delete private habit records?")},text={Text("All private habit records on this device will be permanently deleted. Tasks, journals and other data will remain.")},confirmButton={TextButton(onClick={vm.run{vm.repo.deletePrivate();kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main){refresh++}};clear=false}){Text("Delete private records")}},dismissButton={TextButton(onClick={clear=false}){Text("Keep records")}})
}
