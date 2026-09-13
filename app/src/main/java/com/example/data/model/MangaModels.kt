package com.example.data.model

data class MangaItem(
    val id: Int,
    val titleRomaji: String,
    val titleEnglish: String? = null,
    val coverImage: String = "",
    val bannerImage: String? = null,
    val description: String = "",
    val status: String = "Offline",
    val score: Int? = null,
    val genres: List<String> = emptyList(),
    val chapters: Int? = null,
    val volumes: Int? = null,
    val format: String? = null,
    val startYear: Int? = null
) {
    val displayTitle: String
        get() = titleEnglish?.takeIf { it.isNotBlank() } ?: titleRomaji
}

data class ChapterItem(
    val id: String,
    val title: String,
    val chapterNumber: String,
    val releaseDate: String? = null,
    val mangapillUrl: String? = null,
    val mangadexChapterId: String? = null
)

data class ChapterPage(
    val pageNumber: Int,
    val imageUrl: String
)

enum class ReaderMode {
    VERTICAL_CONTINUOUS,
    HORIZONTAL_PAGED
}

enum class ReadingDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT
}

enum class ReaderContentScale {
    FIT_WIDTH,
    FIT_HEIGHT,
    FIT_SCREEN
}

enum class ReaderBackground {
    BLACK,
    WHITE,
    SEPIA
}

enum class MangaProvider(
    val id: String,
    val displayName: String,
    val domainUrl: String
) {
    MANGAPILL("mangapill", "Mangapill", "mangapill.com"),
    WEEBCENTRAL("weebcentral", "WeebCentral", "weebcentral.com"),
    MANGAFIRE("mangafire", "MangaFire", "mangafire.to"),
    MANGADEX("mangadex", "MangaDex", "mangadex.org")
}

data class SearchFilter(
    val includedGenres: Set<String> = emptySet(),
    val excludedGenres: Set<String> = emptySet(),
    val status: MangaStatusFilter = MangaStatusFilter.ALL,
    val releaseYearRange: YearRangeFilter = YearRangeFilter.ALL,
    val chapterRange: ChapterRangeFilter = ChapterRangeFilter.ALL,
    val sortBy: SortOption = SortOption.POPULARITY
) {
    val activeFilterCount: Int
        get() {
            var count = includedGenres.size + excludedGenres.size
            if (status != MangaStatusFilter.ALL) count++
            if (releaseYearRange != YearRangeFilter.ALL) count++
            if (chapterRange != ChapterRangeFilter.ALL) count++
            if (sortBy != SortOption.POPULARITY) count++
            return count
        }

    val isDefault: Boolean
        get() = activeFilterCount == 0

    fun matches(item: MangaItem): Boolean {
        // 1. Included genres (must contain ALL included genres)
        if (includedGenres.isNotEmpty()) {
            val itemGenresLower = item.genres.map { it.lowercase() }
            val hasAllIncluded = includedGenres.all { inc ->
                itemGenresLower.contains(inc.lowercase())
            }
            if (!hasAllIncluded) return false
        }

        // 2. Excluded genres (must NOT contain any excluded genres)
        if (excludedGenres.isNotEmpty()) {
            val itemGenresLower = item.genres.map { it.lowercase() }
            val hasAnyExcluded = excludedGenres.any { exc ->
                itemGenresLower.contains(exc.lowercase())
            }
            if (hasAnyExcluded) return false
        }

        // 3. Status filter
        if (status != MangaStatusFilter.ALL) {
            val itemStatus = item.status.uppercase()
            val matchesStatus = when (status) {
                MangaStatusFilter.RELEASING -> itemStatus.contains("RELEAS") || itemStatus.contains("ONGOING") || itemStatus.contains("PUBLISH")
                MangaStatusFilter.FINISHED -> itemStatus.contains("FINISH") || itemStatus.contains("COMPLET")
                MangaStatusFilter.HIATUS -> itemStatus.contains("HIATUS")
                MangaStatusFilter.NOT_YET_RELEASED -> itemStatus.contains("NOT_YET") || itemStatus.contains("UPCOMING")
                MangaStatusFilter.ALL -> true
            }
            if (!matchesStatus) return false
        }

        // 4. Release year filter
        if (releaseYearRange != YearRangeFilter.ALL) {
            val year = item.startYear
            if (year != null) {
                val min = releaseYearRange.minYear
                val max = releaseYearRange.maxYear
                if (min != null && year < min) return false
                if (max != null && year > max) return false
            }
        }

        // 5. Chapter count filter
        if (chapterRange != ChapterRangeFilter.ALL) {
            val ch = item.chapters
            if (ch != null) {
                val min = chapterRange.minChapters
                val max = chapterRange.maxChapters
                if (min != null && ch < min) return false
                if (max != null && ch > max) return false
            }
        }

        return true
    }
}

enum class MangaStatusFilter(val label: String, val anilistStatus: String?) {
    ALL("All Status", null),
    RELEASING("Ongoing", "RELEASING"),
    FINISHED("Completed", "FINISHED"),
    HIATUS("On Hiatus", "HIATUS"),
    NOT_YET_RELEASED("Upcoming", "NOT_YET_RELEASED")
}

enum class YearRangeFilter(val label: String, val minYear: Int?, val maxYear: Int?) {
    ALL("All Years", null, null),
    Y2024_PLUS("2024+", 2024, null),
    Y2020_2023("2020 - 2023", 2020, 2023),
    Y2015_2019("2015 - 2019", 2015, 2019),
    Y2010_2014("2010 - 2014", 2010, 2014),
    Y2000_2009("2000s", 2000, 2009),
    CLASSIC("Classic (<2000)", null, 1999)
}

enum class ChapterRangeFilter(val label: String, val minChapters: Int?, val maxChapters: Int?) {
    ALL("All Lengths", null, null),
    ONESHOT("Oneshot (1 ch)", 1, 1),
    SHORT("Short (<30 ch)", null, 29),
    MEDIUM("Medium (30-100 ch)", 30, 100),
    LONG("Long (100-300 ch)", 101, 300),
    EPIC("Epic (300+ ch)", 301, null)
}

enum class SortOption(val label: String, val anilistSort: String) {
    POPULARITY("Most Popular", "POPULARITY_DESC"),
    RATING("Highest Rated", "SCORE_DESC"),
    TRENDING("Trending", "TRENDING_DESC"),
    NEWEST("Newest Release", "START_DATE_DESC")
}

