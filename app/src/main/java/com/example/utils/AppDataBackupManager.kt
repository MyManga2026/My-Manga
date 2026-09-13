package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.BookmarkEntity
import com.example.data.local.DownloadEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.MangaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupExportResult(
    val success: Boolean,
    val bookmarksCount: Int = 0,
    val historyCount: Int = 0,
    val downloadsCount: Int = 0,
    val message: String = ""
)

data class BackupRestoreResult(
    val success: Boolean,
    val bookmarksRestored: Int = 0,
    val historyRestored: Int = 0,
    val downloadsRestored: Int = 0,
    val profileRestored: Boolean = false,
    val username: String = "",
    val message: String = ""
)

object AppDataBackupManager {

    private const val BACKUP_VERSION = 1
    private const val APP_IDENTIFIER = "MyManga"

    /**
     * Generates a complete JSON backup of the user's bookmarks, reading history,
     * downloaded chapter records, and user preferences (profile, avatar, bio, finished titles).
     */
    suspend fun createBackupJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = MangaDatabase.getDatabase(context)
        val dao = db.mangaDao()

        val bookmarks = try { dao.getBookmarksList() } catch (_: Exception) { emptyList() }
        val history = try { dao.getHistoryList() } catch (_: Exception) { emptyList() }
        val downloads = try { dao.getDownloadsList() } catch (_: Exception) { emptyList() }

        val root = JSONObject()
        root.put("app", APP_IDENTIFIER)
        root.put("version", BACKUP_VERSION)
        val now = System.currentTimeMillis()
        root.put("exportedAt", now)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        root.put("exportDate", dateFormat.format(Date(now)))

        // User Profile & Preferences
        val profileJson = JSONObject()
        profileJson.put("username", UserPreferences.getUsername(context))
        profileJson.put("avatarUri", UserPreferences.getAvatarUri(context) ?: "")
        profileJson.put("bio", UserPreferences.getUserBio(context))
        profileJson.put("favoriteGenre", UserPreferences.getFavoriteGenre(context))

        val finishedIds = UserPreferences.getFinishedMangaIds(context)
        val finishedJsonArray = JSONArray()
        finishedIds.forEach { finishedJsonArray.put(it) }
        profileJson.put("finishedMangaIds", finishedJsonArray)

        root.put("userProfile", profileJson)

        // Bookmarks Array
        val bookmarksArray = JSONArray()
        for (b in bookmarks) {
            val item = JSONObject()
            item.put("mangaId", b.mangaId)
            item.put("title", b.title)
            item.put("coverImage", b.coverImage)
            item.put("genres", b.genres)
            item.put("status", b.status)
            item.put("score", b.score)
            item.put("addedAt", b.addedAt)
            bookmarksArray.put(item)
        }
        root.put("bookmarks", bookmarksArray)

        // Reading History Array
        val historyArray = JSONArray()
        for (h in history) {
            val item = JSONObject()
            item.put("mangaId", h.mangaId)
            item.put("title", h.title)
            item.put("coverImage", h.coverImage)
            item.put("chapterId", h.chapterId)
            item.put("chapterTitle", h.chapterTitle)
            item.put("pageNumber", h.pageNumber)
            item.put("totalPages", h.totalPages)
            item.put("lastReadAt", h.lastReadAt)
            historyArray.put(item)
        }
        root.put("history", historyArray)

        // Downloads Metadata Array
        val downloadsArray = JSONArray()
        for (d in downloads) {
            val item = JSONObject()
            item.put("downloadId", d.downloadId)
            item.put("mangaId", d.mangaId)
            item.put("mangaTitle", d.mangaTitle)
            item.put("coverImage", d.coverImage ?: "")
            item.put("chapterId", d.chapterId)
            item.put("chapterTitle", d.chapterTitle)
            item.put("chapterNumber", d.chapterNumber)
            item.put("localPagesJson", d.localPagesJson)
            item.put("totalPages", d.totalPages)
            item.put("downloadedAt", d.downloadedAt)
            downloadsArray.put(item)
        }
        root.put("downloads", downloadsArray)

