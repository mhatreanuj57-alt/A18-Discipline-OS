package com.a18.discipline

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.a18.discipline.data.*
import com.a18.discipline.ui.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate

class MainActivity: ComponentActivity() {
    private var permissionResult: ((Boolean) -> Unit)? = null
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionResult?.invoke(it) }
    fun askNotifications(result: (Boolean) -> Unit) {
        if(Build.VERSION.SDK_INT>=33) { permissionResult=result; notifications.launch(Manifest.permission.POST_NOTIFICATIONS) } else result(true)
    }
    fun askExactAlarms() {
        if(Build.VERSION.SDK_INT>=31) startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:$packageName")))
    }
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO){runCatching{Reminders.rebuild(applicationContext,force=true)}}
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent { A18Theme { AppScreen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AppScreen(vm: DisciplineViewModel = viewModel()) {
    val data by vm.snapshot.collectAsStateWithLifecycle(); val settings by vm.settings.collectAsStateWithLifecycle(); val error by vm.error.collectAsStateWithLifecycle()
    val initialized by vm.initialized.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf("To-do list") }; var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var editTask by remember { mutableStateOf<Task?>(null) }; var editActivity by remember { mutableStateOf<Activity?>(null) }; var editSchedule by remember { mutableStateOf<Schedule?>(null) }
    val context=androidx.compose.ui.platform.LocalContext.current
    var askedNotifications by rememberSaveable{mutableStateOf(false)}
    LaunchedEffect(settings.notifications,initialized) {
        if(initialized && settings.notifications && !Reminders.permitted(context) && !askedNotifications) {
            askedNotifications=true
            (context as? MainActivity)?.askNotifications { granted ->
                vm.options(settings.copy(notifications=granted))
                if(granted && !Reminders.precise(context)) (context as? MainActivity)?.askExactAlarms()
            }
        }
    }
    if(!initialized || !data.ready) {
        LaunchScreen(error,{vm.initialize()})
        return
    }
    val drawer=rememberDrawerState(DrawerValue.Closed); val scope=rememberCoroutineScope()
    val pages=listOf("Command center","To-do list","Four-month plan","Calendar","Study planner","Coding & CI/CD","Projects","Activities","Focus timer","30-day challenge","Journal","Analytics","Goals & habits","Settings") + if(settings.privateEnabled) listOf("Private space") else emptyList()
    val icons=listOf(Icons.Outlined.SpaceDashboard,Icons.Outlined.Checklist,Icons.Outlined.DateRange,Icons.Outlined.CalendarMonth,Icons.Outlined.School,Icons.Outlined.Code,Icons.Outlined.AccountTree,Icons.Outlined.SportsEsports,Icons.Outlined.Timer,Icons.Outlined.LocalFireDepartment,Icons.Outlined.EditNote,Icons.Outlined.BarChart,Icons.Outlined.Flag,Icons.Outlined.Settings,Icons.Outlined.Lock)
    LaunchedEffect(settings.privateEnabled) { if(!settings.privateEnabled && page=="Private space") page="Settings" }
    LaunchedEffect(pages) { if(page !in pages) page="To-do list" }
    val drawerContent: @Composable () -> Unit = {
        Row(Modifier.padding(24.dp)) { Surface(shape=MaterialTheme.shapes.medium,color=androidx.compose.ui.res.colorResource(R.color.launcher_background)) { Image(painterResource(R.drawable.logo),"A18 logo",Modifier.size(48.dp).padding(4.dp)) }; Spacer(Modifier.width(12.dp)); Column { Text("A18",style=MaterialTheme.typography.titleLarge); Text("Discipline OS",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) } }
        Column(Modifier.verticalScroll(rememberScrollState())) { pages.forEachIndexed { i,p -> NavigationDrawerItem(label={Text(p)},selected=page==p,icon={Icon(icons[i],null)},onClick={page=p;scope.launch{drawer.close()}},modifier=Modifier.padding(horizontal=12.dp,vertical=2.dp)) } }
    }
    val destinations=listOf("Command center","To-do list","Calendar","Focus timer")
    val labels=listOf("Home","Tasks","Plan","Focus")
    val assets=listOf(R.drawable.ic_home,R.drawable.ic_task,R.drawable.ic_calendar,R.drawable.ic_focus)
    BoxWithConstraints(Modifier.fillMaxSize()) {
      val compact=maxWidth<600.dp;val expanded=maxWidth>=840.dp
      val mainContent: @Composable () -> Unit = {
       Row(Modifier.fillMaxSize()) {
        if(!compact && !expanded) NavigationRail {
            destinations.forEachIndexed { i,p -> NavigationRailItem(selected=page==p,onClick={page=p},icon={Icon(painterResource(assets[i]),null)},label={Text(labels[i])}) }
            NavigationRailItem(selected=page !in destinations,onClick={scope.launch{drawer.open()}},icon={Icon(Icons.Outlined.Menu,"Open navigation")},label={Text("More")})
        }
        Scaffold(modifier=Modifier.weight(1f),containerColor=MaterialTheme.colorScheme.surface,topBar={ TopAppBar(title={Text(page,style=MaterialTheme.typography.titleLarge)},navigationIcon={if(!expanded)IconButton(onClick={scope.launch{drawer.open()}}){Icon(Icons.Outlined.Menu,"Open navigation")}},actions={Icon(Icons.Outlined.CloudOff,"Local device only",Modifier.padding(end=16.dp))}) },bottomBar={
            if(compact) NavigationBar {
                destinations.forEachIndexed { i,p -> NavigationBarItem(selected=page==p,onClick={page=p},icon={Icon(painterResource(assets[i]),null)},label={Text(labels[i])}) }
                NavigationBarItem(selected=page !in destinations,onClick={scope.launch{drawer.open()}},icon={Icon(Icons.Outlined.MoreHoriz,null)},label={Text("More")})
            }
        }) { inset ->
            Column(Modifier.fillMaxSize().padding(inset).widthIn(max=1000.dp).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                when(page) {
                    "Command center" -> Dashboard(data,settings,date,{date=it},{editTask=it},{page="Focus timer"},{page="To-do list"},{date=planOpeningDate();page="Four-month plan"})
                    "To-do list" -> TasksScreen(data,date,{date=it},{editTask=it},vm)
                    "Four-month plan" -> TasksScreen(data,date,{date=it},{editTask=it},vm,showPlan=true)
                    "Calendar" -> CalendarScreen(data,date,{date=it},{editSchedule=it},vm)
                    "Study planner" -> Workspace(data,"Study",date,vm)
                    "Coding & CI/CD" -> Workspace(data,"Coding",date,vm)
                    "Projects" -> Workspace(data,"Project",date,vm)
                    "Goals & habits" -> Workspace(data,"Goals",date,vm)
                    "Activities" -> ActivitiesScreen(data,settings,date,{date=it},{editActivity=it})
                    "Focus timer" -> FocusScreen(data,settings,vm)
                    "30-day challenge" -> ChallengeScreen(data,date,{date=it},vm)
                    "Journal" -> JournalScreen(date,{date=it},vm)
                    "Analytics" -> AnalyticsScreen(data,date,{date=it})
                    "Settings" -> SettingsScreen(settings,vm)
                    "Private space" -> PrivateScreen(date,vm)
                }
            }
        }
       }
      }
      if(expanded) PermanentNavigationDrawer(drawerContent={PermanentDrawerSheet(Modifier.width(280.dp)){drawerContent()}},content=mainContent)
      else ModalNavigationDrawer(drawerState=drawer,drawerContent={ModalDrawerSheet{drawerContent()}},content=mainContent)
    }
    editTask?.let { t -> TaskEditor(t,{editTask=null},{vm.task(it);editTask=null},{vm.run{vm.repo.delete("tasks",t.id)};editTask=null}) }
    editActivity?.let { v -> ActivityEditor(v,{editActivity=null},{vm.run{vm.repo.saveLog(it)};editActivity=null},{vm.run{vm.repo.delete("logs",v.id)};editActivity=null},data.tasks) }
    editSchedule?.let { v -> ScheduleEditor(v,{editSchedule=null},{vm.run{vm.repo.saveSchedule(it)};editSchedule=null},{vm.run{vm.repo.delete("schedules",v.id)};editSchedule=null}) }
    error?.let { AlertDialog(onDismissRequest={vm.error.value=null},title={Text("Action needs attention")},text={Text(it)},confirmButton={TextButton(onClick={vm.error.value=null}){Text("Close")}}) }
}
