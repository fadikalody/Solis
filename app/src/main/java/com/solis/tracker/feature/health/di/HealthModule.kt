package com.solis.tracker.feature.health.di

import com.solis.tracker.feature.health.data.HealthRepositoryImpl
import com.solis.tracker.feature.health.domain.HealthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthModule {
    @Binds
    abstract fun bindHealthRepository(
        impl: HealthRepositoryImpl
    ): HealthRepository
}
