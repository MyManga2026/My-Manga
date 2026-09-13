package com.example.utils

import android.content.Context
import com.example.data.local.HistoryEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ReadingHeatmapDay(
    val dateKey: String, // "yyyy-MM-dd"
    val dayOfWeek: String, // "Su", "M", "Tu", "W", "Th", "F", "Sa"
    val dayOfMonth: Int,
    val chaptersReadCount: Int,
    val readingSeconds: Long,
    val isToday: Boolean
)

data class GenreStatsItem(
    val genre: String,
    val count: Int,
    val percentage: Float
)

data class ReadingAnalyticsSummary(
    val totalChaptersRead: Int,
    val totalReadingSeconds: Long,
    val formattedReadingTime: String,
    val currentDailyStreak: Int,
    val bestDailyStreak: Int,
    val weeklyHeatmap: List<ReadingHeatmapDay>,
    val monthlyHeatmap: List<ReadingHeatmapDay>,
    val topGenres: List<GenreStatsItem>,
    val mostActiveDay: String,
    val averageChaptersPerDay: Float
)

object ReadingAnalyticsManager {

    private const val PREFS_NAME = "mymanga_reading_analytics_prefs"
    private const val KEY_PREFIX_READING_TIME = "read_sec_" // + dateKey
    private const val KEY_TOTAL_READING_SECONDS = "total_reading_seconds"
    private const val KEY_PREFIX_DAY_CHAPTERS = "day_chaps_" // + dateKey
    private const val KEY_GENRE_READ_COUNT_PREFIX = "genre_count_"
    private const val KEY_BEST_STREAK = "best_reading_streak"

    private val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun getTodayKey(): String {
        return dayKeyFormat.format(Date())
    }

    /**
     * Record time spent reading manga in seconds (called from Reader periodically or on pause).
     */
    fun recordReadingTime(context: Context, secondsToAdd: Long, genreList: List<String> = emptyList()) {
        if (secondsToAdd <= 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayKey = getTodayKey()

        val currentDaySecs = prefs.getLong(KEY_PREFIX_READING_TIME + todayKey, 0L)
        val totalSecs = prefs.getLong(KEY_TOTAL_READING_SECONDS, 0L)

        val editor = prefs.edit()
        editor.putLong(KEY_PREFIX_READING_TIME + todayKey, currentDaySecs + secondsToAdd)
        editor.putLong(KEY_TOTAL_READING_SECONDS, totalSecs + secondsToAdd)

        for (g in genreList) {
            val clean = g.trim()
            if (clean.isNotBlank()) {
                val currentG = prefs.getInt(KEY_GENRE_READ_COUNT_PREFIX + clean, 0)
                editor.putInt(KEY_GENRE_READ_COUNT_PREFIX + clean, currentG + 1)
            }
        }
        editor.apply()
    }

    /**
     * Increment chapter counter for today when a user finishes or progresses a chapter.
     */
    fun recordChapterRead(context: Context, genreList: List<String> = emptyList()) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayKey = getTodayKey()
        val currentCount = prefs.getInt(KEY_PREFIX_DAY_CHAPTERS + todayKey, 0)

        val editor = prefs.edit()
        editor.putInt(KEY_PREFIX_DAY_CHAPTERS + todayKey, currentCount + 1)

        // If active timer wasn't tracked today, add 3 minutes (180s) baseline
        val daySecs = prefs.getLong(KEY_PREFIX_READING_TIME + todayKey, 0L)
        if (daySecs == 0L) {
            editor.putLong(KEY_PREFIX_READING_TIME + todayKey, 180L)
            val totalSecs = prefs.getLong(KEY_TOTAL_READING_SECONDS, 0L)
            editor.putLong(KEY_TOTAL_READING_SECONDS, totalSecs + 180L)
        }

        for (g in genreList) {
            val clean = g.trim()
            if (clean.isNotBlank()) {
                val currentG = prefs.getInt(KEY_GENRE_READ_COUNT_PREFIX + clean, 0)
                editor.putInt(KEY_GENRE_READ_COUNT_PREFIX + clean, currentG + 1)
            }
        }
        editor.apply()
    }

