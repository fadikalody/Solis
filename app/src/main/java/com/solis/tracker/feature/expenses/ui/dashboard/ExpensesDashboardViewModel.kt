package com.solis.tracker.feature.expenses.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solis.tracker.core.datastore.SettingsRepository
import com.solis.tracker.feature.expenses.domain.ExpensesRepository
import com.solis.tracker.feature.expenses.domain.model.TransactionKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ExpensesDashboardState(
    val todaySpend: Long = 0L,
    val monthlySpend: Long = 0L,
    val monthlyBudget: Long = 500000L
)

@HiltViewModel
class ExpensesDashboardViewModel @Inject constructor(
    private val repository: ExpensesRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val now = Instant.now()
    private val zdt = ZonedDateTime.ofInstant(now, ZoneId.systemDefault())
    private val startOfDay = zdt.truncatedTo(ChronoUnit.DAYS).toInstant()
    private val startOfMonth = zdt.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant()
    private val endOfMonth = zdt.plusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant().minusMillis(1)

    val state: StateFlow<ExpensesDashboardState> = combine(
        repository.getTotalAmountByKind(startOfDay, now, TransactionKind.EXPENSE),
        repository.getTotalAmountByKind(startOfMonth, endOfMonth, TransactionKind.EXPENSE),
        settingsRepository.userSettings
    ) { today, monthly, settings ->
        ExpensesDashboardState(
            todaySpend = today,
            monthlySpend = monthly,
            monthlyBudget = settings.monthlyBudgetMinor
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpensesDashboardState()
    )
}
