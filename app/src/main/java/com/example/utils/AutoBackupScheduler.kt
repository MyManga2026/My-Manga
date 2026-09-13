package com.example.utils

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object AutoBackupScheduler {

    private const val UNIQUE_WORK_NAME = "mymanga_24hour_auto_backup"
    const val INTERVAL_24_HOURS_HOURS = 24L

    /**
     * Schedules guaranteed WorkManager periodic task to update the backup file every 24 hours.
     * Persists across app restarts, reboots, and background states.
     */
    fun schedule24HourAutoBackup(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                INTERVAL_24_HOURS_HOURS, TimeUnit.HOURS,
                1, TimeUnit.HOURS // 1-hour flex window for battery optimization
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cancels the active 24-hour periodic backup work.
     */
    fun cancelAutoBackup(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Checks if 24-hour auto backup is enabled with a valid URI, and schedules it if so.
     * Safe to call on app startup.
     */
    fun checkAndSchedule(context: Context) {
        if (UserPreferences.isAutoBackupEnabled(context) && !UserPreferences.getAutoBackupUri(context).isNullOrBlank()) {
            schedule24HourAutoBackup(context)
        }
    }

    /**
     * Triggers an immediate one-time background update to the target file.
     */
    fun triggerImmediateAutoBackup(
        context: Context,
        onComplete: (BackupExportResult) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = AppDataBackupManager.performAutoBackup(context)
            withContext(Dispatchers.Main) {
                onComplete(result)
            }
        }
    }
}
