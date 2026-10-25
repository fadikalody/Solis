package com.solis.tracker.feature.tasks.domain.model

import java.time.Instant
import java.time.LocalDate

enum class TaskPriority {
    LOW, MEDIUM, HIGH
}

enum class TaskStatus {
    OPEN, DONE
}

data class Task(
    val id: String,
    val title: String,
    val notes: String = "",
    val dueDate: LocalDate? = null,
    val dueTime: String? = null, // Format HH:MM
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.OPEN,
    val completedAt: Instant? = null,
    val repeatRule: String? = null,
    val reminderAt: Instant? = null,
    val category: TaskCategory? = null,
    val subtasks: List<Subtask> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)

data class Subtask(
    val id: String,
    val taskId: String,
    val title: String,
    val done: Boolean = false,
    val position: Int = 0
)

data class TaskCategory(
    val id: String,
    val name: String,
    val colorKey: String,
    val icon: String
)
