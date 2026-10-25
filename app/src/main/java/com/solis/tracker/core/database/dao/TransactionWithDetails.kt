package com.solis.tracker.core.database.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.solis.tracker.core.database.entity.AccountEntity
import com.solis.tracker.core.database.entity.ExpenseCategoryEntity
import com.solis.tracker.core.database.entity.TransactionEntity

data class TransactionWithDetails(
    @Embedded val transaction: TransactionEntity,
    
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: ExpenseCategoryEntity,
    
    @Relation(
        parentColumn = "accountId",
        entityColumn = "id"
    )
    val account: AccountEntity?
)
