package com.a18.discipline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.a18.discipline.data.*
import java.time.*

@Composable fun AnalyticsScreen(data: Snapshot,date: String,change: (String) -> Unit) {
    var range by rememberSaveable{mutableStateOf("Week")};var metric by rememberSaveable{mutableStateOf("Study")}
    val end=LocalDate.parse(date);val start=if(range=="Week")end.minusDays(6) else if(range=="Month")end.minusDays(29) else end
    val logs=data.logs.filter{it.date>=start.toString() && it.date<=date}
    val tasks=data.tasks.filter{it.due>=start.toString() && it.due<=date && (!it.archived || it.status in listOf("Completed","Missed","Skipped"))}
    val completed=tasks.count{it.status=="Completed"};val elapsed=tasks.count{it.due<=LocalDate.now().toString()}
    LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=24.dp)) {
        item{DateButton(date,change,"Period ending");Choice("Period",range,listOf("Day","Week","Month"),{range=it})}
        item{Panel("Progress from your records","$start to $date · No estimated or fabricated activity totals") {
            Text("$completed / ${tasks.size} tasks completed (${if(tasks.isEmpty())0 else completed*100/tasks.size}%)")
            Text("${tasks.count{it.effectiveStatus()=="Overdue"}} overdue · ${tasks.count{it.status=="Missed"}} missed · ${tasks.count{it.status=="Postponed"}} postponed")
            val onTime=tasks.count{t -> t.status=="Completed" && t.completed!=null && runCatching{Instant.parse(t.completed)<=if(t.originalDeadline?.isNotBlank()==true)parseDateTime(t.originalDeadline).toInstant() else if(t.deadline.isNotBlank())parseDateTime(t.deadline).toInstant() else LocalDate.parse(t.originalDue ?: t.due).atTime(23,59,59).atZone(ZoneId.systemDefault()).toInstant()}.getOrDefault(false)}
            Text("$onTime completed by original deadline · ${completed-onTime} completed late")
            logKinds.forEach{k -> Text("$k · ${"%.1f".format(logs.filter{it.kind==k}.sumOf{it.minutes}/60)} h")}
        }}
        item{Panel("Daily activity chart","Tap a bar to view that day's records. Values are minutes.") {
            Choice("Metric",metric,logKinds,{metric=it})
            val values=(0..java.time.temporal.ChronoUnit.DAYS.between(start,end).toInt()).map{n -> val d=start.plusDays(n.toLong()).toString();d to logs.filter{it.date==d && it.kind==metric}.sumOf{it.minutes}}
            val max=values.maxOfOrNull{it.second}?.coerceAtLeast(1.0) ?: 1.0
            Row(Modifier.fillMaxWidth().height(170.dp),horizontalArrangement=Arrangement.spacedBy(2.dp),verticalAlignment=androidx.compose.ui.Alignment.Bottom){values.forEach{(d,value) ->
                Surface(onClick={change(d);range="Day"},modifier=Modifier.weight(1f).fillMaxHeight().semantics{contentDescription="$d: ${value.toInt()} minutes of $metric"},color=MaterialTheme.colorScheme.surface){Column(verticalArrangement=Arrangement.Bottom){Box(Modifier.fillMaxWidth().height((value/max*135).toFloat().coerceAtLeast(2f).dp).background(MaterialTheme.colorScheme.primary));if(values.size<=7)Text(d.takeLast(2),style=MaterialTheme.typography.labelSmall)}}
            }}
            if(logs.none{it.kind==metric})Empty("No $metric time recorded in this period.")
        }}
        item{Panel("Subject & project progress") {
            (subjects+"Personal project").forEach{group -> val rows=data.items.filter{it.group==group};val progress=if(rows.isEmpty())0.0 else rows.map{it.progress}.average()
                Text("$group · ${progress.toInt()}% (${rows.size} recorded items)");LinearProgressIndicator(progress={(progress/100).toFloat()},modifier=Modifier.fillMaxWidth())}
        }}
        item{Panel("Weekly review prompts") {
            Text("Which unfinished responsibilities need a new time slot? What helped you focus? Which distractions affected sleep or studies? Record one realistic change in your journal.")
            Text("Time totals reflect manual logs and completed focus sessions. Task completion measures recorded outcomes, not personal worth.",style=MaterialTheme.typography.bodySmall)
        }}
    }
}
