package com.solis.tracker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.solis.tracker.core.database.entity.SubtaskEntity
import com.solis.tracker.core.database.entity.TaskCategoryEntity
import com.solis.tracker.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TaskDao {
    @Transaction
    @Query("SELECT * FROM task ORDER BY dueDate ASC, priority DESC, createdAt DESC")
    fun getAllTasks(): Flow<List<TaskWithSubtasksAndCategory>>
    
    @Transaction
    @Query("SELECT * FROM task WHERE status = :status ORDER BY dueDate ASC, priority DESC, createdAt DESC")
    fun getTasksByStatus(status: String): Flow<List<TaskWithSubtasksAndCategory>>

    @Transaction
    @Query("SELECT * FROM task WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskWithSubtasksAndCategory?
    
    @Query("SELECT * FROM task WHERE id = :taskId")
    suspend fun getTaskEntityById(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM task WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    // Subtasks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Query("DELETE FROM subtask WHERE taskId = :taskId")
    suspend fun deleteSubtasksByTaskId(taskId: String)

    // Categories
    @Query("SELECT * FROM task_category ORDER BY name ASC")
    fun getAllCategories(): Flow<List<TaskCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: TaskCategoryEntity)
    
    @Query("SELECT * FROM task_category WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: String): TaskCategoryEntity?

    @Query("SELECT COUNT(*) FROM task_category")
    suspend fun getCategoryCount(): Int
}
