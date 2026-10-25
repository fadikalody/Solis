package com.solis.tracker.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solis.tracker.core.database.AppDatabase
import com.solis.tracker.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        taskDao = database.taskDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetTasks() = runBlocking {
        val task = TaskEntity(
            id = "1",
            title = "Test Task",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        taskDao.insertTask(task)

        val tasks = taskDao.getAllTasks().first()
        assertEquals(1, tasks.size)
        assertEquals("Test Task", tasks[0].task.title)
    }

    @Test
    fun updateTask() = runBlocking {
        val task = TaskEntity(
            id = "1",
            title = "Test Task",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        taskDao.insertTask(task)

        val updatedTask = task.copy(title = "Updated Task")
        taskDao.updateTask(updatedTask)

        val retrievedTask = taskDao.getTaskById("1")
        assertEquals("Updated Task", retrievedTask?.task?.title)
    }

    @Test
    fun deleteTask() = runBlocking {
        val task = TaskEntity(
            id = "1",
            title = "Test Task",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        taskDao.insertTask(task)
        taskDao.deleteTaskById("1")

        val retrievedTask = taskDao.getTaskById("1")
        assertEquals(null, retrievedTask)
    }
}
