package com.example.utils

import android.content.Context
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import kotlin.random.Random

data class NotificationPayload(
    val title: String,
    val message: String,
    val mangaId: Int? = null,
    val chapterId: String? = null,
    val chapterTitle: String? = null
)

object NotificationPool {

    // 50+ Diverse Notification Messages across 5 Core Categories
    private val staticPool = listOf(
        // Category 1: Continue Reading / Reading Progress Reminders (10)
        NotificationPayload("Continue Your Reading 📖", "Your current chapter is waiting! Jump back in and see what happens next."),
        NotificationPayload("Unread Chapters Pending ⏳", "Don't leave the cliffhanger hanging! Tap to resume your latest manga."),
        NotificationPayload("Pick Up Where You Left Off 📚", "Your reading progress was saved. Continue enjoying your favorite story."),
        NotificationPayload("Story Continuation Alert 📖", "The action is just getting started. Dive back into your reading session!"),
        NotificationPayload("Reading Time Reminder ☕", "Take a relaxing break and continue your favorite manga series now."),
        NotificationPayload("Cliffhanger Awaits! 😱", "Find out how your heroes escape in the next chapter. Tap to read!"),
        NotificationPayload("Daily Reading Session 🌟", "Keep your daily reading streak alive! Spend a few minutes with your favorite manga."),
        NotificationPayload("Your Bookmark is Calling 🔖", "Ready to see what happens next? Your saved progress is ready for you."),
        NotificationPayload("Next Chapter Ready 🚀", "Smooth reading experience awaits. Resume your manga with one tap."),
        NotificationPayload("Night Time Manga Reading 🌙", "Relax and unwind with a few exciting manga chapters before bed."),

        // Category 2: Bookmark & Favorites Reminders (10)
        NotificationPayload("Bookmark Spotlight ⭐", "Have you checked your bookmarked collection today? Your favorite manga awaits!"),
        NotificationPayload("Favorite Manga Check 💖", "Your library is full of top-tier manga. Explore your bookmarked titles now."),
        NotificationPayload("Revisit Your Saved Classics 🏆", "Re-read your favorite epic moments in your bookmarked library."),
        NotificationPayload("Library Update 📁", "Organize your bookmarks and catch up on unread chapters."),
        NotificationPayload("Top Pick in Your Library 🎯", "A saved masterpiece is waiting in your bookmarks. Tap to start reading."),
        NotificationPayload("Bookmark Reminder 📌", "Don't forget the amazing stories you saved earlier today!"),
        NotificationPayload("Don't Miss Out! 🔥", "Your bookmarked series has exciting updates. Check your library now!"),
        NotificationPayload("Favorite Character Alert 🗡️", "Your favorite characters are waiting for you in your library!"),
        NotificationPayload("Your Private Collection 📚", "Jump into your customized library and enjoy ad-free reading."),
        NotificationPayload("Manga Vault Unlocked 🔐", "Your saved titles are ready offline anytime, anywhere!"),

        // Category 3: New Chapter Updates & Releases (10)
        NotificationPayload("New Chapter Released! 💥", "Fresh chapter update available! Tap to read the latest action-packed pages."),
        NotificationPayload("New Chapter Alert ⚡", "A brand new chapter has dropped for top trending manga series!"),
        NotificationPayload("Weekly Chapter Update 🗓️", "New releases are in! Catch up on the newest manga updates right now."),
        NotificationPayload("Latest Chapter Drops 🚨", "Huge updates just landed! Read the newest chapter releases immediately."),
        NotificationPayload("Fresh Off The Press 📰", "New manga pages available! Experience high-resolution reading today."),
        NotificationPayload("Chapter Unlocked 🔑", "The latest story arc is heating up! Read the new chapter update now."),
        NotificationPayload("New Release Digest ⚡", "Check out the newest chapter uploads across popular manga categories."),
        NotificationPayload("Hot Chapter Alert 🔥", "Fans are raving about the latest chapter! Tap to read it right away."),
        NotificationPayload("Chapter Drop Notification 📢", "Your favorite action and romance manga have new chapters available."),
        NotificationPayload("Instant New Release 🚀", "Stay ahead of spoilers! Read the newly added chapter now."),

        // Category 4: New Manga Launch & Discoveries (10)
        NotificationPayload("New Manga Launch 🌟", "Discover hot new manga series fresh on the platform! Start reading now."),
        NotificationPayload("Trending Manga Release 🔥", "A new blockbuster manga series has just launched. Be among the first to read it!"),
        NotificationPayload("Must-Read Recommendation 💡", "Looking for something new? Check out top-rated trending manga launches!"),
        NotificationPayload("New Series Debut 🎭", "Explore thrilling new fantasy, action, and sci-fi manga releases today."),
        NotificationPayload("Editor's Choice Launch 🏆", "Handpicked top recommendation: Discover our newest featured manga series."),
        NotificationPayload("Discover Hidden Gems 💎", "Uncover amazing new manga titles that are taking the community by storm!"),
        NotificationPayload("New Genre Spotlight 🎨", "Expand your taste! Explore newly added romance, dark fantasy, and action series."),
        NotificationPayload("Rising Star Series ✨", "This newly launched manga is trending #1 today. Start chapter 1 now!"),
        NotificationPayload("Weekly Manga Spotlight 🎬", "Dive into our weekly featured new release and enjoy high-speed reading."),
        NotificationPayload("Hot New Arrival 🤩", "Fresh story, stunning art, epic battles. Check out the new arrival!"),

        // Category 5: General Engagement, Tips & Community (12)
        NotificationPayload("Offline Reading Mode 📡", "No internet? No problem! Your downloaded manga is always ready to read."),
        NotificationPayload("Dark Mode Reader 🌙", "Switch to OLED Dark Mode in settings for comfortable night time reading!"),
        NotificationPayload("Reading Goal Reached 🎯", "Awesome progress! Keep up your reading habits with another chapter."),
        NotificationPayload("Custom Reader Controls ⚙️", "Did you know? You can switch between Vertical Scroll and Webtoon modes!"),
        NotificationPayload("Manga Night Session 🌌", "Grab a cup of tea and enjoy continuous reading with smooth gestures."),
        NotificationPayload("High Quality Pages 🖼️", "Enjoy crystal clear high-resolution manga pages and fast image loading."),
        NotificationPayload("Quick Search Tip 🔍", "Looking for action or romance? Use genre filters to find your next read!"),
        NotificationPayload("Daily Discovery 🗺️", "Thousands of manga chapters at your fingertips. Discover something new!"),
        NotificationPayload("Reader Customization 🎨", "Customize your background color, gesture controls, and brightness effortlessly."),
        NotificationPayload("Seamless Progress Sync 🔄", "Your progress is saved automatically every time you switch chapters!"),
        NotificationPayload("Manga Fan Alert 📣", "Never miss a moment of your favorite manga universe. Read now!"),
        NotificationPayload("Ad-Free Reading Experience ⚡", "Enjoy uninterrupted reading sessions with zero clutter.")
    )

