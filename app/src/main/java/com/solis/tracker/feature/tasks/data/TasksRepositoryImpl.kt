package com.solis.tracker.feature.tasks.data

import com.solis.tracker.core.common.AppDispatchers
import com.solis.tracker.core.common.Dispatcher
import com.solis.tracker.core.database.dao.TaskDao
import com.solis.tracker.feature.tasks.domain.TasksRepository
import com.solis.tracker.feature.tasks.domain.model.Task
import com.solis.tracker.feature.tasks.domain.model.TaskCategory
import com.solis.tracker.feature.tasks.domain.model.TaskStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasksRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    @Dispatcher(AppDispatchers.Io) private val ioDispatcher: CoroutineDispatcher
) : TasksRepository {
    
    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { list -> list.map { it.toDomain() } }
    }

    override fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> {
        return taskDao.getTasksByStatus(status.name).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTaskById(taskId: String): Task? = withContext(ioDispatcher) {
        taskDao.getTaskById(taskId)?.toDomain()
    }

    override suspend fun saveTask(task: Task) = withContext(ioDispatcher) {
        // Save the task entity
        val existingTask = taskDao.getTaskEntityById(task.id)
        if (existingTask == null) {
            taskDao.insertTask(task.toEntity())
        } else {
            taskDao.updateTask(task.toEntity())
        }

        // Save subtasks
        taskDao.deleteSubtasksByTaskId(task.id) // Clear old ones
        if (task.subtasks.isNotEmpty()) {
            taskDao.insertSubtasks(task.subtasks.map { it.toEntity() })
        }
    }

    override suspend fun deleteTask(taskId: String) = withContext(ioDispatcher) {
        taskDao.deleteTaskById(taskId)
    }

    override fun getAllCategories(): Flow<List<TaskCategory>> {
        return taskDao.getAllCategories().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveCategory(category: TaskCategory) = withContext(ioDispatcher) {
        taskDao.insertCategory(category.toEntity())
    }

    override suspend fun getCategoryById(categoryId: String): TaskCategory? = withContext(ioDispatcher) {
        taskDao.getCategoryById(categoryId)?.toDomain()
    }

    override suspend fun getCategoryCount(): Int = withContext(ioDispatcher) {
        taskDao.getCategoryCount()
    }
}
