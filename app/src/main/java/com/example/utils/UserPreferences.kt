package com.example.utils

import android.content.Context

object UserPreferences {
    private const val PREFS_NAME = "mymanga_user_prefs"
    private const val KEY_USERNAME = "user_name"
    private const val KEY_AVATAR_URI = "avatar_uri"
    private const val KEY_BANNER_URI = "profile_banner_uri"
    private const val KEY_FAVORITE_GENRE = "favorite_genre"
    private const val KEY_BIO = "user_bio"
    private const val KEY_FIRST_TIME = "is_first_time"

    fun getUsername(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USERNAME, "") ?: ""
    }

    fun setUsername(context: Context, username: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cleanName = username.trim()
        prefs.edit().putString(KEY_USERNAME, cleanName).apply()
        if (cleanName.isNotBlank()) {
            val currentAvatar = prefs.getString(KEY_AVATAR_URI, null)
            if (currentAvatar.isNullOrBlank()) {
                val autoAvatar = com.example.ui.screens.profile.AnimeAvatarRepository.getAvatarForUsername(cleanName)
                prefs.edit().putString(KEY_AVATAR_URI, autoAvatar.imageUrl).apply()
            }
        }
    }

    fun getAvatarUri(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uri = prefs.getString(KEY_AVATAR_URI, null)
        if (!uri.isNullOrBlank()) return uri
        val username = getUsername(context)
        if (username.isNotBlank()) {
            val autoAvatar = com.example.ui.screens.profile.AnimeAvatarRepository.getAvatarForUsername(username)
            prefs.edit().putString(KEY_AVATAR_URI, autoAvatar.imageUrl).apply()
            return autoAvatar.imageUrl
        }
        return null
    }

    fun setAvatarUri(context: Context, uri: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AVATAR_URI, uri).apply()
    }

    fun getBannerUri(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uri = prefs.getString(KEY_BANNER_URI, null)
        val validUri = com.example.ui.screens.profile.AnimeBannerRepository.getValidBannerUri(uri)
        if (uri != validUri) {
            prefs.edit().putString(KEY_BANNER_URI, validUri).apply()
        }
        return validUri
    }

    fun setBannerUri(context: Context, uri: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BANNER_URI, uri).apply()
    }

    fun getFavoriteGenre(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FAVORITE_GENRE, "Action & Shonen") ?: "Action & Shonen"
    }

    fun setFavoriteGenre(context: Context, genre: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FAVORITE_GENRE, genre).apply()
    }

    fun getUserBio(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BIO, "Manga Enthusiast & Reader 📚") ?: "Manga Enthusiast & Reader 📚"
    }

    fun setUserBio(context: Context, bio: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BIO, bio.trim()).apply()
    }

    fun isFirstTime(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_FIRST_TIME, true)
    }

    fun setFirstTimeCompleted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FIRST_TIME, false).apply()
    }

    private const val KEY_FINISHED_MANGA_IDS = "finished_manga_ids"

    fun markMangaFinished(context: Context, mangaId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_FINISHED_MANGA_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add(mangaId.toString())
        prefs.edit().putStringSet(KEY_FINISHED_MANGA_IDS, set).apply()
    }

    fun unmarkMangaFinished(context: Context, mangaId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_FINISHED_MANGA_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (set.remove(mangaId.toString())) {
            prefs.edit().putStringSet(KEY_FINISHED_MANGA_IDS, set).apply()
        }
    }

    fun isMangaFinished(context: Context, mangaId: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_FINISHED_MANGA_IDS, emptySet()) ?: emptySet()
        return set.contains(mangaId.toString())
    }

    fun getFinishedMangaIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_FINISHED_MANGA_IDS, emptySet()) ?: emptySet()
    }

    fun setFinishedMangaIds(context: Context, ids: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_FINISHED_MANGA_IDS, ids).apply()
    }

    private const val KEY_PREFIX_READ_CHAPTERS = "read_chapters_manga_"

    fun getReadChapterIds(context: Context, mangaId: Int): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_PREFIX_READ_CHAPTERS + mangaId, emptySet()) ?: emptySet()
    }

    fun markChaptersRead(context: Context, mangaId: Int, chapterIds: Collection<String>) {
        if (chapterIds.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = KEY_PREFIX_READ_CHAPTERS + mangaId
        val currentSet = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.addAll(chapterIds)
        prefs.edit().putStringSet(key, currentSet).apply()
    }

    fun markChaptersUnread(context: Context, mangaId: Int, chapterIds: Collection<String>) {
        if (chapterIds.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = KEY_PREFIX_READ_CHAPTERS + mangaId
        val currentSet = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.removeAll(chapterIds.toSet())
        prefs.edit().putStringSet(key, currentSet).apply()
    }

    fun isChapterRead(context: Context, mangaId: Int, chapterId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_PREFIX_READ_CHAPTERS + mangaId, emptySet()) ?: emptySet()
        return set.contains(chapterId)
    }

    private const val KEY_DEFAULT_READER_MODE = "default_reader_mode"
    private const val KEY_DEFAULT_READER_BG = "default_reader_bg"
    private const val KEY_KEEP_SCREEN_AWAKE = "keep_screen_awake"
    private const val KEY_PAGE_PRELOAD = "page_preload_enabled"
    private const val KEY_TAP_TO_SCROLL = "tap_to_scroll_enabled"

    fun getDefaultReaderMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEFAULT_READER_MODE, "Webtoon (Vertical)") ?: "Webtoon (Vertical)"
    }

    fun setDefaultReaderMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DEFAULT_READER_MODE, mode).apply()
    }

    fun getDefaultReaderBg(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEFAULT_READER_BG, "OLED Pure Black") ?: "OLED Pure Black"
    }

    fun setDefaultReaderBg(context: Context, bg: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DEFAULT_READER_BG, bg).apply()
    }

    fun isKeepScreenAwake(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_KEEP_SCREEN_AWAKE, true)
    }

    fun setKeepScreenAwake(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_AWAKE, enabled).apply()
    }

    fun isPagePreloadEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PAGE_PRELOAD, true)
    }

    fun setPagePreloadEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PAGE_PRELOAD, enabled).apply()
    }

    fun isTapToScrollEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TAP_TO_SCROLL, true)
    }

    fun setTapToScrollEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_TAP_TO_SCROLL, enabled).apply()
    }

    // --- OWNER & ADMIN CONTROL PANEL PREFERENCES ---
    private const val KEY_OWNER_PASSWORD = "owner_admin_password"
    const val DEFAULT_OWNER_PASSWORD = "JoydeepHardpassword@23"

    fun getOwnerPassword(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_OWNER_PASSWORD, DEFAULT_OWNER_PASSWORD) ?: DEFAULT_OWNER_PASSWORD
    }

    fun setOwnerPassword(context: Context, newPass: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_OWNER_PASSWORD, newPass).apply()
    }

    // 1. Version Update Broadcast
    private const val KEY_UPDATE_AVAILABLE_ENABLED = "update_available_enabled"
    private const val KEY_UPDATE_VERSION_NAME = "update_version_name"
    private const val KEY_UPDATE_FEATURES = "update_features"
    private const val KEY_UPDATE_URL = "update_url"
    private const val KEY_UPDATE_DISMISSED_VERSION = "update_dismissed_version"

    fun isUpdateAvailable(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_UPDATE_AVAILABLE_ENABLED, false)
    }

    fun setUpdateAvailable(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_UPDATE_AVAILABLE_ENABLED, enabled).apply()
    }

    fun getUpdateVersionName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_UPDATE_VERSION_NAME, "v2.5.0") ?: "v2.5.0"
    }

    fun setUpdateVersionName(context: Context, version: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_UPDATE_VERSION_NAME, version).apply()
    }

    fun getUpdateFeatures(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(
            KEY_UPDATE_FEATURES,
            "• Brand new ultra-fast manga reading engine\n• Offline chapter backup & restore\n• Improved search indexing and filters\n• Stability fixes and performance optimizations"
        ) ?: ""
    }

    fun setUpdateFeatures(context: Context, features: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_UPDATE_FEATURES, features).apply()
    }

    const val DEFAULT_UPDATE_URL = "https://apk-downloader.lovable.app/app/manga/mymanga"

    fun getUpdateUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_UPDATE_URL, null)
        return if (stored.isNullOrBlank() || stored == "https://github.com") {
            DEFAULT_UPDATE_URL
        } else {
            stored
        }
    }

    fun setUpdateUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val finalUrl = url.trim().ifBlank { DEFAULT_UPDATE_URL }
        prefs.edit().putString(KEY_UPDATE_URL, finalUrl).apply()
    }

    fun getDismissedUpdateVersion(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_UPDATE_DISMISSED_VERSION, "") ?: ""
    }

    fun setDismissedUpdateVersion(context: Context, version: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_UPDATE_DISMISSED_VERSION, version).apply()
    }

    // 2. Maintenance Mode
    private const val KEY_MAINTENANCE_ENABLED = "maintenance_mode_enabled"
    private const val KEY_MAINTENANCE_MESSAGE = "maintenance_mode_message"
    private const val KEY_MAINTENANCE_ESTIMATE = "maintenance_mode_estimate"
    private const val KEY_MAINTENANCE_REMINDER_REQUESTED = "maintenance_reminder_requested"

    fun isMaintenanceModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MAINTENANCE_ENABLED, false)
    }

    fun setMaintenanceModeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MAINTENANCE_ENABLED, enabled).apply()
    }

    fun getMaintenanceMessage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(
            KEY_MAINTENANCE_MESSAGE,
            "We are currently performing scheduled maintenance and server upgrades to make your reading experience even smoother. We'll be back online shortly!"
        ) ?: ""
    }

    fun setMaintenanceMessage(context: Context, msg: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MAINTENANCE_MESSAGE, msg).apply()
    }

    fun getMaintenanceEstimate(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_MAINTENANCE_ESTIMATE, "Estimated time: 1 - 2 hours") ?: "Estimated time: 1 - 2 hours"
    }

    fun setMaintenanceEstimate(context: Context, est: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MAINTENANCE_ESTIMATE, est).apply()
    }

    fun isMaintenanceReminderRequested(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MAINTENANCE_REMINDER_REQUESTED, false)
    }

    fun setMaintenanceReminderRequested(context: Context, requested: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MAINTENANCE_REMINDER_REQUESTED, requested).apply()
    }

    // 3. 24-Hour Auto-Backup Preferences
    private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
    private const val KEY_AUTO_BACKUP_URI = "auto_backup_uri"
    private const val KEY_AUTO_BACKUP_FILENAME = "auto_backup_filename"
    private const val KEY_LAST_AUTO_BACKUP_TIME = "last_auto_backup_time"
    private const val KEY_LAST_AUTO_BACKUP_STATUS = "last_auto_backup_status"

    fun isAutoBackupEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
    }

    fun getAutoBackupUri(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AUTO_BACKUP_URI, null)
    }

    fun setAutoBackupUri(context: Context, uri: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AUTO_BACKUP_URI, uri).apply()
    }

    fun getAutoBackupFileName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AUTO_BACKUP_FILENAME, null)
    }

    fun setAutoBackupFileName(context: Context, name: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AUTO_BACKUP_FILENAME, name).apply()
    }

    fun getLastAutoBackupTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_AUTO_BACKUP_TIME, 0L)
    }

    fun setLastAutoBackupTime(context: Context, time: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_AUTO_BACKUP_TIME, time).apply()
    }

    fun getLastAutoBackupStatus(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_AUTO_BACKUP_STATUS, null)
    }

    fun setLastAutoBackupStatus(context: Context, status: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_AUTO_BACKUP_STATUS, status).apply()
    }
}
