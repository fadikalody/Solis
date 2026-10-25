package com.solis.tracker.feature.expenses.data

import com.solis.tracker.core.common.AppDispatchers
import com.solis.tracker.core.common.Dispatcher
import com.solis.tracker.core.database.dao.ExpenseDao
import com.solis.tracker.core.database.entity.AccountEntity
import com.solis.tracker.core.database.entity.ExpenseCategoryEntity
import com.solis.tracker.core.datastore.SettingsRepository
import com.solis.tracker.feature.expenses.domain.ExpensesRepository
import com.solis.tracker.feature.expenses.domain.model.Account
import com.solis.tracker.feature.expenses.domain.model.Budget
import com.solis.tracker.feature.expenses.domain.model.CategorySummary
import com.solis.tracker.feature.expenses.domain.model.ExpenseCategory
import com.solis.tracker.feature.expenses.domain.model.Transaction
import com.solis.tracker.feature.expenses.domain.model.TransactionKind
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpensesRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val settingsRepository: SettingsRepository,
    @Dispatcher(AppDispatchers.Io) private val ioDispatcher: CoroutineDispatcher
) : ExpensesRepository {
    
    override fun getAllTransactions(): Flow<List<Transaction>> {
        return expenseDao.getAllTransactions().map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsBetween(start: Instant, end: Instant): Flow<List<Transaction>> {
        return expenseDao.getTransactionsBetween(start.toEpochMilli(), end.toEpochMilli())
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveTransaction(transaction: Transaction) = withContext(ioDispatcher) {
        expenseDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: String) = withContext(ioDispatcher) {
        expenseDao.deleteTransactionById(id)
    }

    override fun getAllCategories(): Flow<List<ExpenseCategory>> {
        return expenseDao.getAllCategories().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveCategory(category: ExpenseCategory) = withContext(ioDispatcher) {
        expenseDao.insertCategory(category.toEntity())
    }

    override fun getAllAccounts(): Flow<List<Account>> {
        return expenseDao.getAllAccounts().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveAccount(account: Account) = withContext(ioDispatcher) {
        expenseDao.insertAccount(account.toEntity())
    }

    override fun getBudgetsForMonth(month: String): Flow<List<Budget>> {
        return expenseDao.getBudgetsForMonth(month).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveBudget(budget: Budget) = withContext(ioDispatcher) {
        expenseDao.insertBudget(budget.toEntity())
    }

    override fun getCategorySummaries(start: Instant, end: Instant): Flow<List<CategorySummary>> {
        return combine(
            expenseDao.getExpenseSumsByCategory(start.toEpochMilli(), end.toEpochMilli()),
            expenseDao.getAllCategories()
        ) { categorySums, categories ->
            val categoriesMap = categories.associateBy { it.id }
            categorySums.mapNotNull { sum ->
                val category = categoriesMap[sum.categoryId]
                if (category != null) {
                    CategorySummary(category.toDomain(), sum.totalAmount)
                } else null
            }
        }
    }

    override fun getTotalAmountByKind(start: Instant, end: Instant, kind: TransactionKind): Flow<Long> {
        return expenseDao.getTotalAmountByKind(start.toEpochMilli(), end.toEpochMilli(), kind.name)
            .map { it ?: 0L }
    }

    override suspend fun seedDefaultsIfNeeded() = withContext(ioDispatcher) {
        val settings = settingsRepository.userSettings.first()
        if (!settings.isExpensesSeeded) {
            val categories = listOf(
                ExpenseCategoryEntity("cat_food", "Food & Dining", "", "", "EXPENSE", true),
                ExpenseCategoryEntity("cat_transport", "Transport", "", "", "EXPENSE", true),
                ExpenseCategoryEntity("cat_groceries", "Groceries", "", "", "EXPENSE", true),
                ExpenseCategoryEntity("cat_utilities", "Utilities", "", "", "EXPENSE", true),
                ExpenseCategoryEntity("cat_entertainment", "Entertainment", "", "", "EXPENSE", true),
                ExpenseCategoryEntity("cat_income", "Income", "", "", "INCOME", true)
            )
            categories.forEach { expenseDao.insertCategory(it) }
            expenseDao.insertAccount(AccountEntity("acc_cash", "Cash", "CASH", 0L))
            settingsRepository.setExpensesSeeded(true)
        }
    }
}
