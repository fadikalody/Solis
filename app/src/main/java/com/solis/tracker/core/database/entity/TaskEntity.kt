package com.solis.tracker.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "task",
    indices = [
        Index("dueDate"),
        Index("status"),
        Index("categoryId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = TaskCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String = "",
    val dueDate: LocalDate? = null,
    val dueTime: String? = null,
    val priority: String = "MEDIUM",
    val status: String = "OPEN",
    val completedAt: Instant? = null,
    val repeatRule: String? = null,
    val reminderAt: Instant? = null,
    val categoryId: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
