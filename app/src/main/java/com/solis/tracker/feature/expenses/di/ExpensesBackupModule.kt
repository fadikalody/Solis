package com.solis.tracker.feature.expenses.di

import com.solis.tracker.core.backup.BackupContributor
import com.solis.tracker.feature.expenses.backup.ExpensesBackupContributor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ExpensesBackupModule {
    @Binds
    @IntoSet
    abstract fun bindExpensesBackupContributor(
        impl: ExpensesBackupContributor
    ): BackupContributor
}
