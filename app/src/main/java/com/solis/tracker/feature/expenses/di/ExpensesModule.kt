package com.solis.tracker.feature.expenses.di

import com.solis.tracker.feature.expenses.data.ExpensesRepositoryImpl
import com.solis.tracker.feature.expenses.domain.ExpensesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ExpensesModule {
    @Binds
    abstract fun bindExpensesRepository(
        impl: ExpensesRepositoryImpl
    ): ExpensesRepository
}
