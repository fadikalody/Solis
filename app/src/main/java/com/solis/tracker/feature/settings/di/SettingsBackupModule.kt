package com.solis.tracker.feature.settings.di

import com.solis.tracker.core.backup.BackupContributor
import com.solis.tracker.feature.settings.backup.SettingsBackupContributor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsBackupModule {
    @Binds
    @IntoSet
    abstract fun bindSettingsBackupContributor(
        impl: SettingsBackupContributor
    ): BackupContributor
}
