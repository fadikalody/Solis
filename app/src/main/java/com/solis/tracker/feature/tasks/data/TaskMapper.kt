package com.solis.tracker.feature.tasks.data

import com.solis.tracker.core.database.dao.TaskWithSubtasksAndCategory
import com.solis.tracker.core.database.entity.SubtaskEntity
import com.solis.tracker.core.database.entity.TaskCategoryEntity
import com.solis.tracker.core.database.entity.TaskEntity
import com.solis.tracker.feature.tasks.domain.model.Subtask
import com.solis.tracker.feature.tasks.domain.model.Task
import com.solis.tracker.feature.tasks.domain.model.TaskCategory
import com.solis.tracker.feature.tasks.domain.model.TaskPriority
import com.solis.tracker.feature.tasks.domain.model.TaskStatus

fun TaskCategoryEntity.toDomain() = TaskCategory(
    id = id,
    name = name,
    colorKey = colorKey,
    icon = icon
)

fun TaskCategory.toEntity() = TaskCategoryEntity(
    id = id,
    name = name,
    colorKey = colorKey,
    icon = icon
)

fun SubtaskEntity.toDomain() = Subtask(
    id = id,
    taskId = taskId,
    title = title,
    done = done,
    position = position
)

fun Subtask.toEntity() = SubtaskEntity(
    id = id,
    taskId = taskId,
    title = title,
    done = done,
    position = position
)

fun TaskWithSubtasksAndCategory.toDomain(): Task {
    return Task(
        id = task.id,
        title = task.title,
        notes = task.notes,
        dueDate = task.dueDate,
        dueTime = task.dueTime,
        priority = runCatching { TaskPriority.valueOf(task.priority) }.getOrElse { TaskPriority.MEDIUM },
        status = runCatching { TaskStatus.valueOf(task.status) }.getOrElse { TaskStatus.OPEN },
        completedAt = task.completedAt,
        repeatRule = task.repeatRule,
        reminderAt = task.reminderAt,
        category = category?.toDomain(),
        subtasks = subtasks.map { it.toDomain() }.sortedBy { it.position },
        createdAt = task.createdAt,
        updatedAt = task.updatedAt
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        notes = notes,
        dueDate = dueDate,
        dueTime = dueTime,
        priority = priority.name,
        status = status.name,
        completedAt = completedAt,
        repeatRule = repeatRule,
        reminderAt = reminderAt,
        categoryId = category?.id,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
