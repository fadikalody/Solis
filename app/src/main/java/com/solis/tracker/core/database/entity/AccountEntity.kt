package com.solis.tracker.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String = "CASH",
    val openingBalanceMinor: Long = 0L
)
