package com.solis.tracker.feature.tasks.di

import com.solis.tracker.feature.tasks.data.TasksRepositoryImpl
import com.solis.tracker.feature.tasks.domain.TasksRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TasksModule {
    @Binds
    abstract fun bindTasksRepository(
        impl: TasksRepositoryImpl
    ): TasksRepository
}
