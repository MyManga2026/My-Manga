package com.example.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_ID = "mymanga_updates_channel"
    private const val CHANNEL_NAME = "MyManga Updates & Reminders"
    private const val CHANNEL_DESC = "Notifications for new manga chapters, daily reading streaks, and library updates."

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isQuietHours(context: Context? = null): Boolean {
        // Quiet hours: only deep night 2:00 AM to 5:00 AM local device time if needed
        val calLocal = java.util.Calendar.getInstance()
        val hourLocal = calLocal.get(java.util.Calendar.HOUR_OF_DAY)
        return hourLocal in 2..4
    }

    fun sendNotification(
        context: Context,
        title: String,
        message: String,
        mangaId: Int? = null,
        chapterId: String? = null,
        chapterTitle: String? = null,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt(),
        bypassQuietHours: Boolean = true
    ) {
        if (!bypassQuietHours && isQuietHours(context)) {
            // Suppress only if quiet hours apply
            return
        }

        createNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            mangaId?.let { putExtra("EXTRA_MANGA_ID", it) }
            chapterId?.let { putExtra("EXTRA_CHAPTER_ID", it) }
            chapterTitle?.let { putExtra("EXTRA_CHAPTER_TITLE", it) }
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ) {
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            }
        } catch (_: Exception) {
            // Ignore if notification cannot be posted
        }
    }

    private var pendingSessionMangaTitle: String? = null
    private var pendingSessionMangaId: Int? = null
    private var pendingSessionChapterId: String? = null
    private var pendingSessionChapterTitle: String? = null
    private var hasPendingSessionProgress: Boolean = false

    fun recordReadingSession(
        mangaTitle: String,
        mangaId: Int,
        chapterId: String? = null,
        chapterTitle: String? = null
    ) {
        pendingSessionMangaTitle = mangaTitle
        pendingSessionMangaId = mangaId
        pendingSessionChapterId = chapterId
        pendingSessionChapterTitle = chapterTitle
        hasPendingSessionProgress = true
    }

    fun triggerAppCloseProgressNotification(context: Context) {
        if (hasPendingSessionProgress && !pendingSessionMangaTitle.isNullOrEmpty()) {
            val mangaId = pendingSessionMangaId
            if (mangaId != null && UserPreferences.isMangaFinished(context, mangaId)) {
                hasPendingSessionProgress = false
                return
            }
            val titleName = pendingSessionMangaTitle!!
            sendNotification(
                context = context,
                title = "Reading Progress Saved 📖",
                message = "$titleName manga progress saved",
                mangaId = pendingSessionMangaId,
                chapterId = pendingSessionChapterId,
                chapterTitle = pendingSessionChapterTitle
            )
            hasPendingSessionProgress = false
        }
    }

    fun sendNewChapterNotification(
        context: Context,
        mangaTitle: String = "Jujutsu Kaisen",
        chapterTitle: String = "Chapter 265: The Unforgiven",
        mangaId: Int = 101517,
        chapterId: String? = null
    ) {
        val username = UserPreferences.getUsername(context)
        val greeting = if (username.isNotBlank()) "Hey $username! " else ""
        sendNotification(
            context = context,
            title = "📖 New Chapter Available!",
            message = "${greeting}$mangaTitle - $chapterTitle is now ready to read on MyManga. Tap to read the latest chapter!",
            mangaId = mangaId,
            chapterId = chapterId,
            chapterTitle = chapterTitle,
            notificationId = 1001
        )
    }

    fun sendBookmarkOrHistoryUpdateNotification(
        context: Context,
        mangaTitle: String,
        newChapterName: String,
        mangaId: Int? = null,
        chapterId: String? = null
    ) {
        if (mangaId != null && UserPreferences.isMangaFinished(context, mangaId)) {
            return
        }
        val username = UserPreferences.getUsername(context)
        val nameLabel = if (username.isNotBlank()) "$username, " else ""
        sendNotification(
            context = context,
            title = "⚡ Chapter Update: $mangaTitle",
            message = "${nameLabel}a new chapter ($newChapterName) has been published for $mangaTitle! Tap to read the latest update now.",
            mangaId = mangaId,
            chapterId = chapterId,
            chapterTitle = newChapterName,
            notificationId = 2001
        )
    }

    fun sendReadingInactivityReminder(
        context: Context,
        mangaTitle: String,
        chapterTitle: String,
        hoursInactive: Long,
        mangaId: Int? = null,
        chapterId: String? = null
    ) {
        val username = UserPreferences.getUsername(context)
        val nameLabel = if (username.isNotBlank()) username else "Manga Reader"
        val greetings = listOf(
            "Kon'nichiwa, $nameLabel! 🌸",
            "Greetings, $nameLabel! ✨",
            "Ohayō, $nameLabel! ☕",
            "Welcome back, $nameLabel! 📚"
        )
        val selectedGreeting = greetings.random()
        val hoursText = if (hoursInactive >= 24) "${hoursInactive / 24}d ago" else "${hoursInactive}h ago"

        sendNotification(
            context = context,
            title = selectedGreeting,
            message = "You were reading '$mangaTitle' ($chapterTitle) $hoursText. Tap to resume your reading journey!",
            mangaId = mangaId,
            chapterId = chapterId,
            chapterTitle = chapterTitle,
            notificationId = 2002
        )
    }

    fun sendAppEngagementReminder(context: Context) {
        val username = UserPreferences.getUsername(context)
        val nameLabel = if (username.isNotBlank()) username else "Otaku"
        val messages = listOf(
            "Kon'nichiwa $nameLabel! 🎌 Fresh manga releases and daily trending updates are waiting in MyManga. Dive back in!",
            "Greetings, $nameLabel! ✨ Discover top-rated manga chapters and resume your saved library reading on MyManga.",
            "Manga Time, $nameLabel! 📚 Your favorite series have fresh updates available. Open MyManga to start reading!"
        )
        sendNotification(
            context = context,
            title = "🌸 MyManga Daily Digest",
            message = messages.random(),
            notificationId = 2003
        )
    }

    suspend fun checkAndSendSmartMangaNotifications(
        context: Context,
        historyList: List<com.example.data.local.HistoryEntity>,
        bookmarksList: List<com.example.data.local.BookmarkEntity>,
        forceTestMode: Boolean = false
    ) {
        if (!hasNotificationPermission(context)) return

        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences("mymanga_notif_prefs", Context.MODE_PRIVATE)
        val lastNotifTime = prefs.getLong("last_smart_notification_time", 0L)
        val twoHoursMs = (2 * 60 * 60 * 1000).toLong()

        if (!forceTestMode && (now - lastNotifTime < twoHoursMs)) {
            return
        }

        val twentyFourHoursMs = 24 * 60 * 60 * 1000L

        // 1. Check Reading History for >= 24h Inactivity (excluding finished manga)
        val activeHistory = historyList.filterNot { UserPreferences.isMangaFinished(context, it.mangaId) }
        val inactiveHistoryItem = activeHistory.firstOrNull {
            forceTestMode || (now - it.lastReadAt >= twentyFourHoursMs)
        }

        if (inactiveHistoryItem != null) {
            prefs.edit().putLong("last_smart_notification_time", now).apply()
            val hoursInactive = if (inactiveHistoryItem.lastReadAt > 0) {
                (now - inactiveHistoryItem.lastReadAt) / (1000 * 60 * 60)
            } else 24L

            sendReadingInactivityReminder(
                context = context,
                mangaTitle = inactiveHistoryItem.title,
                chapterTitle = inactiveHistoryItem.chapterTitle.ifBlank { "Chapter 1" },
                hoursInactive = if (hoursInactive < 24 && forceTestMode) 24L else hoursInactive,
                mangaId = inactiveHistoryItem.mangaId,
                chapterId = inactiveHistoryItem.chapterId
            )
            return
        }

        // 2. Check Bookmarked Manga for Updates (excluding finished titles)
        val activeBookmarks = bookmarksList.filterNot { bookmark ->
            UserPreferences.isMangaFinished(context, bookmark.mangaId) ||
            bookmark.status.equals("FINISHED", ignoreCase = true) ||
            bookmark.status.equals("COMPLETED", ignoreCase = true)
        }
        val recentBookmark = activeBookmarks.firstOrNull()
        if (recentBookmark != null) {
            prefs.edit().putLong("last_smart_notification_time", now).apply()
            sendBookmarkOrHistoryUpdateNotification(
                context = context,
                mangaTitle = recentBookmark.title,
                newChapterName = "Latest Chapter Update",
                mangaId = recentBookmark.mangaId
            )
            return
        }

        // 3. Fallback: Friendly App Retention Reminder
        prefs.edit().putLong("last_smart_notification_time", now).apply()
        sendAppEngagementReminder(context)
    }

    fun sendDailyStreakNotification(context: Context) {
        val username = UserPreferences.getUsername(context)
        val greeting = if (username.isNotBlank()) "Hey $username, " else ""
        sendNotification(
            context = context,
            title = "🔥 Keep Your Reading Streak Alive!",
            message = "${greeting}new trending manga chapters have dropped today. Tap to resume reading!",
            notificationId = 1002
        )
    }

    fun sendLibraryUpdateNotification(context: Context, bookmarkCount: Int) {
        val username = UserPreferences.getUsername(context)
        val nameLabel = if (username.isNotBlank()) "Hey $username, " else ""
        sendNotification(
            context = context,
            title = "📚 Library Update",
            message = "${nameLabel}you have $bookmarkCount manga saved in your MyManga library. Check for latest updates!",
            notificationId = 1003
        )
    }
}