        return@withContext root.toString(2)
    }

    /**
     * Exports backup data directly to a file selected by the user via Storage Access Framework (SAF).
     */
    suspend fun exportToFile(context: Context, uri: Uri): BackupExportResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = createBackupJson(context)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            } ?: return@withContext BackupExportResult(
                success = false,
                message = "Unable to open file for writing"
            )

            val db = MangaDatabase.getDatabase(context)
            val dao = db.mangaDao()
            val bookmarksCount = try { dao.getBookmarksList().size } catch (_: Exception) { 0 }
            val historyCount = try { dao.getHistoryList().size } catch (_: Exception) { 0 }
            val downloadsCount = try { dao.getDownloadsList().size } catch (_: Exception) { 0 }

            BackupExportResult(
                success = true,
                bookmarksCount = bookmarksCount,
                historyCount = historyCount,
                downloadsCount = downloadsCount,
                message = "Export completed successfully"
            )
        } catch (e: Exception) {
            BackupExportResult(
                success = false,
                message = e.localizedMessage ?: "Failed to export backup file"
            )
        }
    }

    /**
     * Reads a backup JSON file from the user's selected Uri and restores all bookmarks,
     * reading history, downloads, and user profile preferences into the app.
     */
    suspend fun importFromFile(context: Context, uri: Uri): BackupRestoreResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext BackupRestoreResult(
                success = false,
                message = "Failed to open selected backup file"
            )

            restoreFromJson(context, jsonString)
        } catch (e: Exception) {
            BackupRestoreResult(
                success = false,
                message = "Import error: ${e.localizedMessage ?: "Invalid file"}"
            )
        }
    }

    /**
     * Parses and restores data from raw JSON content.
     */
    suspend fun restoreFromJson(context: Context, jsonString: String): BackupRestoreResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            val db = MangaDatabase.getDatabase(context)
            val dao = db.mangaDao()

            var restoredBookmarksCount = 0
            var restoredHistoryCount = 0
            var restoredDownloadsCount = 0
            var profileRestored = false
            var restoredUsername = ""

            // 1. Restore User Profile & Preferences
            if (root.has("userProfile")) {
                val profileObj = root.optJSONObject("userProfile")
                if (profileObj != null) {
                    val username = profileObj.optString("username", "").trim()
                    if (username.isNotBlank()) {
                        UserPreferences.setUsername(context, username)
                        restoredUsername = username
                    }

                    val avatar = profileObj.optString("avatarUri", "")
                    if (avatar.isNotBlank()) {
                        UserPreferences.setAvatarUri(context, avatar)
                    }

                    val bio = profileObj.optString("bio", "")
                    if (bio.isNotBlank()) {
                        UserPreferences.setUserBio(context, bio)
                    }

                    val favGenre = profileObj.optString("favoriteGenre", "")
                    if (favGenre.isNotBlank()) {
                        UserPreferences.setFavoriteGenre(context, favGenre)
                    }

                    val finishedArray = profileObj.optJSONArray("finishedMangaIds")
                    if (finishedArray != null) {
                        val set = mutableSetOf<String>()
                        for (i in 0 until finishedArray.length()) {
                            val id = finishedArray.optString(i)
                            if (id.isNotBlank()) set.add(id)
                        }
                        UserPreferences.setFinishedMangaIds(context, set)
                    }

                    UserPreferences.setFirstTimeCompleted(context)
                    profileRestored = true
                }
            }

            // 2. Restore Bookmarks
            if (root.has("bookmarks")) {
                val bookmarksArray = root.optJSONArray("bookmarks")
                if (bookmarksArray != null && bookmarksArray.length() > 0) {
                    val list = mutableListOf<BookmarkEntity>()
                    for (i in 0 until bookmarksArray.length()) {
                        val obj = bookmarksArray.optJSONObject(i) ?: continue
                        val mangaId = obj.optInt("mangaId", -1)
                        if (mangaId != -1) {
                            list.add(
                                BookmarkEntity(
                                    mangaId = mangaId,
                                    title = obj.optString("title", "Untitled Manga"),
                                    coverImage = obj.optString("coverImage", ""),
                                    genres = obj.optString("genres", ""),
                                    status = obj.optString("status", "RELEASING"),
                                    score = obj.optInt("score", 0),
                                    addedAt = obj.optLong("addedAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                    if (list.isNotEmpty()) {
                        dao.insertBookmarks(list)
                        restoredBookmarksCount = list.size
                    }
                }
            }

            // 3. Restore History
            if (root.has("history")) {
                val historyArray = root.optJSONArray("history")
                if (historyArray != null && historyArray.length() > 0) {
                    val list = mutableListOf<HistoryEntity>()
                    for (i in 0 until historyArray.length()) {
                        val obj = historyArray.optJSONObject(i) ?: continue
                        val mangaId = obj.optInt("mangaId", -1)
                        val chapterId = obj.optString("chapterId", "")
                        if (mangaId != -1 && chapterId.isNotBlank()) {
                            list.add(
                                HistoryEntity(
                                    mangaId = mangaId,
                                    title = obj.optString("title", "Untitled Manga"),
                                    coverImage = obj.optString("coverImage", ""),
                                    chapterId = chapterId,
                                    chapterTitle = obj.optString("chapterTitle", "Chapter 1"),
                                    pageNumber = obj.optInt("pageNumber", 1),
                                    totalPages = obj.optInt("totalPages", 1),
                                    lastReadAt = obj.optLong("lastReadAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                    if (list.isNotEmpty()) {
                        dao.insertHistories(list)
                        restoredHistoryCount = list.size
                    }
                }
            }

            // 4. Restore Downloads Metadata
            if (root.has("downloads")) {
                val downloadsArray = root.optJSONArray("downloads")
                if (downloadsArray != null && downloadsArray.length() > 0) {
                    val list = mutableListOf<DownloadEntity>()
                    for (i in 0 until downloadsArray.length()) {
                        val obj = downloadsArray.optJSONObject(i) ?: continue
                        val downloadId = obj.optString("downloadId", "")
                        val mangaId = obj.optInt("mangaId", -1)
                        val chapterId = obj.optString("chapterId", "")
                        if (downloadId.isNotBlank() && mangaId != -1 && chapterId.isNotBlank()) {
                            list.add(
                                DownloadEntity(
                                    downloadId = downloadId,
                                    mangaId = mangaId,
                                    mangaTitle = obj.optString("mangaTitle", "Manga"),
                                    coverImage = obj.optString("coverImage", "").takeIf { it.isNotBlank() },
                                    chapterId = chapterId,
                                    chapterTitle = obj.optString("chapterTitle", "Chapter"),
                                    chapterNumber = obj.optString("chapterNumber", "1"),
                                    localPagesJson = obj.optString("localPagesJson", "[]"),
                                    totalPages = obj.optInt("totalPages", 1),
                                    downloadedAt = obj.optLong("downloadedAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                    if (list.isNotEmpty()) {
                        dao.insertDownloads(list)
                        restoredDownloadsCount = list.size
                    }
                }
            }

            BackupRestoreResult(
                success = true,
                bookmarksRestored = restoredBookmarksCount,
                historyRestored = restoredHistoryCount,
                downloadsRestored = restoredDownloadsCount,
                profileRestored = profileRestored,
                username = restoredUsername,
                message = "All data restored successfully!"
            )
        } catch (e: Exception) {
            BackupRestoreResult(
                success = false,
                message = "Failed to parse backup data: ${e.localizedMessage ?: "Invalid JSON format"}"
            )
        }
    }

    /**
     * Generates a recommended default filename for exported backup files.
     */
    fun getDefaultBackupFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "MyManga_Backup_$timestamp.json"
    }

    /**
     * Persists URI read/write permissions for background auto-backup tasks.
     */
    fun persistUriPermission(context: Context, uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: Exception) {}
    }

    /**
     * Extracts the user-friendly file name from a content Uri.
     */
    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var result = "MyManga_Backup.json"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        result = name
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    /**
     * Executes automatic background backup to the user's previously chosen target file Uri.
     */
    suspend fun performAutoBackup(context: Context): BackupExportResult = withContext(Dispatchers.IO) {
        val uriString = UserPreferences.getAutoBackupUri(context)
        if (uriString.isNullOrBlank()) {
            val result = BackupExportResult(
                success = false,
                message = "No target backup file location selected"
            )
            UserPreferences.setLastAutoBackupStatus(context, "Failed: No file selected")
            return@withContext result
        }

        val uri = try {
            Uri.parse(uriString)
        } catch (e: Exception) {
            val result = BackupExportResult(
                success = false,
                message = "Invalid backup file URI: ${e.message}"
            )
            UserPreferences.setLastAutoBackupStatus(context, "Failed: Invalid file path")
            return@withContext result
        }

        val exportResult = exportToFile(context, uri)
        if (exportResult.success) {
            UserPreferences.setLastAutoBackupTime(context, System.currentTimeMillis())
            val statusMsg = "Success (${exportResult.bookmarksCount} bookmarks, ${exportResult.historyCount} history items)"
            UserPreferences.setLastAutoBackupStatus(context, statusMsg)
        } else {
            UserPreferences.setLastAutoBackupStatus(context, "Failed: ${exportResult.message}")
        }
        exportResult
    }
}
