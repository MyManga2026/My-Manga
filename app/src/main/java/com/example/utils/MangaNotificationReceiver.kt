package com.example.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.MangaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MangaNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        // Schedule the next alarm 2.5 hours from now
        NotificationScheduler.scheduleNextNotification(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
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
                    chapterTitle = payload.chapterTitle
                )
            } catch (e: Exception) {
                val fallback = NotificationPool.getRandomFallbackNotification(context)
                NotificationHelper.sendNotification(
                    context = context,
                    title = fallback.title,
                    message = fallback.message
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
