package com.example.data.network

import com.example.data.model.ChapterItem
import com.example.data.model.ChapterPage
import com.example.data.model.MangaItem
import com.example.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.abs

class MangaDexClient {

    private val client: OkHttpClient = NetworkUtils.sharedHttpClient

    private val userAgent = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"

    /**
     * Fetches top popular manga sorted by followedCount with cover art from MangaDex.
     */
    suspend fun getPopularManga(page: Int = 1, perPage: Int = 24): List<MangaItem> = withContext(Dispatchers.IO) {
        val offset = (page - 1) * perPage
        val url = "https://api.mangadex.org/manga?limit=$perPage&offset=$offset&order[followedCount]=desc&hasAvailableChapters=true&contentRating[]=safe&contentRating[]=suggestive&includes[]=cover_art"
        fetchMangaListFromUrl(url)
    }

    /**
     * Fetches trending / highly rated manga with available chapters from MangaDex.
     */
    suspend fun getTrendingManga(page: Int = 1, perPage: Int = 24): List<MangaItem> = withContext(Dispatchers.IO) {
        val offset = (page - 1) * perPage
        val url = "https://api.mangadex.org/manga?limit=$perPage&offset=$offset&order[rating]=desc&hasAvailableChapters=true&contentRating[]=safe&contentRating[]=suggestive&includes[]=cover_art"
        fetchMangaListFromUrl(url)
    }

    /**
     * Searches MangaDex with query keyword and/or genre filter.
     */
    suspend fun searchManga(
        searchQuery: String? = null,
        genre: String? = null,
        page: Int = 1,
        perPage: Int = 24
    ): List<MangaItem> = withContext(Dispatchers.IO) {
        val offset = (page - 1) * perPage
        val sb = StringBuilder("https://api.mangadex.org/manga?limit=$perPage&offset=$offset&contentRating[]=safe&contentRating[]=suggestive&includes[]=cover_art&order[relevance]=desc")

        if (!searchQuery.isNullOrBlank()) {
            val cleanTitle = searchQuery.trim().replace(Regex("[^a-zA-Z0-9 ]"), " ").replace("\\s+".toRegex(), " ")
            if (cleanTitle.isNotBlank()) {
                val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
                sb.append("&title=").append(encoded)
            }
        }

        var results = fetchMangaListFromUrl(sb.toString())

        if (!genre.isNullOrBlank() && genre != "All") {
            results = results.filter { item ->
                item.genres.any { g -> g.equals(genre, ignoreCase = true) }
            }
        }

        results
    }

