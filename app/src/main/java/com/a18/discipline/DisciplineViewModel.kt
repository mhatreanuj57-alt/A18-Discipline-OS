package com.a18.discipline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.a18.discipline.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(kotlinx.coroutines.FlowPreview::class,kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DisciplineViewModel(application: Application): AndroidViewModel(application) {
    val repo = (application as DisciplineApp).repository
    val error = MutableStateFlow<String?>(null)
    val initialized = MutableStateFlow(false)
    private val reload = MutableStateFlow(0)
    val snapshot = reload.flatMapLatest { repo.snapshot.catch { error.value = "Local data could not be read. Your database has not been reset. ${it.message}" } }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),Snapshot())
    val settings = repo.settings.catch { error.value = "Settings could not be read: ${it.message}" }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),Settings())
    fun initialize() = run {
        initialized.value=false
        error.value=null
        repo.initialize()
        if(repo.database.entries().find("meta","task-alert-preference-v1")==null) {
            repo.saveSettings(repo.settings.first().copy(notifications=true))
            repo.database.entries().put(Entry("meta","task-alert-preference-v1","","1"))
        }
        reload.value+=1
        initialized.value=true
    }
    init { initialize(); viewModelScope.launch {
        combine(snapshot,settings) { data,options -> data.tasks to options.notifications }.distinctUntilChanged().debounce(1000).collect { (tasks,enabled) ->
            withContext(Dispatchers.IO) { Reminders.reschedule(application,tasks,enabled) }
        }
    } }
    fun run(action: suspend () -> Unit) { viewModelScope.launch { try { withContext(Dispatchers.IO) { action() } } catch (e: Exception) { if(e is CancellationException) throw e; error.value=e.message ?: "Local operation failed. No data was cleared." } } }
    fun task(task: Task) = run { repo.saveTask(task); repo.initialize() }
    fun status(task: Task,status: String,date: String=task.due) = run { repo.status(task,status,date) }
    fun options(value: Settings) = run { repo.saveSettings(value) }
    fun timer(timer: Timer) = run { repo.saveTimer(timer); Reminders.focus(getApplication(),timer) }
}
