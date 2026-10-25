package com.solis.tracker.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "transaction",
    indices = [
        Index("occurredAt"),
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
data class TransactionEntity(
    @PrimaryKey val id: String,
    val kind: String = "EXPENSE",
    val amountMinor: Long,
    val currencyCode: String = "USD",
    val categoryId: String,
    val accountId: String? = null,
    val note: String = "",
    val occurredAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
