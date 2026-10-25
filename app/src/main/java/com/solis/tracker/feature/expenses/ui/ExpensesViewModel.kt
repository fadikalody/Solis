package com.solis.tracker.feature.expenses.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solis.tracker.core.datastore.SettingsRepository
import com.solis.tracker.feature.expenses.domain.ExpensesRepository
import com.solis.tracker.feature.expenses.domain.model.ExpenseCategory
import com.solis.tracker.feature.expenses.domain.model.Transaction
import com.solis.tracker.feature.expenses.domain.model.TransactionKind
import com.solis.tracker.feature.expenses.domain.model.Budget
import com.solis.tracker.feature.expenses.domain.model.CategorySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

sealed interface ExpensesUiState {
    data object Loading : ExpensesUiState
    data class Success(
        val transactions: List<Transaction>,
        val categories: List<ExpenseCategory>,
        val currentMonthBudgets: List<Budget>,
        val currentMonthSummaries: List<CategorySummary>,
        val totalExpense: Long,
        val totalIncome: Long,
        val monthlyBudgetMinor: Long = 500000L,
        val saveError: String? = null
    ) : ExpensesUiState
}

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repository: ExpensesRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.seedDefaultsIfNeeded()
        }
    }

    private val now = Instant.now()
    private val zdt = ZonedDateTime.ofInstant(now, ZoneId.systemDefault())
    private val startOfMonth = zdt.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant()
    private val endOfMonth = zdt.plusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant().minusMillis(1)
    private val monthString = "${zdt.year}-${String.format(Locale.US, "%02d", zdt.monthValue)}"

    private val stateFlow1 = combine(
        repository.getAllTransactions(),
        repository.getAllCategories(),
        repository.getBudgetsForMonth(monthString)
    ) { t, c, b -> Triple(t, c, b) }

    private val stateFlow2 = combine(
        repository.getCategorySummaries(startOfMonth, endOfMonth),
        repository.getTotalAmountByKind(startOfMonth, endOfMonth, TransactionKind.EXPENSE),
        repository.getTotalAmountByKind(startOfMonth, endOfMonth, TransactionKind.INCOME)
    ) { s, e, i -> Triple(s, e, i) }

    private val _saveError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ExpensesUiState> = combine(
        stateFlow1, 
        stateFlow2, 
        _saveError,
        settingsRepository.userSettings
    ) { (transactions, categories, budgets), (summaries, totalExpense, totalIncome), error, settings ->
        ExpensesUiState.Success(
            transactions = transactions,
            categories = categories,
            currentMonthBudgets = budgets,
            currentMonthSummaries = summaries,
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            monthlyBudgetMinor = settings.monthlyBudgetMinor,
            saveError = error
        ) as ExpensesUiState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpensesUiState.Loading
    )

    private val _showTransactionSheet = MutableStateFlow(false)
    val showTransactionSheet = _showTransactionSheet.asStateFlow()

    fun openTransactionSheet() {
        _saveError.value = null
        _showTransactionSheet.value = true
    }

    fun closeTransactionSheet() {
        _showTransactionSheet.value = false
        _saveError.value = null
    }

    fun clearError() {
        _saveError.value = null
    }

    fun saveTransaction(
        amountStr: String,
        kind: TransactionKind,
        categoryId: String?,
        note: String
    ) {
        viewModelScope.launch {
            try {
                val amountMinor = try {
                    BigDecimal(amountStr).multiply(BigDecimal(100)).toLong()
                } catch (e: Exception) {
                    _saveError.value = "Invalid amount"
                    return@launch
                }
                
                if (amountMinor <= 0) {
                    _saveError.value = "Amount must be greater than zero"
                    return@launch
                }

                val categories = repository.getAllCategories().first()
                val category = categoryId?.let { id -> categories.find { it.id == id } } ?: categories.firstOrNull()
                
                if (category == null) {
                    _saveError.value = "No categories available to save this transaction."
                    return@launch
                }

                val transaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    kind = kind,
                    amountMinor = amountMinor,
                    currencyCode = "USD",
                    category = category,
                    account = null,
                    note = note,
                    occurredAt = Instant.now(),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
                repository.saveTransaction(transaction)
                closeTransactionSheet()
            } catch (e: Exception) {
                _saveError.value = "Failed to save transaction: ${e.message}"
            }
        }
    }
}
