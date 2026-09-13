package com.example.data.network

import com.example.data.model.ChapterItem
import com.example.data.model.ChapterPage
import com.example.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.regex.Pattern

class WeebCentralClient(
    private val mangapillClient: MangapillClient,
    private val mangadexClient: MangaDexClient
) {
    private val client: OkHttpClient = NetworkUtils.sharedHttpClient

    private val userAgent = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"

    suspend fun getChapters(mangaTitle: String, totalChapters: Int?, titleRomaji: String? = null): List<ChapterItem> = withContext(Dispatchers.IO) {
        val chapters = mutableListOf<ChapterItem>()
        try {
            val encodedTitle = URLEncoder.encode(mangaTitle, "UTF-8")
            val url = "https://weebcentral.com/search/data?text=$encodedTitle"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Referer", "https://weebcentral.com/")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    val pattern = Pattern.compile("href=\"(https://weebcentral.com/chapters/[^\"]+)\"[^>]*>(.*?)</a>", Pattern.DOTALL)
                    val matcher = pattern.matcher(html)
                    var count = 1
                    while (matcher.find()) {
                        val chUrl = matcher.group(1) ?: continue
                        val lowerUrl = chUrl.lowercase()
                        if (lowerUrl.contains("novel") || lowerUrl.contains("light-novel")) continue

                        val titleText = matcher.group(2)?.replace(Regex("<[^>]*>"), "")?.trim() ?: "Chapter $count"
                        chapters.add(
                            ChapterItem(
                                id = "wc-$count",
                                title = titleText,
                                chapterNumber = "$count",
                                releaseDate = null,
                                mangapillUrl = chUrl
                            )
                        )
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (chapters.isNotEmpty()) return@withContext chapters

        // Fallback to Mangapill or MangaDex
        val pill = mangapillClient.getChaptersForManga(mangaTitle, totalChapters, titleRomaji)
        if (pill.isNotEmpty()) return@withContext pill

        val dexId = mangadexClient.searchMangaId(mangaTitle, titleRomaji)
        if (!dexId.isNullOrBlank()) {
            return@withContext mangadexClient.getChapters(dexId)
        }

        emptyList()
    }

    suspend fun getChapterPages(chapter: ChapterItem, mangaTitle: String): List<ChapterPage> = withContext(Dispatchers.IO) {
        if (chapter.mangapillUrl?.contains("weebcentral.com") == true) {
            val pages = mutableListOf<ChapterPage>()
            try {
                val request = Request.Builder()
                    .url(chapter.mangapillUrl)
                    .header("User-Agent", userAgent)
                    .header("Referer", "https://weebcentral.com/")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val html = response.body?.string() ?: ""
                        val pattern = Pattern.compile("<img[^>]+src=\"([^\"]+)\"")
                        val matcher = pattern.matcher(html)
                        var pageNum = 1
                        while (matcher.find()) {
                            val imgUrl = matcher.group(1)
                            if (!imgUrl.isNullOrBlank() && (imgUrl.contains("weebcentral") || imgUrl.endsWith(".png") || imgUrl.endsWith(".jpg"))) {
                                pages.add(ChapterPage(pageNum, imgUrl))
                                pageNum++
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (pages.isNotEmpty()) return@withContext pages
        }

        // Fallback to Mangapill / MangaDex
        val pillPages = mangapillClient.getChapterPages(chapter, mangaTitle)
        if (pillPages.isNotEmpty()) return@withContext pillPages

        val dexId = mangadexClient.searchMangaId(mangaTitle)
        if (!dexId.isNullOrBlank()) {
            val dexChapters = mangadexClient.getChapters(dexId)
            val match = dexChapters.find { it.chapterNumber == chapter.chapterNumber } ?: dexChapters.firstOrNull()
            if (match?.mangadexChapterId != null) {
                return@withContext mangadexClient.getChapterPages(match.mangadexChapterId)
            }
        }

        emptyList()
    }
}
