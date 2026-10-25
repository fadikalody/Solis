package com.solis.tracker.feature.tasks.di

import com.solis.tracker.core.extension.DashboardContributor
import com.solis.tracker.feature.tasks.ui.dashboard.TasksDashboardContributor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class TasksContributorModule {
    @Binds
    @IntoSet
    abstract fun bindTasksDashboardContributor(
        impl: TasksDashboardContributor
    ): DashboardContributor
}
