package com.solis.tracker.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_category")
data class ExpenseCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorKey: String,
    val icon: String,
    val kind: String = "EXPENSE",
    val isDefault: Boolean = false
)