    /**
     * Build comprehensive reading analytics combining persisted reading history entities and session times.
     */
    fun calculateAnalytics(context: Context, historyList: List<HistoryEntity>): ReadingAnalyticsSummary {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayKey = getTodayKey()

        // 1. Aggregate history dates & chapters
        val dateToChapterCount = mutableMapOf<String, Int>()
        var historyDistinctChapters = 0

        for (item in historyList) {
            if (item.lastReadAt > 0) {
                historyDistinctChapters++
                val dateStr = dayKeyFormat.format(Date(item.lastReadAt))
                dateToChapterCount[dateStr] = (dateToChapterCount[dateStr] ?: 0) + 1
            }
        }

        // Also merge explicitly recorded day chapters from preferences
        var dayPrefChaptersSum = 0
        val allPrefs = prefs.all
        for ((key, value) in allPrefs) {
            if (key.startsWith(KEY_PREFIX_DAY_CHAPTERS) && value is Int && value > 0) {
                val dateStr = key.removePrefix(KEY_PREFIX_DAY_CHAPTERS)
                val existing = dateToChapterCount[dateStr] ?: 0
                dateToChapterCount[dateStr] = maxOf(existing, value)
                dayPrefChaptersSum += value
            }
        }

        // Total chapters read: real calculated count
        val totalChapters = maxOf(historyDistinctChapters, dateToChapterCount.values.sum(), dayPrefChaptersSum)

        // 2. Calculate Reading Time
        var totalReadingSecs = prefs.getLong(KEY_TOTAL_READING_SECONDS, 0L)
        if (totalReadingSecs <= 0L && totalChapters > 0) {
            // Estimated reading time if user read chapters without continuous background timer
            totalReadingSecs = totalChapters * 180L
        } else if (totalChapters == 0) {
            totalReadingSecs = 0L
        }

        val formattedTime = formatTime(totalReadingSecs)

        // 3. Calculate Current Streak and Best Streak
        val (currentStreak, bestStreak) = calculateStreaks(prefs, dateToChapterCount.filter { it.value > 0 }.keys)

        // 4. Generate Weekly Heatmap (Last 7 Days ending today)
        val weeklyHeatmap = generateWeeklyHeatmap(prefs, dateToChapterCount)

        // 5. Generate Monthly Heatmap (Last 28 Days ending today)
        val monthlyHeatmap = generateMonthlyHeatmap(prefs, dateToChapterCount)

        // 6. Calculate Top Favorite Genres
        val topGenres = calculateTopGenres(prefs, historyList)

        // 7. Most active day & average
        val dayOfWeekCount = mutableMapOf<String, Int>()
        val dayFormat = SimpleDateFormat("EEEE", Locale.US)
        for (item in historyList) {
            if (item.lastReadAt > 0) {
                val dayName = dayFormat.format(Date(item.lastReadAt))
                dayOfWeekCount[dayName] = (dayOfWeekCount[dayName] ?: 0) + 1
            }
        }
        val mostActive = if (dayOfWeekCount.isNotEmpty()) {
            dayOfWeekCount.maxByOrNull { it.value }?.key ?: "None"
        } else {
            "None"
        }
        val activeDaysCount = dateToChapterCount.count { it.value > 0 }
        val avgChapters = if (activeDaysCount > 0 && totalChapters > 0) {
            totalChapters.toFloat() / activeDaysCount.toFloat()
        } else {
            0.0f
        }

        return ReadingAnalyticsSummary(
            totalChaptersRead = totalChapters,
            totalReadingSeconds = totalReadingSecs,
            formattedReadingTime = formattedTime,
            currentDailyStreak = currentStreak,
            bestDailyStreak = bestStreak,
            weeklyHeatmap = weeklyHeatmap,
            monthlyHeatmap = monthlyHeatmap,
            topGenres = topGenres,
            mostActiveDay = mostActive,
            averageChaptersPerDay = ((avgChapters * 10).toInt() / 10f)
        )
    }

