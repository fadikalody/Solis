package com.solis.tracker.feature.health.di

import com.solis.tracker.core.backup.BackupContributor
import com.solis.tracker.feature.health.backup.HealthBackupContributor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthBackupModule {
    @Binds
    @IntoSet
    abstract fun bindHealthBackupContributor(
        impl: HealthBackupContributor
    ): BackupContributor
}
