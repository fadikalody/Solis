package com.solis.tracker.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_category")
data class TaskCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorKey: String,
    val icon: String
)
