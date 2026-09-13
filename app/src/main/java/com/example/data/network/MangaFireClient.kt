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

class MangaFireClient(
    private val mangapillClient: MangapillClient,
    private val mangadexClient: MangaDexClient
) {
    private val client: OkHttpClient = NetworkUtils.sharedHttpClient

    private val userAgent = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"

    suspend fun getChapters(mangaTitle: String, totalChapters: Int?, titleRomaji: String? = null): List<ChapterItem> = withContext(Dispatchers.IO) {
        val chapters = mutableListOf<ChapterItem>()
        try {
            val encodedTitle = URLEncoder.encode(mangaTitle, "UTF-8")
            val url = "https://mangafire.to/filter?keyword=$encodedTitle"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Referer", "https://mangafire.to/")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    val pattern = Pattern.compile("href=\"(/read/[^\"]+)\"[^>]*>(.*?)</a>", Pattern.DOTALL)
                    val matcher = pattern.matcher(html)
                    var count = 1
                    while (matcher.find()) {
                        val path = matcher.group(1) ?: continue
                        val lowerPath = path.lowercase()
                        if (lowerPath.contains("novel") || lowerPath.contains("light-novel")) continue

                        val titleText = matcher.group(2)?.replace(Regex("<[^>]*>"), "")?.trim() ?: "Chapter $count"
                        chapters.add(
                            ChapterItem(
                                id = "mf-$count",
                                title = titleText,
                                chapterNumber = "$count",
                                releaseDate = null,
                                mangapillUrl = "https://mangafire.to$path"
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

        // Fallback to MangaDex / Mangapill
        val dexId = mangadexClient.searchMangaId(mangaTitle, titleRomaji)
        if (!dexId.isNullOrBlank()) {
            val dexChapters = mangadexClient.getChapters(dexId)
            if (dexChapters.isNotEmpty()) return@withContext dexChapters
        }

        mangapillClient.getChaptersForManga(mangaTitle, totalChapters, titleRomaji)
    }

    suspend fun getChapterPages(chapter: ChapterItem, mangaTitle: String): List<ChapterPage> = withContext(Dispatchers.IO) {
        if (chapter.mangapillUrl?.contains("mangafire.to") == true) {
            val pages = mutableListOf<ChapterPage>()
            try {
                val request = Request.Builder()
                    .url(chapter.mangapillUrl)
                    .header("User-Agent", userAgent)
                    .header("Referer", "https://mangafire.to/")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val html = response.body?.string() ?: ""
                        val pattern = Pattern.compile("src=\"(https://[^\"]+)\"")
                        val matcher = pattern.matcher(html)
                        var pageNum = 1
                        while (matcher.find()) {
                            val imgUrl = matcher.group(1)
                            if (!imgUrl.isNullOrBlank() && (imgUrl.contains("mfcdn") || imgUrl.contains("mangafire"))) {
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

        // Fallback to MangaDex / Mangapill
        val dexId = mangadexClient.searchMangaId(mangaTitle)
        if (!dexId.isNullOrBlank()) {
            val dexChapters = mangadexClient.getChapters(dexId)
            val match = dexChapters.find { it.chapterNumber == chapter.chapterNumber } ?: dexChapters.firstOrNull()
            if (match?.mangadexChapterId != null) {
                val dexPages = mangadexClient.getChapterPages(match.mangadexChapterId)
                if (dexPages.isNotEmpty()) return@withContext dexPages
            }
        }

        mangapillClient.getChapterPages(chapter, mangaTitle)
    }
}