    private fun calculateStreaks(
        prefs: android.content.SharedPreferences,
        activeDateKeys: Set<String>
    ): Pair<Int, Int> {
        val savedBest = prefs.getInt(KEY_BEST_STREAK, 0)
        if (activeDateKeys.isEmpty()) {
            return Pair(0, savedBest)
        }

        val cal = Calendar.getInstance()
        var currentStreak = 0

        val todayStr = dayKeyFormat.format(cal.time)
        val readToday = activeDateKeys.contains(todayStr)

        if (readToday) {
            currentStreak = 1
            cal.add(Calendar.DAY_OF_YEAR, -1)
            while (true) {
                val dStr = dayKeyFormat.format(cal.time)
                if (activeDateKeys.contains(dStr)) {
                    currentStreak++
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        } else {
            // Check yesterday to maintain streak
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dayKeyFormat.format(cal.time)
            if (activeDateKeys.contains(yesterdayStr)) {
                currentStreak = 1
                cal.add(Calendar.DAY_OF_YEAR, -1)
                while (true) {
                    val dStr = dayKeyFormat.format(cal.time)
                    if (activeDateKeys.contains(dStr)) {
                        currentStreak++
                        cal.add(Calendar.DAY_OF_YEAR, -1)
                    } else {
                        break
                    }
                }
            } else {
                currentStreak = 0
            }
        }

        val bestStreak = maxOf(savedBest, currentStreak)
        if (bestStreak > savedBest) {
            prefs.edit().putInt(KEY_BEST_STREAK, bestStreak).apply()
        }

        return Pair(currentStreak, bestStreak)
    }

    private fun generateWeeklyHeatmap(
        prefs: android.content.SharedPreferences,
        dateToChapterCount: Map<String, Int>
    ): List<ReadingHeatmapDay> {
        val days = mutableListOf<ReadingHeatmapDay>()
        val cal = Calendar.getInstance()
        val todayStr = dayKeyFormat.format(cal.time)

        val dayLetters = listOf("Su", "M", "Tu", "W", "Th", "F", "Sa")

        // Go back 6 days to get 7 days ending today
        cal.add(Calendar.DAY_OF_YEAR, -6)

        for (i in 0 until 7) {
            val dateKey = dayKeyFormat.format(cal.time)
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val dayOfWeekIndex = cal.get(Calendar.DAY_OF_WEEK) - 1
            val dayLetter = dayLetters.getOrElse(dayOfWeekIndex) { "D" }

            val chaps = dateToChapterCount[dateKey] ?: 0
            var secs = prefs.getLong(KEY_PREFIX_READING_TIME + dateKey, 0L)
            if (secs == 0L && chaps > 0) {
                secs = chaps * 180L
            }

            days.add(
                ReadingHeatmapDay(
                    dateKey = dateKey,
                    dayOfWeek = dayLetter,
                    dayOfMonth = dayOfMonth,
                    chaptersReadCount = chaps,
                    readingSeconds = secs,
                    isToday = (dateKey == todayStr)
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return days
    }

    private fun generateMonthlyHeatmap(
        prefs: android.content.SharedPreferences,
        dateToChapterCount: Map<String, Int>
    ): List<ReadingHeatmapDay> {
        val days = mutableListOf<ReadingHeatmapDay>()
        val cal = Calendar.getInstance()
        val todayStr = dayKeyFormat.format(cal.time)

        val dayLetters = listOf("Su", "M", "Tu", "W", "Th", "F", "Sa")

        // Generate the last 28 days (4 full weeks ending today)
        cal.add(Calendar.DAY_OF_YEAR, -27)

        for (i in 0 until 28) {
            val dateKey = dayKeyFormat.format(cal.time)
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val dayOfWeekIndex = cal.get(Calendar.DAY_OF_WEEK) - 1
            val dayLetter = dayLetters.getOrElse(dayOfWeekIndex) { "D" }

            val chaps = dateToChapterCount[dateKey] ?: 0
            var secs = prefs.getLong(KEY_PREFIX_READING_TIME + dateKey, 0L)
            if (secs == 0L && chaps > 0) {
                secs = chaps * 180L
            }

            days.add(
                ReadingHeatmapDay(
                    dateKey = dateKey,
                    dayOfWeek = dayLetter,
                    dayOfMonth = dayOfMonth,
                    chaptersReadCount = chaps,
                    readingSeconds = secs,
                    isToday = (dateKey == todayStr)
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return days
    }

    private fun calculateTopGenres(
        prefs: android.content.SharedPreferences,
        historyList: List<HistoryEntity>
    ): List<GenreStatsItem> {
        val genreCounts = mutableMapOf<String, Int>()

        // Read from recorded genre counts in prefs
        val allPrefs = prefs.all
        for ((k, v) in allPrefs) {
            if (k.startsWith(KEY_GENRE_READ_COUNT_PREFIX) && v is Int && v > 0) {
                val g = k.removePrefix(KEY_GENRE_READ_COUNT_PREFIX)
                genreCounts[g] = (genreCounts[g] ?: 0) + v
            }
        }

        if (genreCounts.isEmpty()) {
            return emptyList()
        }

        val totalScore = genreCounts.values.sum().coerceAtLeast(1)

        return genreCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { entry ->
                GenreStatsItem(
                    genre = entry.key,
                    count = entry.value,
                    percentage = ((entry.value.toFloat() / totalScore.toFloat()) * 100f).coerceIn(1f, 100f)
                )
            }
    }

    private fun formatTime(totalSeconds: Long): String {
        if (totalSeconds <= 0L) return "0m"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }
}