    fun getRandomNotification(
        bookmarks: List<BookmarkEntity>,
        history: List<HistoryEntity>,
        context: Context? = null
    ): NotificationPayload {
        val username = context?.let { UserPreferences.getUsername(it) }.orEmpty()
        val nameLabel = if (username.isNotBlank()) username else ""

        val roll = Random.nextInt(100)

        // Filter out finished manga so reminders aren't sent for completed titles
        val activeHistory = if (context != null) {
            history.filterNot { UserPreferences.isMangaFinished(context, it.mangaId) }
        } else {
            history
        }

        // 1. 35% chance: Personalized Continue Reading Reminder if active history exists
        if (roll < 35 && activeHistory.isNotEmpty()) {
            val lastItem = activeHistory.random()
            val greeting = if (nameLabel.isNotBlank()) "Hey $nameLabel! " else ""
            return NotificationPayload(
                title = "Continue Reading: ${lastItem.title} 📖",
                message = "${greeting}You were on ${lastItem.chapterTitle} (Page ${lastItem.pageNumber}). Tap to jump straight back in!",
                mangaId = lastItem.mangaId,
                chapterId = lastItem.chapterId,
                chapterTitle = lastItem.chapterTitle
            )
        }

        // 2. 30% chance: Personalized Bookmark Reminder if continuing bookmarks exist
        val activeBookmarks = if (context != null) {
            bookmarks.filterNot { item ->
                UserPreferences.isMangaFinished(context, item.mangaId) ||
                item.status.equals("FINISHED", ignoreCase = true) ||
                item.status.equals("COMPLETED", ignoreCase = true)
            }
        } else {
            bookmarks
        }

        if (roll < 65 && activeBookmarks.isNotEmpty()) {
            val bookmarkedItem = activeBookmarks.random()
            val greeting = if (nameLabel.isNotBlank()) "Hey $nameLabel, " else ""
            return NotificationPayload(
                title = "Bookmark Update: ${bookmarkedItem.title} 🔖",
                message = "${greeting}new chapter updates may be available for your continuing favorite '${bookmarkedItem.title}'! Tap to read now.",
                mangaId = bookmarkedItem.mangaId
            )
        }

        // 3. Otherwise: Pick randomly from the 50+ message pool
        val basePayload = staticPool.random()
        return if (nameLabel.isNotBlank()) {
            basePayload.copy(
                message = "Hey $nameLabel, " + basePayload.message.replaceFirstChar { it.lowercase() }
            )
        } else {
            basePayload
        }
    }

    fun getRandomFallbackNotification(context: Context? = null): NotificationPayload {
        val username = context?.let { UserPreferences.getUsername(it) }.orEmpty()
        val basePayload = staticPool.random()
        return if (username.isNotBlank()) {
            basePayload.copy(
                message = "Hey $username, " + basePayload.message.replaceFirstChar { it.lowercase() }
            )
        } else {
            basePayload
        }
    }
}
