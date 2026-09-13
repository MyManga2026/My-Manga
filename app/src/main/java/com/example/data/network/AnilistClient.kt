package com.example.data.network

import com.example.data.model.MangaItem
import com.example.data.model.SearchFilter
import com.example.data.model.MangaStatusFilter
import com.example.data.model.YearRangeFilter
import com.example.data.model.ChapterRangeFilter
import com.example.data.model.SortOption
import com.example.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class AnilistClient {

    private val client: OkHttpClient = NetworkUtils.sharedHttpClient

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val graphqlUrl = "https://graphql.anilist.co"
    private val userAgent = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"

    // In-memory cache to prevent rate-limiting (429) on frequent screen navigation
    private val cache = ConcurrentHashMap<String, Pair<Long, List<MangaItem>>>()
    private val detailCache = ConcurrentHashMap<Int, Pair<Long, MangaItem>>()
    private val cacheTtlMs = 10 * 60 * 1000L // 10 minutes cache

    suspend fun getTrendingManga(page: Int = 1, perPage: Int = 24): List<MangaItem> = withContext(Dispatchers.IO) {
        val cacheKey = "trending_${page}_${perPage}"
        val cached = cache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.first < cacheTtlMs && cached.second.isNotEmpty()) {
            return@withContext cached.second
        }

        val query = """
            query (${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(type: MANGA, sort: TRENDING_DESC) {
                  id
                  title { romaji english native }
                  coverImage { extraLarge large medium }
                  bannerImage
                  description(asHtml: false)
                  status
                  averageScore
                  genres
                  chapters
                  volumes
                  format
                  startDate { year }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("page", page)
            put("perPage", perPage)
        }

        val results = executeGraphQL(query, variables)
        if (results.isNotEmpty()) {
            cache[cacheKey] = Pair(System.currentTimeMillis(), results)
        }
        results
    }

    suspend fun getPopularManga(page: Int = 1, perPage: Int = 24): List<MangaItem> = withContext(Dispatchers.IO) {
        val cacheKey = "popular_${page}_${perPage}"
        val cached = cache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.first < cacheTtlMs && cached.second.isNotEmpty()) {
            return@withContext cached.second
        }

        val query = """
            query (${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(type: MANGA, sort: POPULARITY_DESC) {
                  id
                  title { romaji english native }
                  coverImage { extraLarge large medium }
                  bannerImage
                  description(asHtml: false)
                  status
                  averageScore
                  genres
                  chapters
                  volumes
                  format
                  startDate { year }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("page", page)
            put("perPage", perPage)
        }

        val results = executeGraphQL(query, variables)
        if (results.isNotEmpty()) {
            cache[cacheKey] = Pair(System.currentTimeMillis(), results)
        }
        results
    }

    suspend fun searchManga(
        searchQuery: String? = null,
        genre: String? = null,
        filter: SearchFilter? = null,
        page: Int = 1,
        perPage: Int = 28
    ): List<MangaItem> = withContext(Dispatchers.IO) {
        val cleanQuery = searchQuery?.trim()?.takeIf { it.isNotBlank() }
        val incGenres = filter?.includedGenres?.toList() 
            ?: (genre?.trim()?.takeIf { it.isNotBlank() && it != "All" }?.let { listOf(it) } ?: emptyList())
        val excGenres = filter?.excludedGenres?.toList() ?: emptyList()
        val statusFilter = filter?.status?.anilistStatus
        val minYear = filter?.releaseYearRange?.minYear
        val maxYear = filter?.releaseYearRange?.maxYear
        val minCh = filter?.chapterRange?.minChapters
        val maxCh = filter?.chapterRange?.maxChapters
        val sortName = filter?.sortBy?.anilistSort ?: "POPULARITY_DESC"

        val cacheKey = "search_${cleanQuery ?: "null"}_inc${incGenres.joinToString(",")}_exc${excGenres.joinToString(",")}_st${statusFilter ?: "null"}_yr${minYear ?: 0}-${maxYear ?: 0}_ch${minCh ?: 0}-${maxCh ?: 0}_s${sortName}_p${page}"

        val cached = cache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.first < cacheTtlMs && cached.second.isNotEmpty()) {
            return@withContext cached.second
        }

        // Build dynamic GraphQL query args
        val varDefs = mutableListOf<String>()
        val mediaArgs = mutableListOf("type: MANGA", "format_in: [MANGA, ONE_SHOT]")

        varDefs.add("\$page: Int")
        varDefs.add("\$perPage: Int")

        if (cleanQuery != null) {
            varDefs.add("\$search: String")
            mediaArgs.add("search: \$search")
        }
        if (incGenres.isNotEmpty()) {
            varDefs.add("\$genre_in: [String]")
            mediaArgs.add("genre_in: \$genre_in")
        }
        if (excGenres.isNotEmpty()) {
            varDefs.add("\$genre_not_in: [String]")
            mediaArgs.add("genre_not_in: \$genre_not_in")
        }
        if (statusFilter != null) {
            varDefs.add("\$status: MediaStatus")
            mediaArgs.add("status: \$status")
        }
        if (minYear != null) {
            varDefs.add("\$startDate_greater: FuzzyDateInt")
            mediaArgs.add("startDate_greater: \$startDate_greater")
        }
        if (maxYear != null) {
            varDefs.add("\$startDate_lesser: FuzzyDateInt")
            mediaArgs.add("startDate_lesser: \$startDate_lesser")
        }
        if (minCh != null) {
            varDefs.add("\$chapters_greater: Int")
            mediaArgs.add("chapters_greater: \$chapters_greater")
        }
        if (maxCh != null) {
            varDefs.add("\$chapters_lesser: Int")
            mediaArgs.add("chapters_lesser: \$chapters_lesser")
        }

        mediaArgs.add("sort: [$sortName]")

        val queryHeader = "query (${varDefs.joinToString(", ")})"
        val mediaArgsStr = mediaArgs.joinToString(", ")

        val query = """
            $queryHeader {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media($mediaArgsStr) {
                  id
                  title { romaji english native }
                  coverImage { extraLarge large medium }
                  bannerImage
                  description(asHtml: false)
                  status
                  averageScore
                  genres
                  chapters
                  volumes
                  format
                  startDate { year }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("page", page)
            put("perPage", perPage)
            if (cleanQuery != null) put("search", cleanQuery)
            if (incGenres.isNotEmpty()) put("genre_in", JSONArray(incGenres))
            if (excGenres.isNotEmpty()) put("genre_not_in", JSONArray(excGenres))
            if (statusFilter != null) put("status", statusFilter)
            if (minYear != null) put("startDate_greater", (minYear * 10000))
            if (maxYear != null) put("startDate_lesser", (maxYear * 10000 + 1231))
            if (minCh != null) put("chapters_greater", minCh - 1)
            if (maxCh != null) put("chapters_lesser", maxCh + 1)
        }

        var results = executeGraphQL(query, variables)
        if (filter != null && results.isNotEmpty()) {
            results = results.filter { filter.matches(it) }
        }

        if (results.isNotEmpty()) {
            cache[cacheKey] = Pair(System.currentTimeMillis(), results)
        }
        results
    }

    suspend fun getMangaDetails(id: Int): MangaItem? = withContext(Dispatchers.IO) {
        val cached = detailCache[id]
        if (cached != null && System.currentTimeMillis() - cached.first < cacheTtlMs) {
            return@withContext cached.second
        }

        val query = """
            query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: MANGA) {
                id
                title { romaji english native }
                coverImage { extraLarge large medium }
                bannerImage
                description(asHtml: false)
                status
                averageScore
                genres
                chapters
                volumes
                format
                startDate { year }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("id", id)
        }

        val requestJson = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }

        val request = Request.Builder()
            .url(graphqlUrl)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Origin", "https://anilist.co")
            .header("Referer", "https://anilist.co/")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val json = JSONObject(body)
                val media = json.optJSONObject("data")?.optJSONObject("Media") ?: return@use null
                val item = parseMediaObject(media)
                if (item != null) {
                    detailCache[id] = Pair(System.currentTimeMillis(), item)
                }
                item
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getMangaRecommendations(id: Int): List<MangaItem> = withContext(Dispatchers.IO) {
        val cacheKey = "recs_${id}"
        val cached = cache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.first < cacheTtlMs && cached.second.isNotEmpty()) {
            return@withContext cached.second
        }

        val query = """
            query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: MANGA) {
                recommendations(page: 1, perPage: 12, sort: [RATING_DESC]) {
                  nodes {
                    mediaRecommendation {
                      id
                      title { romaji english native }
                      coverImage { extraLarge large medium }
                      bannerImage
                      description(asHtml: false)
                      status
                      averageScore
                      genres
                      chapters
                      volumes
                      format
                      startDate { year }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("id", id)
        }

        val requestJson = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }

        val request = Request.Builder()
            .url(graphqlUrl)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Origin", "https://anilist.co")
            .header("Referer", "https://anilist.co/")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = response.body?.string() ?: return@use emptyList()
                val json = JSONObject(body)
                val nodesArray = json.optJSONObject("data")
                    ?.optJSONObject("Media")
                    ?.optJSONObject("recommendations")
                    ?.optJSONArray("nodes") ?: return@use emptyList()

                val resultList = mutableListOf<MangaItem>()
                for (i in 0 until nodesArray.length()) {
                    val node = nodesArray.optJSONObject(i) ?: continue
                    val recMedia = node.optJSONObject("mediaRecommendation") ?: continue
                    parseMediaObject(recMedia)?.let { resultList.add(it) }
                }
                if (resultList.isNotEmpty()) {
                    cache[cacheKey] = Pair(System.currentTimeMillis(), resultList)
                }
                resultList
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun executeGraphQL(query: String, variables: JSONObject): List<MangaItem> {
        val requestJson = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }

        val request = Request.Builder()
            .url(graphqlUrl)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Origin", "https://anilist.co")
            .header("Referer", "https://anilist.co/")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = response.body?.string() ?: return@use emptyList()
                val json = JSONObject(body)
                val mediaArray = json.optJSONObject("data")
                    ?.optJSONObject("Page")
                    ?.optJSONArray("media") ?: return@use emptyList()

                val resultList = mutableListOf<MangaItem>()
                for (i in 0 until mediaArray.length()) {
                    val mediaObj = mediaArray.optJSONObject(i) ?: continue
                    parseMediaObject(mediaObj)?.let { resultList.add(it) }
                }
                resultList
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseMediaObject(obj: JSONObject): MangaItem? {
        val id = obj.optInt("id", -1)
        if (id == -1) return null

        val titleObj = obj.optJSONObject("title")
        val romaji = titleObj?.optString("romaji") ?: "Unknown Title"
        val english = titleObj?.optString("english")?.takeIf { it != "null" && it.isNotBlank() }
        val coverObj = obj.optJSONObject("coverImage")
        val cover = coverObj?.optString("extraLarge")
            ?: coverObj?.optString("large")
            ?: coverObj?.optString("medium")
            ?: ""

        val banner = obj.optString("bannerImage").takeIf { it != "null" && it.isNotBlank() }
        var rawDesc = obj.optString("description")
        if (rawDesc == "null") rawDesc = ""
        val cleanDesc = cleanAndFormatHtml(rawDesc)

        val status = obj.optString("status", "RELEASING").replace("_", " ")
        val score = if (obj.has("averageScore") && !obj.isNull("averageScore")) obj.optInt("averageScore") else null

        val genresArray = obj.optJSONArray("genres")
        val genres = mutableListOf<String>()
        if (genresArray != null) {
            for (i in 0 until genresArray.length()) {
                genres.add(genresArray.optString(i))
            }
        }

        val chapters = if (obj.has("chapters") && !obj.isNull("chapters")) obj.optInt("chapters") else null
        val volumes = if (obj.has("volumes") && !obj.isNull("volumes")) obj.optInt("volumes") else null
        val format = obj.optString("format", "MANGA").replace("_", " ")
        val startDateObj = obj.optJSONObject("startDate")
        val startYear = if (startDateObj != null && startDateObj.has("year") && !startDateObj.isNull("year")) startDateObj.optInt("year") else null

        return MangaItem(
            id = id,
            titleRomaji = romaji,
            titleEnglish = english,
            coverImage = cover,
            bannerImage = banner,
            description = cleanDesc,
            status = status,
            score = score,
            genres = genres,
            chapters = chapters,
            volumes = volumes,
            format = format,
            startYear = startYear
        )
    }

    private fun cleanAndFormatHtml(html: String): String {
        if (html.isBlank()) return ""
        var text = html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("<[^>]*>"), "")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&nbsp;", " ")
        text = text.lines().map { it.trim() }.joinToString("\n")
        return text.replace(Regex("\n{3,}"), "\n\n").trim()
    }
}

