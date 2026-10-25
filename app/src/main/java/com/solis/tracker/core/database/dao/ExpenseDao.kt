package com.solis.tracker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.solis.tracker.core.database.entity.AccountEntity
import com.solis.tracker.core.database.entity.BudgetEntity
import com.solis.tracker.core.database.entity.ExpenseCategoryEntity
import com.solis.tracker.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class CategorySum(
    val categoryId: String,
    val totalAmount: Long
)

@Dao
interface ExpenseDao {
    @Transaction
    @Query("SELECT * FROM `transaction` ORDER BY occurredAt DESC")
    fun getAllTransactions(): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query("SELECT * FROM `transaction` WHERE occurredAt >= :startDate AND occurredAt <= :endDate ORDER BY occurredAt DESC")
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<TransactionWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM `transaction` WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: String)

    @Query("SELECT * FROM expense_category ORDER BY name ASC")
    fun getAllCategories(): Flow<List<ExpenseCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ExpenseCategoryEntity)

    @Query("SELECT * FROM expense_category WHERE id = :id")
    suspend fun getCategoryById(id: String): ExpenseCategoryEntity?

    @Query("SELECT * FROM account ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Query("SELECT * FROM account WHERE id = :id")
    suspend fun getAccountById(id: String): AccountEntity?

    @Query("SELECT * FROM budget WHERE month = :month")
    fun getBudgetsForMonth(month: String): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("SELECT categoryId, SUM(amountMinor) as totalAmount FROM `transaction` WHERE occurredAt >= :startDate AND occurredAt <= :endDate AND kind = 'EXPENSE' GROUP BY categoryId")
    fun getExpenseSumsByCategory(startDate: Long, endDate: Long): Flow<List<CategorySum>>
    
    @Query("SELECT SUM(amountMinor) FROM `transaction` WHERE occurredAt >= :startDate AND occurredAt <= :endDate AND kind = :kind")
    fun getTotalAmountByKind(startDate: Long, endDate: Long, kind: String): Flow<Long?>
}
