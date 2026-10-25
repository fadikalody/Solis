package com.solis.tracker.feature.expenses.domain

import com.solis.tracker.feature.expenses.domain.model.Account
import com.solis.tracker.feature.expenses.domain.model.Budget
import com.solis.tracker.feature.expenses.domain.model.CategorySummary
import com.solis.tracker.feature.expenses.domain.model.ExpenseCategory
import com.solis.tracker.feature.expenses.domain.model.Transaction
import com.solis.tracker.feature.expenses.domain.model.TransactionKind
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ExpensesRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsBetween(start: Instant, end: Instant): Flow<List<Transaction>>
    suspend fun saveTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: String)
    
    fun getAllCategories(): Flow<List<ExpenseCategory>>
    suspend fun saveCategory(category: ExpenseCategory)
    
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun saveAccount(account: Account)
    
    fun getBudgetsForMonth(month: String): Flow<List<Budget>>
    suspend fun saveBudget(budget: Budget)
    
    fun getCategorySummaries(start: Instant, end: Instant): Flow<List<CategorySummary>>
    fun getTotalAmountByKind(start: Instant, end: Instant, kind: TransactionKind): Flow<Long>

    suspend fun seedDefaultsIfNeeded()
}
