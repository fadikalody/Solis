package com.solis.tracker.feature.tasks.di

import com.solis.tracker.core.backup.BackupContributor
import com.solis.tracker.feature.tasks.backup.TasksBackupContributor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class TasksBackupModule {
    @Binds
    @IntoSet
    abstract fun bindTasksBackupContributor(
        impl: TasksBackupContributor
    ): BackupContributor
}
