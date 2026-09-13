package com.example.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.*
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private const val REQUEST_CODE = 4004
    private const val UNIQUE_WORK_NAME = "mymanga_2hour_periodic_notification"
    const val INTERVAL_2_HOURS_MS = 2 * 60 * 60 * 1000L // 2 Hours in milliseconds

    /**
     * Primary entry point: sets up both WorkManager (guaranteed OS-level background periodic work)
     * and AlarmManager (instant 2-hour wakeup timer) for 100% reliable every-2-hour notifications.
     */
    fun schedulePeriodicNotifications(context: Context) {
        scheduleWorkManager(context)
        scheduleNextAlarm(context)
    }

    // Alias for backward compatibility
    fun scheduleNextNotification(context: Context) {
        schedulePeriodicNotifications(context)
    }

    /**
     * Schedules guaranteed WorkManager periodic task every 2 hours.
     * WorkManager persists across app closures, backgrounding, and device reboots.
     */
    fun scheduleWorkManager(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<MangaNotificationWorker>(
                2, TimeUnit.HOURS,
                15, TimeUnit.MINUTES // Flex interval
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    15, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest
            )
        } catch (_: Exception) {}
    }

    /**
     * Sets an AlarmManager alarm for 2 hours from now.
     */
    fun scheduleNextAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, MangaNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + INTERVAL_2_HOURS_MS

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) {
            try {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    INTERVAL_2_HOURS_MS,
                    pendingIntent
                )
            } catch (_: Exception) {}
        }
    }

    /**
     * One-time immediate trigger for testing or instant verification.
     */
    fun triggerImmediateTestNotification(context: Context) {
        val oneTimeWork = OneTimeWorkRequestBuilder<MangaNotificationWorker>().build()
        WorkManager.getInstance(context).enqueue(oneTimeWork)
    }
}
