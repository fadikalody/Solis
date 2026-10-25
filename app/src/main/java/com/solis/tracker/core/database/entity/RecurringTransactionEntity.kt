package com.solis.tracker.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "recurring_transaction",
    indices = [
        Index("categoryId"),
        Index("accountId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey val id: String,
    val kind: String = "EXPENSE",
    val amountMinor: Long,
    val currencyCode: String = "USD",
    val categoryId: String,
    val accountId: String? = null,
    val note: String = "",
    val repeatRule: String,
    val nextRunAt: Instant
)
