package com.a18.discipline.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import java.time.*

@Composable fun A18Theme(content: @Composable () -> Unit) {
    val dark=isSystemInDarkTheme();val context=LocalContext.current
    val colors=if(android.os.Build.VERSION.SDK_INT>=31) {
        if(dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if(dark) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme=colors,typography=Typography(),content=content)
}
@Composable fun Panel(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.medium,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(title,style=MaterialTheme.typography.titleMedium)
        subtitle?.let { Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }; content()
    } }
}
@Composable fun Field(label: String,value: String,onChange: (String) -> Unit,lines: Int = 1,password: Boolean = false) {
    OutlinedTextField(value,onChange,label={ Text(label) },modifier=Modifier.fillMaxWidth(),singleLine=lines==1,minLines=lines,
        visualTransformation=if(password) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None)
}
@Composable fun Choice(label: String,value: String,values: List<String>,change: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box { OutlinedButton(onClick={ expanded=true },modifier=Modifier.fillMaxWidth()) { Text("$label: $value"); Icon(Icons.Outlined.ExpandMore,null) }
        DropdownMenu(expanded,{expanded=false}) { values.forEach { option -> DropdownMenuItem(text={Text(option)},onClick={change(option);expanded=false}) } }
    }
}
@Composable fun DateButton(value: String,change: (String) -> Unit,label: String="Date") {
    val context=LocalContext.current
    OutlinedButton(onClick={ val date=LocalDate.parse(value); DatePickerDialog(context,{_,y,m,d -> change(LocalDate.of(y,m+1,d).toString())},date.year,date.monthValue-1,date.dayOfMonth).show() },modifier=Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.CalendarMonth,null); Spacer(Modifier.width(8.dp)); Text("$label · $value")
    }
}
@Composable fun TimeButton(label: String,value: String,change: (String) -> Unit) {
    val context=LocalContext.current
    OutlinedButton(onClick={ val t=runCatching{LocalTime.parse(value)}.getOrDefault(LocalTime.of(18,0)); TimePickerDialog(context,{_,h,m -> change("%02d:%02d".format(h,m))},t.hour,t.minute,true).show() }) { Text("$label · ${value.ifBlank { "Set time" }}") }
}
@Composable fun Toggle(label: String,checked: Boolean,change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(label,Modifier.weight(1f).padding(top=12.dp)); Switch(checked,change) }
}
@Composable fun Check(label: String,checked: Boolean,change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { change(!checked) }.padding(vertical=2.dp)) { Checkbox(checked,change); Text(label,Modifier.padding(top=12.dp).weight(1f)) }
}
@Composable fun Editor(title: String,close: () -> Unit,save: () -> Unit,delete: (() -> Unit)?=null,error: String?=null,content: @Composable ColumnScope.() -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false),modifier=Modifier.fillMaxWidth().padding(12.dp).widthIn(max=620.dp),
        title={Text(title)},text={ Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) { content(); error?.let { Text(it,color=MaterialTheme.colorScheme.error) } } },
        confirmButton={Button(onClick=save){Text("Save")}},dismissButton={Row { if(delete!=null) TextButton(onClick={confirm=true}){Text("Delete")}; TextButton(onClick=close){Text("Cancel")} }})
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Delete this record?")},text={Text("This cannot be undone. Other records and past achievements will be kept.")},confirmButton={TextButton(onClick={delete?.invoke();confirm=false}){Text("Delete")}},dismissButton={TextButton(onClick={confirm=false}){Text("Keep")}})
}
@Composable fun Empty(text: String) { Text(text,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=12.dp)) }
fun number(value: String,max: Double=100000.0): Double = value.toDoubleOrNull()?.also { require(it.isFinite() && it in 0.0..max) { "Enter a number between 0 and ${max.toInt()}" } } ?: throw IllegalArgumentException("Enter a valid number")
