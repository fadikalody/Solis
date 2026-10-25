package com.solis.tracker.feature.tasks.domain

import com.solis.tracker.feature.tasks.domain.model.Task
import com.solis.tracker.feature.tasks.domain.model.TaskCategory
import com.solis.tracker.feature.tasks.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

interface TasksRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>>
    suspend fun getTaskById(taskId: String): Task?
    suspend fun saveTask(task: Task)
    suspend fun deleteTask(taskId: String)
    
    // Categories
    fun getAllCategories(): Flow<List<TaskCategory>>
    suspend fun saveCategory(category: TaskCategory)
    suspend fun getCategoryById(categoryId: String): TaskCategory?
    suspend fun getCategoryCount(): Int
}
