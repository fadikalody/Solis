package com.solis.tracker.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.solis.tracker.feature.tasks.domain.TasksRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var tasksRepository: TasksRepository
    @Inject lateinit var taskScheduler: TaskScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            scope.launch {
                val tasks = tasksRepository.getAllTasks().first()
                val now = Instant.now()
                tasks.forEach { task ->
                    if (task.reminderAt != null && task.reminderAt.isAfter(now)) {
                        taskScheduler.scheduleReminder(task.id, task.reminderAt)
                    }
                }
            }
        }
    }
}