    private fun fetchMangaListFromUrl(url: String): List<MangaItem> {
        val list = mutableListOf<MangaItem>()
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val json = JSONObject(body)
                val data = json.optJSONArray("data") ?: return emptyList()

                for (i in 0 until data.length()) {
                    val mangaObj = data.optJSONObject(i) ?: continue
                    parseMangaDexItem(mangaObj)?.let { list.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseMangaDexItem(mangaObj: JSONObject): MangaItem? {
        val dexId = mangaObj.optString("id") ?: return null
        val attributes = mangaObj.optJSONObject("attributes") ?: return null

        val titleObj = attributes.optJSONObject("title")
        val romajiTitle = titleObj?.optString("ja-ro")
            ?: titleObj?.optString("en")
            ?: titleObj?.optString("ja")
            ?: "Unknown Title"

        var englishTitle = titleObj?.optString("en")
        if (englishTitle.isNullOrBlank()) {
            val altTitles = attributes.optJSONArray("altTitles")
            if (altTitles != null) {
                for (j in 0 until altTitles.length()) {
                    val alt = altTitles.optJSONObject(j) ?: continue
                    if (alt.has("en")) {
                        englishTitle = alt.optString("en")
                        break
                    }
                }
            }
        }

        val descObj = attributes.optJSONObject("description")
        val rawDesc = descObj?.optString("en") ?: ""
        val cleanDesc = rawDesc.lines().firstOrNull { it.isNotBlank() } ?: "Popular manga from MangaDex"

        val statusStr = attributes.optString("status", "RELEASING").uppercase()
        val year = attributes.optInt("year").takeIf { it > 0 }

        // Find cover file from relationships
        var coverFilename: String? = null
        val relationships = mangaObj.optJSONArray("relationships")
        if (relationships != null) {
            for (j in 0 until relationships.length()) {
                val rel = relationships.optJSONObject(j) ?: continue
                if (rel.optString("type") == "cover_art") {
                    val coverAttr = rel.optJSONObject("attributes")
                    coverFilename = coverAttr?.optString("fileName")
                    break
                }
            }
        }

        val coverUrl = if (!coverFilename.isNullOrBlank()) {
            "https://uploads.mangadex.org/covers/$dexId/$coverFilename"
        } else {
            "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/default.jpg"
        }

        // Extract genres from tags
        val genres = mutableListOf<String>()
        val tags = attributes.optJSONArray("tags")
        if (tags != null) {
            for (j in 0 until tags.length()) {
                val tag = tags.optJSONObject(j) ?: continue
                val tagAttr = tag.optJSONObject("attributes") ?: continue
                val tagName = tagAttr.optJSONObject("name")?.optString("en")
                if (!tagName.isNullOrBlank()) {
                    genres.add(tagName)
                }
            }
        }

        val links = attributes.optJSONObject("links")
        val anilistIdStr = links?.optString("al")
        val finalId = anilistIdStr?.toIntOrNull() ?: (abs(dexId.hashCode()) % 900000 + 100000)

        val lastChapter = attributes.optString("lastChapter").toIntOrNull()

        return MangaItem(
            id = finalId,
            titleRomaji = romajiTitle,
            titleEnglish = englishTitle,
            coverImage = coverUrl,
            bannerImage = null,
            description = cleanDesc,
            status = if (statusStr.contains("COMPLET")) "FINISHED" else "RELEASING",
            score = 85,
            genres = if (genres.isNotEmpty()) genres else listOf("Action", "Adventure"),
            chapters = lastChapter,
            volumes = null,
            format = "MANGA",
            startYear = year
        )
    }

    /**
     * Searches MangaDex for a manga by title and returns the MangaDex manga ID.
     */
    suspend fun searchMangaId(mangaTitle: String, titleRomaji: String? = null): String? = withContext(Dispatchers.IO) {
        var id = searchMangaIdQuery(mangaTitle)
        if (id == null && !titleRomaji.isNullOrBlank() && !titleRomaji.equals(mangaTitle, ignoreCase = true)) {
            id = searchMangaIdQuery(titleRomaji)
        }
        id
    }

    private fun searchMangaIdQuery(mangaTitle: String): String? {
        val cleanTitle = mangaTitle.trim()
            .replace(Regex("[^a-zA-Z0-9 ]"), "")
            .replace("\\s+".toRegex(), " ")

        if (cleanTitle.isBlank()) return null

        val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
        val url = "https://api.mangadex.org/manga?title=$encodedTitle&limit=10&contentRating[]=safe&contentRating[]=suggestive&order[relevance]=desc"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val json = JSONObject(body)
                val data = json.optJSONArray("data") ?: return@use null

                var fallbackId: String? = null

                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i) ?: continue
                    val itemId = item.optString("id") ?: continue
                    val attributes = item.optJSONObject("attributes") ?: continue

                    val titleObj = attributes.optJSONObject("title")
                    val mainTitle = titleObj?.optString("en")
                        ?: titleObj?.optString("ja-ro")
                        ?: titleObj?.optString("ja")
                        ?: ""

                    val lowerTitle = mainTitle.lowercase()
                    val isNovel = lowerTitle.contains("novel") || lowerTitle.contains("light novel")

                    if (fallbackId == null) fallbackId = itemId

                    if (!isNovel) {
                        return@use itemId
                    }
                }
                return@use fallbackId
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Fetches chapter list for a MangaDex manga ID.
     */
    suspend fun getChapters(mangadexMangaId: String): List<ChapterItem> = withContext(Dispatchers.IO) {
        val chapters = mutableListOf<ChapterItem>()
        val url = "https://api.mangadex.org/manga/$mangadexMangaId/feed?translatedLanguage[]=en&order[chapter]=desc&limit=100&includes[]=scanlation_group"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList<ChapterItem>()
                val body = response.body?.string() ?: return@use emptyList<ChapterItem>()
                val json = JSONObject(body)
                val data = json.optJSONArray("data") ?: return@use emptyList<ChapterItem>()

                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i) ?: continue
                    val chapterId = item.optString("id")
                    val attributes = item.optJSONObject("attributes") ?: continue

                    val chNum = attributes.optString("chapter", "").ifBlank { "${i + 1}" }
                    val chTitleAttr = attributes.optString("title", "").ifBlank { null }
                    val displayTitle = if (!chTitleAttr.isNullOrBlank()) {
                        "Ch. $chNum - $chTitleAttr"
                    } else {
                        "Chapter $chNum"
                    }

                    val publishAt = attributes.optString("publishAt", "")
                    val dateStr = if (publishAt.length >= 10) publishAt.substring(0, 10) else "Recent"

                    chapters.add(
                        ChapterItem(
                            id = "md-$chapterId",
                            title = displayTitle,
                            chapterNumber = chNum,
                            releaseDate = dateStr,
                            mangapillUrl = null,
                            mangadexChapterId = chapterId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        chapters
    }

    /**
     * Fetches chapter page image URLs from MangaDex at-home API.
     */
    suspend fun getChapterPages(mangadexChapterId: String): List<ChapterPage> = withContext(Dispatchers.IO) {
        val pages = mutableListOf<ChapterPage>()
        val url = "https://api.mangadex.org/at-home/server/$mangadexChapterId"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList<ChapterPage>()
                val body = response.body?.string() ?: return@use emptyList<ChapterPage>()
                val json = JSONObject(body)

                val baseUrl = json.optString("baseUrl")
                val chapterObj = json.optJSONObject("chapter") ?: return@use emptyList<ChapterPage>()
                val hash = chapterObj.optString("hash")

                // Prefer full resolution data array, or dataSaver if data is empty
                val dataArray = chapterObj.optJSONArray("data") ?: chapterObj.optJSONArray("dataSaver")
                val isSaver = chapterObj.optJSONArray("data") == null || (dataArray != null && dataArray.length() == 0)
                val folder = if (isSaver) "data-saver" else "data"

                if (!baseUrl.isNullOrBlank() && !hash.isNullOrBlank() && dataArray != null) {
                    for (i in 0 until dataArray.length()) {
                        val filename = dataArray.optString(i)
                        if (!filename.isNullOrBlank()) {
                            val imgUrl = "$baseUrl/$folder/$hash/$filename"
                            pages.add(ChapterPage(pageNumber = i + 1, imageUrl = imgUrl))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        pages
    }
}

