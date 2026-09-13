package com.example.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * WorkManager worker that runs automatically every 24 hours to export and update
 * the latest bookmarks, reading history, downloads, and user preferences into the
 * same file selected by the user.
 */
class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!UserPreferences.isAutoBackupEnabled(applicationContext)) {
            return Result.success()
        }

        val targetUri = UserPreferences.getAutoBackupUri(applicationContext)
        if (targetUri.isNullOrBlank()) {
            return Result.success()
        }

        return try {
            val result = AppDataBackupManager.performAutoBackup(applicationContext)
            if (result.success) {
                // Post confirmation notification if enabled
                try {
                    NotificationHelper.sendNotification(
                        context = applicationContext,
                        title = "Auto-Backup Updated 📦",
                        message = "Your reading history and bookmarks were automatically backed up to your selected file.",
                        bypassQuietHours = true
                    )
                } catch (_: Exception) {}

                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
