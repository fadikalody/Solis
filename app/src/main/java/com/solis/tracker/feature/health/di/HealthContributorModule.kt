package com.solis.tracker.feature.health.di

import com.solis.tracker.core.extension.DashboardContributor
import com.solis.tracker.feature.health.ui.dashboard.HealthDashboardContributor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthContributorModule {
    @Binds
    @IntoSet
    abstract fun bindHealthDashboardContributor(
        impl: HealthDashboardContributor
    ): DashboardContributor
}
