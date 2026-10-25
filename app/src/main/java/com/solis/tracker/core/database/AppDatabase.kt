package com.solis.tracker.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.solis.tracker.core.database.dao.ExpenseDao
import com.solis.tracker.core.database.dao.HealthDao
import com.solis.tracker.core.database.dao.TaskDao
import com.solis.tracker.core.database.entity.AccountEntity
import com.solis.tracker.core.database.entity.BudgetEntity
import com.solis.tracker.core.database.entity.ExpenseCategoryEntity
import com.solis.tracker.core.database.entity.HealthEntryEntity
import com.solis.tracker.core.database.entity.HealthMetricTypeEntity
import com.solis.tracker.core.database.entity.RecurringTransactionEntity
import com.solis.tracker.core.database.entity.SubtaskEntity
import com.solis.tracker.core.database.entity.TaskCategoryEntity
import com.solis.tracker.core.database.entity.TaskEntity
import com.solis.tracker.core.database.entity.TransactionEntity

@Database(
    entities = [
        TaskEntity::class,
        SubtaskEntity::class,
        TaskCategoryEntity::class,
        ExpenseCategoryEntity::class,
        AccountEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        HealthMetricTypeEntity::class,
        HealthEntryEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun healthDao(): HealthDao
}
