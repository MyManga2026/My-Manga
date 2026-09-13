package com.example.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.MangaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MangaNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val context = applicationContext

            if (!NotificationHelper.hasNotificationPermission(context)) {
                return@withContext Result.success()
            }

            val db = MangaDatabase.getDatabase(context)
            val dao = db.mangaDao()

            val bookmarks = try { dao.getBookmarksList() } catch (_: Exception) { emptyList() }
            val historyList = try { dao.getHistoryList() } catch (_: Exception) { emptyList() }

            val payload = NotificationPool.getRandomNotification(
                bookmarks = bookmarks,
                history = historyList,
                context = context
            )

            NotificationHelper.sendNotification(
                context = context,
                title = payload.title,
                message = payload.message,
                mangaId = payload.mangaId,
                chapterId = payload.chapterId,
                chapterTitle = payload.chapterTitle,
                bypassQuietHours = false
            )

            // Also keep alarm scheduler in sync
            NotificationScheduler.scheduleNextAlarm(context)

            Result.success()
        } catch (e: Exception) {
            try {
                val fallback = NotificationPool.getRandomFallbackNotification(applicationContext)
                NotificationHelper.sendNotification(
                    context = applicationContext,
                    title = fallback.title,
                    message = fallback.message
                )
            } catch (_: Exception) {}
            Result.success()
        }
    }
}
