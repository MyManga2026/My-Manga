package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaDao {

    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY addedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY addedAt DESC")
    suspend fun getBookmarksList(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE mangaId = :mangaId LIMIT 1")
    suspend fun getBookmark(mangaId: Int): BookmarkEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE mangaId = :mangaId)")
    fun isBookmarked(mangaId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)

    @Query("DELETE FROM bookmarks WHERE mangaId = :mangaId")
    suspend fun deleteBookmark(mangaId: Int)

    // History
    @Query("SELECT * FROM reading_history ORDER BY lastReadAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM reading_history ORDER BY lastReadAt DESC")
    suspend fun getHistoryList(): List<HistoryEntity>

    @Query("SELECT * FROM reading_history WHERE mangaId = :mangaId LIMIT 1")
    suspend fun getHistory(mangaId: Int): HistoryEntity?

    @Query("SELECT * FROM reading_history WHERE mangaId = :mangaId LIMIT 1")
    fun getHistoryForManga(mangaId: Int): Flow<HistoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistories(histories: List<HistoryEntity>)

    @Query("DELETE FROM reading_history WHERE mangaId = :mangaId")
    suspend fun deleteHistory(mangaId: Int)

    @Query("DELETE FROM reading_history")
    suspend fun clearAllHistory()

    // Downloads
    @Query("SELECT * FROM downloaded_chapters ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloaded_chapters ORDER BY downloadedAt DESC")
    suspend fun getDownloadsList(): List<DownloadEntity>

    @Query("SELECT * FROM downloaded_chapters WHERE mangaId = :mangaId ORDER BY downloadedAt DESC")
    suspend fun getDownloadsForManga(mangaId: Int): List<DownloadEntity>

    @Query("SELECT * FROM downloaded_chapters WHERE mangaId = :mangaId AND chapterId = :chapterId LIMIT 1")
    suspend fun getDownload(mangaId: Int, chapterId: String): DownloadEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_chapters WHERE mangaId = :mangaId AND chapterId = :chapterId)")
    fun isChapterDownloaded(mangaId: Int, chapterId: String): Flow<Boolean>

    @Query("SELECT chapterId FROM downloaded_chapters WHERE mangaId = :mangaId")
    fun getDownloadedChapterIdsForManga(mangaId: Int): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloads(downloads: List<DownloadEntity>)

    @Query("DELETE FROM downloaded_chapters WHERE downloadId = :downloadId")
    suspend fun deleteDownload(downloadId: String)
}
