package com.solis.tracker.feature.tasks.ui

import com.solis.tracker.core.notifications.TaskScheduler
import com.solis.tracker.feature.tasks.domain.TasksRepository
import com.solis.tracker.feature.tasks.domain.model.Task
import com.solis.tracker.feature.tasks.domain.model.TaskPriority
import com.solis.tracker.feature.tasks.domain.model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {
    private lateinit var repository: TasksRepository
    private lateinit var taskScheduler: TaskScheduler
    private lateinit var viewModel: TasksViewModel

    private val testDispatcher = UnconfinedTestDispatcher()
    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
    private val categoriesFlow = MutableStateFlow<List<com.solis.tracker.feature.tasks.domain.model.TaskCategory>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        taskScheduler = mock()

        whenever(repository.getAllTasks()).thenReturn(tasksFlow)
        whenever(repository.getAllCategories()).thenReturn(categoriesFlow)

        viewModel = TasksViewModel(repository, taskScheduler)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveTask creates new task and schedules reminder`() = runTest {
        val reminderTime = Instant.now().plusSeconds(3600)
        viewModel.saveTask(
            title = "Test Task",
            notes = "Notes",
            dueDate = null,
            dueTime = null,
            priority = TaskPriority.HIGH,
            categoryId = null,
            reminderAt = reminderTime,
            repeatRule = null
        )

        verify(repository).saveTask(any())
        verify(taskScheduler).scheduleReminder(any(), any())
    }

    @Test
    fun `toggleTaskComplete marks open task as done and emits undo event`() = runTest {
        val task = Task(
            id = "1",
            title = "Test",
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.OPEN,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val undoEvents = mutableListOf<Task>()
        val job = backgroundScope.launch(testDispatcher) {
            viewModel.undoEvent.collect { undoEvents.add(it) }
        }

        viewModel.toggleTaskComplete(task)

        verify(repository).saveTask(any())
        assertTrue(undoEvents.isNotEmpty())
        assertEquals(task.id, undoEvents.first().id)
        
        job.cancel()
    }

    @Test
    fun `toggleTaskComplete generates next occurrence for repeating task`() = runTest {
        val task = Task(
            id = "1",
            title = "Daily Task",
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.OPEN,
            dueDate = java.time.LocalDate.now(),
            repeatRule = "DAILY",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        viewModel.toggleTaskComplete(task)

        // It should save the completed task and the new next occurrence task
        // We can check if saveTask was called twice or just verify interactions
        verify(repository, org.mockito.kotlin.times(2)).saveTask(any())
    }
}
