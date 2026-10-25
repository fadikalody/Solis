package com.solis.tracker.feature.tasks.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solis.tracker.core.notifications.TaskScheduler
import com.solis.tracker.feature.tasks.domain.TasksRepository
import com.solis.tracker.feature.tasks.domain.model.Task
import com.solis.tracker.feature.tasks.domain.model.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class TasksDashboardState(
    val dueToday: List<Task> = emptyList(),
    val overdueCount: Int = 0
)

@HiltViewModel
class TasksDashboardViewModel @Inject constructor(
    private val repository: TasksRepository,
    private val taskScheduler: TaskScheduler
) : ViewModel() {

    val state: StateFlow<TasksDashboardState> = repository.getAllTasks().map { tasks ->
        val today = LocalDate.now()
        val active = tasks.filter { it.status == TaskStatus.OPEN && it.dueDate != null }
        val dueToday = active.filter { it.dueDate == today }
        val overdueCount = active.count { it.dueDate!!.isBefore(today) }
        TasksDashboardState(dueToday = dueToday, overdueCount = overdueCount)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TasksDashboardState()
    )

    fun completeTask(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                status = TaskStatus.DONE,
                completedAt = Instant.now(),
                updatedAt = Instant.now()
            )
            repository.saveTask(updatedTask)
            taskScheduler.cancelReminder(task.id)

            if (task.repeatRule != null) {
                generateNextOccurrence(task)
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
