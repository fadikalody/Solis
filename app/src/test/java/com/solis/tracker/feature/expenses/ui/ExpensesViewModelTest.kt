package com.solis.tracker.feature.expenses.ui

import app.cash.turbine.test
import com.solis.tracker.core.datastore.SettingsRepository
import com.solis.tracker.core.datastore.UserSettings
import com.solis.tracker.feature.expenses.domain.ExpensesRepository
import com.solis.tracker.feature.expenses.domain.model.Budget
import com.solis.tracker.feature.expenses.domain.model.CategorySummary
import com.solis.tracker.feature.expenses.domain.model.ExpenseCategory
import com.solis.tracker.feature.expenses.domain.model.Transaction
import com.solis.tracker.feature.expenses.domain.model.TransactionKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModelTest {
    private lateinit var repository: ExpensesRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: ExpensesViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    private val categoriesFlow = MutableStateFlow<List<ExpenseCategory>>(
        listOf(ExpenseCategory("cat1", "Test Cat", "", "", TransactionKind.EXPENSE, true))
    )
    private val budgetsFlow = MutableStateFlow<List<Budget>>(emptyList())
    private val summariesFlow = MutableStateFlow<List<CategorySummary>>(emptyList())
    private val totalExpenseFlow = MutableStateFlow(0L)
    private val totalIncomeFlow = MutableStateFlow(0L)
    private val userSettingsFlow = MutableStateFlow(UserSettings(monthlyBudgetMinor = 500000L))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        settingsRepository = mock()

        whenever(repository.getAllTransactions()).thenReturn(transactionsFlow)
        whenever(repository.getAllCategories()).thenReturn(categoriesFlow)
        whenever(repository.getBudgetsForMonth(any())).thenReturn(budgetsFlow)
        whenever(repository.getCategorySummaries(any(), any())).thenReturn(summariesFlow)
        whenever(repository.getTotalAmountByKind(any(), any(), org.mockito.kotlin.eq(TransactionKind.EXPENSE))).thenReturn(totalExpenseFlow)
        whenever(repository.getTotalAmountByKind(any(), any(), org.mockito.kotlin.eq(TransactionKind.INCOME))).thenReturn(totalIncomeFlow)
        
        whenever(settingsRepository.userSettings).thenReturn(userSettingsFlow)

        viewModel = ExpensesViewModel(repository, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveTransaction parses valid amount and saves successfully`() = runTest(testDispatcher) {
        viewModel.saveTransaction("12.34", TransactionKind.EXPENSE, "cat1", "Test")

        advanceUntilIdle()

        val captor = argumentCaptor<Transaction>()
        verify(repository).saveTransaction(captor.capture())

        val savedTx = captor.firstValue
        assertEquals(1234L, savedTx.amountMinor)
        assertEquals("Test", savedTx.note)
        assertEquals(TransactionKind.EXPENSE, savedTx.kind)
    }

    @Test
    fun `saveTransaction ignores invalid amount`() = runTest {
        viewModel.saveTransaction("invalid", TransactionKind.EXPENSE, "cat1", "Test")
        viewModel.saveTransaction("0", TransactionKind.EXPENSE, "cat1", "Test")
        viewModel.saveTransaction("-10", TransactionKind.EXPENSE, "cat1", "Test")
        
        // saveTransaction should not be called
        verify(repository, org.mockito.kotlin.never()).saveTransaction(any())
    }

    @Test
    fun `uiState combines all flows successfully`() = runTest {
        // Emit values to the flows
        transactionsFlow.value = listOf(mock())
        categoriesFlow.value = listOf(mock())
        budgetsFlow.value = listOf(mock())
        summariesFlow.value = listOf(mock())
        totalExpenseFlow.value = 1000L
        totalIncomeFlow.value = 5000L
        
        viewModel.uiState.test {
            // Because inputs are ready, it should emit Success soon
            val finalState = expectMostRecentItem()
            
            assertTrue(finalState is ExpensesUiState.Success)
            val success = finalState as ExpensesUiState.Success

            assertEquals(1, success.transactions.size)
            assertEquals(1, success.categories.size)
            assertEquals(1, success.currentMonthBudgets.size)
            assertEquals(1, success.currentMonthSummaries.size)
            assertEquals(1000L, success.totalExpense)
            assertEquals(5000L, success.totalIncome)
            assertEquals(500000L, success.monthlyBudgetMinor)
        }
    }
}
