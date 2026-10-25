package com.solis.tracker.core.database.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.solis.tracker.core.database.entity.SubtaskEntity
import com.solis.tracker.core.database.entity.TaskCategoryEntity
import com.solis.tracker.core.database.entity.TaskEntity

data class TaskWithSubtasksAndCategory(
    @Embedded
    val task: TaskEntity,
    
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: TaskCategoryEntity?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val subtasks: List<SubtaskEntity>
)
