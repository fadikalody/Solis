package com.solis.tracker.core.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleReminder(taskId: String, reminderAt: Instant) {
        val now = Instant.now()
        if (reminderAt.isBefore(now)) return
        
        val delayMillis = reminderAt.toEpochMilli() - now.toEpochMilli()

        val inputData = Data.Builder()
            .putString(TaskReminderWorker.KEY_TASK_ID, taskId)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "task_reminder_$taskId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelReminder(taskId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("task_reminder_$taskId")
    }
}
