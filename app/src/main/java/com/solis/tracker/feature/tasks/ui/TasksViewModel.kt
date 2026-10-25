package com.solis.tracker.feature.tasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solis.tracker.core.notifications.TaskScheduler
import com.solis.tracker.feature.tasks.domain.TasksRepository
import com.solis.tracker.feature.tasks.domain.model.Task
import com.solis.tracker.feature.tasks.domain.model.TaskCategory
import com.solis.tracker.feature.tasks.domain.model.TaskPriority
import com.solis.tracker.feature.tasks.domain.model.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

sealed interface TasksUiState {
    data object Loading : TasksUiState
    data class Success(val tasks: List<Task>, val categories: List<TaskCategory>) : TasksUiState
    data class Error(val message: String) : TasksUiState
}

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TasksRepository,
    private val taskScheduler: TaskScheduler
) : ViewModel() {

    val uiState: StateFlow<TasksUiState> = combine(
        repository.getAllTasks(),
        repository.getAllCategories()
    ) { tasks, categories ->
        TasksUiState.Success(tasks, categories) as TasksUiState
    }
    .catch { e -> emit(TasksUiState.Error(e.message ?: "Unknown error")) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TasksUiState.Loading
    )

    private val _showTaskSheet = MutableStateFlow(false)
    val showTaskSheet = _showTaskSheet.asStateFlow()

    private val _editingTask = MutableStateFlow<Task?>(null)
    val editingTask = _editingTask.asStateFlow()

    private val _undoEvent = MutableSharedFlow<Task>()
    val undoEvent = _undoEvent.asSharedFlow()

    fun openTaskSheet(task: Task? = null) {
        _editingTask.value = task
        _showTaskSheet.value = true
    }

    fun closeTaskSheet() {
        _showTaskSheet.value = false
        _editingTask.value = null
    }

    fun saveTask(
        title: String,
        notes: String,
        dueDate: LocalDate?,
        dueTime: String?,
        priority: TaskPriority,
        categoryId: String?,
        reminderAt: Instant?,
        repeatRule: String?
    ) {
        viewModelScope.launch {
            val task = _editingTask.value?.copy(
                title = title,
                notes = notes,
                dueDate = dueDate,
                dueTime = dueTime,
                priority = priority,
                category = categoryId?.let { id -> TaskCategory(id, "", "", "") },
                reminderAt = reminderAt,
                repeatRule = repeatRule,
                updatedAt = Instant.now()
            ) ?: Task(
                id = UUID.randomUUID().toString(),
                title = title,
                notes = notes,
                dueDate = dueDate,
                dueTime = dueTime,
                priority = priority,
                status = TaskStatus.OPEN,
                category = categoryId?.let { id -> TaskCategory(id, "", "", "") },
                reminderAt = reminderAt,
                repeatRule = repeatRule,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            
            repository.saveTask(task)
            
            if (reminderAt != null) {
                taskScheduler.scheduleReminder(task.id, reminderAt)
            } else {
                taskScheduler.cancelReminder(task.id)
            }

            closeTaskSheet()
        }
    }

    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            val newStatus = if (task.status == TaskStatus.OPEN) TaskStatus.DONE else TaskStatus.OPEN
            val updatedTask = task.copy(
                status = newStatus,
                completedAt = if (newStatus == TaskStatus.DONE) Instant.now() else null,
                updatedAt = Instant.now()
            )
            repository.saveTask(updatedTask)

            if (newStatus == TaskStatus.DONE) {
                taskScheduler.cancelReminder(task.id)
                _undoEvent.emit(task)
                
                // Handle repeat rule
                if (task.repeatRule != null) {
                    generateNextOccurrence(task)
                }
            } else {
                // if undoing completion, reschedule reminder
                if (task.reminderAt != null && task.reminderAt.isAfter(Instant.now())) {
                    taskScheduler.scheduleReminder(task.id, task.reminderAt)
                }
            }
        }
    }
    
    fun undoTaskComplete(task: Task) {
        viewModelScope.launch {
            repository.saveTask(task) // Re-save original task
            if (task.reminderAt != null && task.reminderAt.isAfter(Instant.now())) {
                taskScheduler.scheduleReminder(task.id, task.reminderAt)
            }
        }
    }

    private suspend fun generateNextOccurrence(task: Task) {
        if (task.repeatRule == null || task.dueDate == null) return
        
        val nextDate = when (task.repeatRule) {
            "DAILY" -> task.dueDate.plusDays(1)
            "WEEKLY" -> task.dueDate.plusWeeks(1)
            "MONTHLY" -> task.dueDate.plusMonths(1)
            else -> task.dueDate
        }
        
        val newTask = Task(
            id = UUID.randomUUID().toString(),
            title = task.title,
            notes = task.notes,
            dueDate = nextDate,
            dueTime = task.dueTime,
            priority = task.priority,
            status = TaskStatus.OPEN,
            category = task.category,
            repeatRule = task.repeatRule,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        repository.saveTask(newTask)
    }
}
