package com.a18.discipline.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.a18.discipline.*
import com.a18.discipline.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable fun SettingsScreen(settings: Settings,vm: DisciplineViewModel) {
    val context=LocalContext.current
    var sensitive by remember{mutableStateOf(false)};var password by remember{mutableStateOf("")};var message by remember{mutableStateOf("")}
    var incoming by remember{mutableStateOf<String?>(null)};var busy by remember{mutableStateOf(false)}
    var name by remember(settings.name){mutableStateOf(settings.name)};var mission by remember(settings.mission){mutableStateOf(settings.mission)}
    var focus by remember(settings.focus){mutableStateOf(settings.focus.toString())};var rest by remember(settings.`break`){mutableStateOf(settings.`break`.toString())}
    var longRest by remember(settings.longBreak){mutableStateOf(settings.longBreak.toString())};var gaming by remember(settings.gamingLimit){mutableStateOf(settings.gamingLimit.toString())}
    var social by remember(settings.socialLimit){mutableStateOf(settings.socialLimit.toString())};var study by remember(settings.studyTarget){mutableStateOf(settings.studyTarget.toString())}
    var coding by remember(settings.codingTarget){mutableStateOf(settings.codingTarget.toString())};var weekly by remember(settings.weeklyStudy){mutableStateOf(settings.weeklyStudy.toString())}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri -> if(uri!=null)vm.run {
        withContext(Dispatchers.Main){busy=true}
        try { val value=vm.repo.export(sensitive,password)
            context.contentResolver.openOutputStream(uri,"wt")?.bufferedWriter(Charsets.UTF_8)?.use{it.write(value)} ?: error("File could not be opened")
            withContext(Dispatchers.Main){message="Backup saved to your selected file.";password=""}
        } finally {withContext(Dispatchers.Main){busy=false}}
    }}
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri -> if(uri!=null)vm.run {
        val value=context.contentResolver.openInputStream(uri)?.use { stream ->
            val buffer=java.io.ByteArrayOutputStream();val bytes=ByteArray(8192);var read=stream.read(bytes)
            while(read!=-1){require(buffer.size()+read<=50_000_000){"Backup exceeds 50 MB"};buffer.write(bytes,0,read);read=stream.read(bytes)}
            buffer.toString(Charsets.UTF_8.name())
        } ?: error("Could not read selected file")
        withContext(Dispatchers.Main){incoming=value}
    }}
    Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Panel("Your operating system","Room database · DataStore settings · No account · No internet permission") {
            Field("Name",name,{name=it})
            Field("Mission",mission,{mission=it},2)
            Button(onClick={vm.options(settings.copy(name=name,mission=mission))}){Text("Save profile")}
            Toggle("Strict Officer Mode",settings.strict,{vm.options(settings.copy(strict=it))})
            Toggle("Enable optional private space",settings.privateEnabled,{vm.options(settings.copy(privateEnabled=it))})
            Text("Private space is hidden unless enabled. Officer mode never comments on sexual behavior.",style=MaterialTheme.typography.bodySmall)
            Toggle("Study & task notifications",settings.notifications,{enabled ->
                if(enabled)(context as? MainActivity)?.askNotifications{granted ->vm.options(settings.copy(notifications=granted));if(!granted)message="Notification permission was not granted. You can allow it in Android Settings." else if(!Reminders.precise(context))(context as? MainActivity)?.askExactAlarms()}
                else vm.options(settings.copy(notifications=false))
            })
            Text("Regular app notifications at each task's beginning and ending. Start: your WAV. End: your MP3. Private habit records never enter notifications; lock-screen previews use generic wording.",style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick={(context as? MainActivity)?.askExactAlarms()}){Text("Allow precise notification timing")}
            listOf("Starting sound & popups" to Reminders.START_CHANNEL,"Ending sound & popups" to Reminders.END_CHANNEL).forEach{(label,channel) ->
                OutlinedButton(onClick={context.startActivity(android.content.Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,context.packageName).putExtra(android.provider.Settings.EXTRA_CHANNEL_ID,channel))}){Text(label)}
            }
            Text("These are notifications, not alarm-clock alerts. Android labels its precise scheduling permission 'Alarms & reminders'. Closely spaced reminders can be delayed in deep idle. Sound and popups respect Silent, Do Not Disturb and channel settings; simultaneous alerts may produce one sound. Force-stop suspends alerts until reopening.",style=MaterialTheme.typography.bodySmall)
        }
        Panel("Focus durations & targets") {
            Field("Focus minutes (1–180)",focus,{focus=it});Field("Break minutes (1–60)",rest,{rest=it});Field("Long break minutes (1–90)",longRest,{longRest=it})
            Field("Daily gaming limit (minutes)",gaming,{gaming=it});Field("Daily social media limit (minutes)",social,{social=it})
            Field("Daily study target (minutes)",study,{study=it});Field("Daily coding target (minutes)",coding,{coding=it});Field("Weekly study target (hours)",weekly,{weekly=it})
            Button(onClick={runCatching{settings.copy(focus=focus.toInt(),`break`=rest.toInt(),longBreak=longRest.toInt(),gamingLimit=gaming.toInt(),socialLimit=social.toInt(),studyTarget=study.toInt(),codingTarget=coding.toInt(),weeklyStudy=weekly.toInt()).also{Backup(settings=it).validate()}}.onSuccess{vm.options(it);message="Targets saved."}.onFailure{vm.error.value=it.message}}){Text("Save targets")}
        }
        Panel("Backup & restore","Choose your own JSON file using Android's file picker.") {
            Text("Ordinary backups exclude private habit records, journals and sensitive drafts. Android and laptop records are stored separately; importing a file is a manual transfer, not synchronization.")
            Check("I consent to include / import private habits, journals and sensitive drafts",sensitive,{sensitive=it})
            Field("Backup passphrase (12+ characters for export)",password,{password=it},password=true)
            Text(if(sensitive)"Sensitive exports require encryption. Keep the passphrase safe: it cannot be recovered." else "Leave the passphrase empty for an ordinary JSON backup. A passphrase encrypts the backup using the website-compatible format.",style=MaterialTheme.typography.bodySmall)
            Button(enabled=!busy,onClick={if((sensitive || password.isNotBlank()) && password.length<12)vm.error.value="Use a backup passphrase of at least 12 characters." else export.launch("A18-backup-${LocalDate.now()}.json")},modifier=Modifier.fillMaxWidth()){Text(if(busy)"Working…" else "Download backup")}
            OutlinedButton(enabled=!busy,onClick={restore.launch(arrayOf("application/json","text/plain","application/octet-stream"))},modifier=Modifier.fillMaxWidth()){Text("Restore / import web backup")}
            if(message.isNotBlank())Text(message,color=MaterialTheme.colorScheme.primary)
        }
        Panel("Local storage needs a backup") {
            Text("Your saved records survive normal app and phone restarts. Clearing app storage or uninstalling removes the local database and encryption key. Keep separate exported backups. The app does not upload or sync records, and Android cloud backup is disabled.")
            Text("Database version 1 · Android 8.0 or newer · Native Kotlin / Compose",style=MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
    }
    incoming?.let{raw -> AlertDialog(onDismissRequest={incoming=null},title={Text("Merge this backup?")},text={Text("The file will be validated before any records are saved. Missing IDs and untouched bundled defaults will be imported; your existing edits and completion history win conflicts. Private records and journals ${if(sensitive)"will be imported with your consent and encrypted locally" else "will be excluded"}. Notification and privacy settings will be disabled after import.")},confirmButton={TextButton(onClick={vm.run {
        withContext(Dispatchers.Main){busy=true}
        try{vm.repo.restore(raw,sensitive,password);withContext(Dispatchers.Main){message="Backup merged. Existing records were preserved.";password=""}}finally{withContext(Dispatchers.Main){busy=false}}
    };incoming=null}){Text("Validate & merge")}},dismissButton={TextButton(onClick={incoming=null}){Text("Cancel")}})}
}
