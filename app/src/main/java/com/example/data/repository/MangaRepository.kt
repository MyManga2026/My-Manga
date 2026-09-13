package com.example.data.repository

import android.content.Context
import com.example.data.local.BookmarkEntity
import com.example.data.local.DownloadEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.MangaDao
import com.example.data.model.ChapterItem
import com.example.data.model.ChapterPage
import com.example.data.model.MangaItem
import com.example.data.model.MangaProvider
import com.example.data.model.SearchFilter
import com.example.data.network.AnilistClient
import com.example.data.network.MangaDexClient
import com.example.data.network.MangaFireClient
import com.example.data.network.MangapillClient
import com.example.data.network.WeebCentralClient
import com.example.utils.NetworkUtils
import com.example.utils.NotificationHelper
import com.example.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class MangaRepository(
    private val appContext: Context? = null,
    private val anilistClient: AnilistClient,
    private val mangapillClient: MangapillClient,
    private val mangadexClient: MangaDexClient,
    private val weebCentralClient: WeebCentralClient,
    private val mangaFireClient: MangaFireClient,
    private val mangaDao: MangaDao
) {
    // In-memory cache to ensure manga details and items are always instantly accessible
    private val globalMangaCache = ConcurrentHashMap<Int, MangaItem>()

    fun cacheMangaItem(item: MangaItem) {
        globalMangaCache[item.id] = item
    }

    fun cacheMangaItems(items: Collection<MangaItem>) {
        items.forEach { globalMangaCache[it.id] = it }
    }

    // AniList API with MangaDex & CuratedCatalog fallback
    suspend fun getTrendingManga(page: Int = 1): List<MangaItem> {
        try {
            val list = anilistClient.getTrendingManga(page)
            if (list.isNotEmpty()) {
                cacheMangaItems(list)
                return list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val dexList = mangadexClient.getTrendingManga(page = page)
            if (dexList.isNotEmpty()) {
                cacheMangaItems(dexList)
                return dexList
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val curated = CuratedMangaCatalog.getTrending()
        cacheMangaItems(curated)
        return curated
    }

    suspend fun getPopularManga(page: Int = 1): List<MangaItem> {
        try {
            val list = anilistClient.getPopularManga(page)
            if (list.isNotEmpty()) {
                cacheMangaItems(list)
                return list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val dexList = mangadexClient.getPopularManga(page = page)
            if (dexList.isNotEmpty()) {
                cacheMangaItems(dexList)
                return dexList
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val curated = CuratedMangaCatalog.getPopular()
        cacheMangaItems(curated)
        return curated
    }

    suspend fun searchManga(
        query: String? = null,
        genre: String? = null,
        filter: SearchFilter? = null,
        page: Int = 1
    ): List<MangaItem> {
        try {
            val list = anilistClient.searchManga(searchQuery = query, genre = genre, filter = filter, page = page)
            if (list.isNotEmpty()) {
                cacheMangaItems(list)
                return list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val dexList = mangadexClient.searchManga(searchQuery = query, genre = genre, page = page)
            if (dexList.isNotEmpty()) {
                val filteredDex = if (filter != null) dexList.filter { filter.matches(it) } else dexList
                if (filteredDex.isNotEmpty()) {
                    cacheMangaItems(filteredDex)
                    return filteredDex
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val curated = CuratedMangaCatalog.search(query = query, genre = genre, filter = filter)
        if (curated.isNotEmpty()) {
            cacheMangaItems(curated)
            return curated
        }

        // If still nothing, match from local DB
        try {
            val localResults = mutableListOf<MangaItem>()
            val cleanQ = query?.lowercase()?.trim() ?: ""
            val bookmarks = mangaDao.getBookmarksList()
            for (bm in bookmarks) {
                if (cleanQ.isBlank() || bm.title.lowercase().contains(cleanQ)) {
                    val localItem = MangaItem(
                        id = bm.mangaId,
                        titleRomaji = bm.title,
                        titleEnglish = bm.title,
                        coverImage = bm.coverImage,
                        description = "Bookmarked manga",
                        status = "Saved",
                        genres = listOf("Bookmarked")
                    )
                    if (filter == null || filter.matches(localItem)) {
                        localResults.add(localItem)
                        cacheMangaItem(localItem)
                    }
                }
            }
            if (localResults.isNotEmpty()) return localResults
        } catch (_: Exception) {}

        return emptyList()
    }

    suspend fun getMangaRecommendations(id: Int, primaryGenre: String? = null): List<MangaItem> {
        val recs = try {
            val list = anilistClient.getMangaRecommendations(id)
            if (list.isNotEmpty()) {
                list
            } else if (!primaryGenre.isNullOrBlank()) {
                val genreRecs = searchManga(genre = primaryGenre).filter { it.id != id }
                if (genreRecs.isNotEmpty()) genreRecs.take(12) else CuratedMangaCatalog.getByGenre(primaryGenre).filter { it.id != id }.take(12)
            } else {
                getPopularManga(page = 1).filter { it.id != id }.take(12)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (!primaryGenre.isNullOrBlank()) {
                CuratedMangaCatalog.getByGenre(primaryGenre).filter { it.id != id }.take(12)
            } else {
                CuratedMangaCatalog.getPopular().filter { it.id != id }.take(12)
            }
        }
        cacheMangaItems(recs)
        return recs
    }

    suspend fun getMangaDetails(id: Int): MangaItem? {
        // 1. Try AniList online
        try {
            val onlineDetails = anilistClient.getMangaDetails(id)
            if (onlineDetails != null) {
                cacheMangaItem(onlineDetails)
                return onlineDetails
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Check in-memory global cache
        val inMemory = globalMangaCache[id]
        if (inMemory != null) return inMemory

        // 3. Check Curated catalog
        val curated = CuratedMangaCatalog.getById(id)
        if (curated != null) {
            cacheMangaItem(curated)
            return curated
        }

        // 4. Fallback for offline / local mode: check downloads, bookmarks, or history
        try {
            val downloads = mangaDao.getDownloadsForManga(id)
            if (downloads.isNotEmpty()) {
                val firstDl = downloads.first()
                val item = MangaItem(
                    id = firstDl.mangaId,
                    titleRomaji = firstDl.mangaTitle,
                    titleEnglish = firstDl.mangaTitle,
                    coverImage = firstDl.coverImage ?: "",
                    description = "Downloaded manga (Offline mode)",
                    status = "Downloaded",
                    chapters = downloads.size
                )
                cacheMangaItem(item)
                return item
            }

            val bookmark = mangaDao.getBookmark(id)
            if (bookmark != null) {
                val item = MangaItem(
                    id = bookmark.mangaId,
                    titleRomaji = bookmark.title,
                    titleEnglish = bookmark.title,
                    coverImage = bookmark.coverImage,
                    description = "Bookmarked manga (Saved in library)",
                    status = "Saved",
                    chapters = null
                )
                cacheMangaItem(item)
                return item
            }

            val history = mangaDao.getHistory(id)
            if (history != null) {
                val item = MangaItem(
                    id = history.mangaId,
                    titleRomaji = history.title,
                    titleEnglish = history.title,
                    coverImage = history.coverImage,
                    description = "Recently read manga",
                    status = "Reading",
                    chapters = null
                )
                cacheMangaItem(item)
                return item
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun parseChapterNumberFloat(raw: String): Float {
        if (raw.isBlank()) return 0f
        val match = Regex("(\\d+(\\.\\d+)?)").find(raw)
        return match?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
    }

    // Chapters Source: Mangapill (1st/Default), WeebCentral (2nd), MangaFire (3rd), MangaDex (4th)
    suspend fun getChapters(
        mangaTitle: String,
        totalChapters: Int?,
        titleRomaji: String? = null,
        provider: MangaProvider = MangaProvider.MANGAPILL,
        mangaId: Int? = null
    ): List<ChapterItem> {
        var onlineChapters: List<ChapterItem> = emptyList()
        try {
            onlineChapters = when (provider) {
                MangaProvider.MANGAPILL -> {
                    val pill = mangapillClient.getChaptersForManga(mangaTitle, totalChapters, titleRomaji)
                    if (pill.any { !it.mangapillUrl.isNullOrBlank() }) pill
                    else {
                        val dexChapters = try {
                            val dexId = mangadexClient.searchMangaId(mangaTitle, titleRomaji)
                            if (!dexId.isNullOrBlank()) mangadexClient.getChapters(dexId) else emptyList()
                        } catch (_: Exception) { emptyList() }
                        if (dexChapters.isNotEmpty()) dexChapters else pill
                    }
                }
                MangaProvider.WEEBCENTRAL -> {
                    weebCentralClient.getChapters(mangaTitle, totalChapters, titleRomaji)
                }
                MangaProvider.MANGAFIRE -> {
                    mangaFireClient.getChapters(mangaTitle, totalChapters, titleRomaji)
                }
                MangaProvider.MANGADEX -> {
                    try {
                        val dexId = mangadexClient.searchMangaId(mangaTitle, titleRomaji)
                        if (!dexId.isNullOrBlank()) {
                            val dexChapters = mangadexClient.getChapters(dexId)
                            if (dexChapters.isNotEmpty()) dexChapters else emptyList()
                        } else emptyList()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val isDummyList = onlineChapters.isNotEmpty() && onlineChapters.all { it.mangapillUrl == null && it.id.startsWith("ch-") }

        if (mangaId != null) {
            try {
                val downloaded = mangaDao.getDownloadsForManga(mangaId)
                if (downloaded.isNotEmpty()) {
                    val downloadedChapterItems = downloaded.map { dl ->
                        ChapterItem(
                            id = dl.chapterId,
                            title = dl.chapterTitle,
                            chapterNumber = dl.chapterNumber
                        )
                    }.sortedByDescending { parseChapterNumberFloat(it.chapterNumber) }

                    // If offline, dummy list, or online empty, return downloaded chapters
                    if (onlineChapters.isEmpty() || isDummyList) {
                        return downloadedChapterItems
                    }

                    // If online list exists, merge downloaded chapter IDs
                    return onlineChapters.map { onlineCh ->
                        val matchingDl = downloaded.find { dl ->
                            dl.chapterId == onlineCh.id ||
                            dl.chapterNumber == onlineCh.chapterNumber ||
                            dl.chapterTitle.equals(onlineCh.title, ignoreCase = true)
                        }
                        if (matchingDl != null) {
                            onlineCh.copy(id = matchingDl.chapterId)
                        } else {
                            onlineCh
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (onlineChapters.isNotEmpty() && !isDummyList) {
            return onlineChapters
        }

        // Final fallback to downloaded chapters if any
        if (mangaId != null) {
            try {
                val downloaded = mangaDao.getDownloadsForManga(mangaId)
                if (downloaded.isNotEmpty()) {
                    return downloaded.map { dl ->
                        ChapterItem(
                            id = dl.chapterId,
                            title = dl.chapterTitle,
                            chapterNumber = dl.chapterNumber
                        )
                    }.sortedByDescending { parseChapterNumberFloat(it.chapterNumber) }
                }
            } catch (_: Exception) {}
        }

        return onlineChapters
    }

    // Chapter Pages Source according to active MangaProvider
    suspend fun getChapterPages(
        chapter: ChapterItem,
        mangaTitle: String,
        provider: MangaProvider = MangaProvider.MANGAPILL,
        mangaId: Int? = null
    ): List<ChapterPage> {
        // Check offline downloads first if mangaId is available
        if (mangaId != null) {
            try {
                var downloaded = mangaDao.getDownload(mangaId, chapter.id)
                if (downloaded == null) {
                    val allDownloads = mangaDao.getDownloadsForManga(mangaId)
                    downloaded = allDownloads.find { dl ->
                        dl.chapterId == chapter.id ||
                        dl.chapterNumber == chapter.chapterNumber ||
                        dl.chapterTitle.equals(chapter.title, ignoreCase = true) ||
                        (dl.chapterNumber.isNotBlank() && chapter.id.contains(dl.chapterNumber))
                    }
                }

                if (downloaded != null && downloaded.localPagesJson.isNotBlank()) {
                    val jsonArr = JSONArray(downloaded.localPagesJson)
                    val localPages = mutableListOf<ChapterPage>()
                    for (i in 0 until jsonArr.length()) {
                        val path = jsonArr.getString(i)
                        val file = File(path)
                        if (file.exists() && file.length() > 0L) {
                            localPages.add(ChapterPage(i + 1, "file://$path"))
                        }
                    }
                    if (localPages.isNotEmpty()) {
                        return localPages
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return when (provider) {
            MangaProvider.MANGAPILL -> {
                val pillPages = mangapillClient.getChapterPages(chapter, mangaTitle)
                if (pillPages.isNotEmpty() && pillPages.none { it.imageUrl.contains("picsum.photos") }) {
                    pillPages
                } else {
                    getChapterPages(chapter, mangaTitle, MangaProvider.MANGADEX, mangaId)
                }
            }
            MangaProvider.WEEBCENTRAL -> {
                weebCentralClient.getChapterPages(chapter, mangaTitle)
            }
            MangaProvider.MANGAFIRE -> {
                mangaFireClient.getChapterPages(chapter, mangaTitle)
            }
            MangaProvider.MANGADEX -> {
                if (!chapter.mangadexChapterId.isNullOrBlank()) {
                    val dexPages = mangadexClient.getChapterPages(chapter.mangadexChapterId)
                    if (dexPages.isNotEmpty()) return dexPages
                }
                try {
                    val dexId = mangadexClient.searchMangaId(mangaTitle)
                    if (!dexId.isNullOrBlank()) {
                        val dexChapters = mangadexClient.getChapters(dexId)
                        val match = dexChapters.find {
                            it.chapterNumber == chapter.chapterNumber ||
                            it.chapterNumber == chapter.id ||
                            chapter.id.contains("ch-${it.chapterNumber}") ||
                            chapter.title.contains("Chapter ${it.chapterNumber}", ignoreCase = true)
                        }
                        if (match?.mangadexChapterId != null) {
                            val dexPages = mangadexClient.getChapterPages(match.mangadexChapterId)
                            if (dexPages.isNotEmpty()) return dexPages
                        } else if (dexChapters.isNotEmpty()) {
                            val firstDexId = dexChapters.first().mangadexChapterId
                            if (firstDexId != null) {
                                val dexPages = mangadexClient.getChapterPages(firstDexId)
                                if (dexPages.isNotEmpty()) return dexPages
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                mangapillClient.getChapterPages(chapter, mangaTitle)
            }
        }
    }

    // Local Database - Bookmarks
    val allBookmarks: Flow<List<BookmarkEntity>> = mangaDao.getAllBookmarks()

    fun isBookmarked(mangaId: Int): Flow<Boolean> = mangaDao.isBookmarked(mangaId)

    suspend fun toggleBookmark(manga: MangaItem, currentlyBookmarked: Boolean, context: Context? = null) {
        if (currentlyBookmarked) {
            mangaDao.deleteBookmark(manga.id)
        } else {
            mangaDao.insertBookmark(
                BookmarkEntity(
                    mangaId = manga.id,
                    title = manga.displayTitle,
                    coverImage = manga.coverImage,
                    genres = manga.genres.joinToString(", "),
                    status = manga.status,
                    score = manga.score ?: 0
                )
            )
            val ctx = context ?: appContext
            ctx?.let {
                val isFinished = UserPreferences.isMangaFinished(it, manga.id) ||
                        manga.status.equals("FINISHED", ignoreCase = true) ||
                        manga.status.equals("COMPLETED", ignoreCase = true)

                if (isFinished) {
                    UserPreferences.markMangaFinished(it, manga.id)
                }

                val notifMessage = if (isFinished) {
                    "Saved '${manga.displayTitle}' to your library!"
                } else {
                    "Saved '${manga.displayTitle}' to your library! You will receive notification updates for new chapter releases."
                }

                NotificationHelper.sendNotification(
                    context = it,
                    title = "Bookmark Saved 🔖",
                    message = notifMessage,
                    mangaId = manga.id
                )
            }
        }
    }

    // Local Database - History
    val allHistory: Flow<List<HistoryEntity>> = mangaDao.getAllHistory()

    suspend fun getHistory(mangaId: Int): HistoryEntity? = mangaDao.getHistory(mangaId)

    fun getHistoryForManga(mangaId: Int): Flow<HistoryEntity?> = mangaDao.getHistoryForManga(mangaId)

    suspend fun saveReadingProgress(
        manga: MangaItem,
        chapter: ChapterItem,
        pageNumber: Int,
        totalPages: Int,
        isFinished: Boolean = false,
        context: Context? = null
    ) {
        mangaDao.insertHistory(
            HistoryEntity(
                mangaId = manga.id,
                title = manga.displayTitle,
                coverImage = manga.coverImage,
                chapterId = chapter.id,
                chapterTitle = chapter.title,
                pageNumber = pageNumber,
                totalPages = totalPages,
                lastReadAt = System.currentTimeMillis()
            )
        )

        val targetContext = context ?: appContext
        if (targetContext != null) {
            if (isFinished) {
                UserPreferences.markMangaFinished(targetContext, manga.id)
            }
            com.example.utils.ReadingAnalyticsManager.recordChapterRead(targetContext, manga.genres)
        }

        if (!isFinished) {
            NotificationHelper.recordReadingSession(
                mangaTitle = manga.displayTitle,
                mangaId = manga.id,
                chapterId = chapter.id,
                chapterTitle = chapter.title
            )
        }
    }

    suspend fun deleteHistory(mangaId: Int) {
        mangaDao.deleteHistory(mangaId)
    }

    suspend fun clearHistory() {
        mangaDao.clearAllHistory()
    }

    // Local Database - Downloads
    val allDownloads: Flow<List<DownloadEntity>> = mangaDao.getAllDownloads()

    fun getDownloadedChapterIdsForManga(mangaId: Int): Flow<List<String>> =
        mangaDao.getDownloadedChapterIdsForManga(mangaId)

    suspend fun isChapterDownloaded(mangaId: Int, chapterId: String): Boolean {
        return mangaDao.getDownload(mangaId, chapterId) != null
    }

    suspend fun downloadChapter(
        context: Context,
        manga: MangaItem,
        chapter: ChapterItem,
        provider: MangaProvider = MangaProvider.MANGAPILL,
        onProgress: (downloaded: Int, total: Int) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch online pages
            val pages = getChapterPages(chapter, manga.displayTitle, provider, manga.id)
            if (pages.isEmpty()) return@withContext false

            // 2. Prepare output directory
            val safeChapterId = chapter.id.replace("/", "_").replace(":", "_")
            val dir = File(context.filesDir, "downloads/${manga.id}/$safeChapterId")
            if (!dir.exists()) dir.mkdirs()

            val okHttpClient = NetworkUtils.sharedHttpClient

            val localPaths = mutableListOf<String>()
            val total = pages.size

            for ((index, page) in pages.withIndex()) {
                val pageNum = index + 1
                val file = File(dir, "page_$pageNum.jpg")
                if (!file.exists() || file.length() == 0L) {
                    val request = Request.Builder()
                        .url(page.imageUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7)")
                        .header("Referer", provider.domainUrl)
                        .build()

                    try {
                        okHttpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val bytes = response.body?.bytes()
                                if (bytes != null) {
                                    file.writeBytes(bytes)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (file.exists() && file.length() > 0L) {
                    localPaths.add(file.absolutePath)
                }
                onProgress(pageNum, total)
            }

            if (localPaths.isNotEmpty()) {
                val jsonArr = JSONArray(localPaths).toString()
                val downloadEntity = DownloadEntity(
                    downloadId = "${manga.id}_${chapter.id}",
                    mangaId = manga.id,
                    mangaTitle = manga.displayTitle,
                    coverImage = manga.coverImage,
                    chapterId = chapter.id,
                    chapterTitle = chapter.title,
                    chapterNumber = chapter.chapterNumber,
                    localPagesJson = jsonArr,
                    totalPages = localPaths.size
                )
                mangaDao.insertDownload(downloadEntity)

                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    // Local Persistence Shortcuts
    suspend fun getBookmarksList(): List<BookmarkEntity> = mangaDao.getBookmarksList()
    suspend fun getHistoryList(): List<HistoryEntity> = mangaDao.getHistoryList()

    suspend fun deleteDownload(context: Context, downloadId: String, mangaId: Int, chapterId: String) = withContext(Dispatchers.IO) {
        mangaDao.deleteDownload(downloadId)
        try {
            val safeChapterId = chapterId.replace("/", "_").replace(":", "_")
            val dir = File(context.filesDir, "downloads/$mangaId/$safeChapterId")
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
