package com.solis.tracker.feature.expenses.di

import com.solis.tracker.core.extension.DashboardContributor
import com.solis.tracker.feature.expenses.ui.dashboard.ExpensesDashboardContributor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ExpensesContributorModule {
    @Binds
    @IntoSet
    abstract fun bindExpensesDashboardContributor(
        impl: ExpensesDashboardContributor
    ): DashboardContributor
}
