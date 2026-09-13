package com.example.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChapterRangeFilter
import com.example.data.model.MangaItem
import com.example.data.model.MangaStatusFilter
import com.example.data.model.SearchFilter
import com.example.data.model.SortOption
import com.example.data.model.YearRangeFilter
import com.example.data.repository.CuratedMangaCatalog
import com.example.data.repository.MangaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(
        val results: List<MangaItem>,
        val totalCount: Int = results.size
    ) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

enum class TagFilterState {
    NEUTRAL,
    INCLUDED,
    EXCLUDED
}

class SearchViewModel(private val repository: MangaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(SearchFilter())
    val filter: StateFlow<SearchFilter> = _filter.asStateFlow()

    // Backward compatibility for single genre quick pill
    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    val genresList = listOf(
        "Action",
        "Adventure",
        "Comedy",
        "Drama",
        "Fantasy",
        "Horror",
        "Mystery",
        "Psychological",
        "Romance",
        "Sci-Fi",
        "Slice of Life",
        "Sports",
        "Supernatural",
        "Thriller"
    )

    private var searchJob: Job? = null

    init {
        // Load initial popular manga as search starting point
        performSearch("", _filter.value)
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        triggerDebouncedSearch()
    }

    fun getGenreState(genre: String): TagFilterState {
        val current = _filter.value
        return when {
            current.includedGenres.contains(genre) -> TagFilterState.INCLUDED
            current.excludedGenres.contains(genre) -> TagFilterState.EXCLUDED
            else -> TagFilterState.NEUTRAL
        }
    }

    /**
     * Cycles a genre through: NEUTRAL -> INCLUDED (+) -> EXCLUDED (-) -> NEUTRAL
     */
    fun cycleGenreState(genre: String) {
        val current = _filter.value
        val newFilter = when (getGenreState(genre)) {
            TagFilterState.NEUTRAL -> {
                current.copy(
                    includedGenres = current.includedGenres + genre,
                    excludedGenres = current.excludedGenres - genre
                )
            }
            TagFilterState.INCLUDED -> {
                current.copy(
                    includedGenres = current.includedGenres - genre,
                    excludedGenres = current.excludedGenres + genre
                )
            }
            TagFilterState.EXCLUDED -> {
                current.copy(
                    includedGenres = current.includedGenres - genre,
                    excludedGenres = current.excludedGenres - genre
                )
            }
        }
        updateFilterAndSearch(newFilter)
    }

    fun toggleGenreInclude(genre: String) {
        val current = _filter.value
        val isIncluded = current.includedGenres.contains(genre)
        val newIncluded = if (isIncluded) current.includedGenres - genre else current.includedGenres + genre
        val newExcluded = current.excludedGenres - genre
        updateFilterAndSearch(current.copy(includedGenres = newIncluded, excludedGenres = newExcluded))
    }

    fun toggleGenreExclude(genre: String) {
        val current = _filter.value
        val isExcluded = current.excludedGenres.contains(genre)
        val newExcluded = if (isExcluded) current.excludedGenres - genre else current.excludedGenres + genre
        val newIncluded = current.includedGenres - genre
        updateFilterAndSearch(current.copy(includedGenres = newIncluded, excludedGenres = newExcluded))
    }

    fun removeGenreFilter(genre: String) {
        val current = _filter.value
        val newFilter = current.copy(
            includedGenres = current.includedGenres - genre,
            excludedGenres = current.excludedGenres - genre
        )
        updateFilterAndSearch(newFilter)
    }

    fun setStatusFilter(status: MangaStatusFilter) {
        updateFilterAndSearch(_filter.value.copy(status = status))
    }

    fun setYearRangeFilter(yearRange: YearRangeFilter) {
        updateFilterAndSearch(_filter.value.copy(releaseYearRange = yearRange))
    }

    fun setChapterRangeFilter(chapterRange: ChapterRangeFilter) {
        updateFilterAndSearch(_filter.value.copy(chapterRange = chapterRange))
    }

    fun setSortOption(sortOption: SortOption) {
        updateFilterAndSearch(_filter.value.copy(sortBy = sortOption))
    }

    fun onGenreSelect(genre: String) {
        _selectedGenre.value = genre
        val newFilter = if (genre == "All") {
            _filter.value.copy(includedGenres = emptySet(), excludedGenres = emptySet())
        } else {
            _filter.value.copy(includedGenres = setOf(genre), excludedGenres = _filter.value.excludedGenres - genre)
        }
        updateFilterAndSearch(newFilter)
    }

    fun applyFilter(newFilter: SearchFilter) {
        updateFilterAndSearch(newFilter)
    }

    fun resetFilters() {
        _selectedGenre.value = "All"
        updateFilterAndSearch(SearchFilter())
    }

    private fun updateFilterAndSearch(newFilter: SearchFilter) {
        _filter.value = newFilter
        _selectedGenre.value = if (newFilter.includedGenres.size == 1 && newFilter.excludedGenres.isEmpty()) {
            newFilter.includedGenres.first()
        } else if (newFilter.includedGenres.isEmpty()) {
            "All"
        } else {
            "${newFilter.includedGenres.size} Selected"
        }
        triggerDebouncedSearch()
    }

    private fun triggerDebouncedSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            performSearch(_query.value, _filter.value)
        }
    }

    fun performSearch(q: String, currentFilter: SearchFilter) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val results = repository.searchManga(
                    query = q.takeIf { it.isNotBlank() },
                    filter = currentFilter
                )
                _uiState.value = SearchUiState.Success(results)
            } catch (e: Exception) {
                // Check curated catalog as instant fallback before erroring
                val curatedResults = CuratedMangaCatalog.search(
                    query = q.takeIf { it.isNotBlank() },
                    genre = null,
                    filter = currentFilter
                )
                if (curatedResults.isNotEmpty()) {
                    _uiState.value = SearchUiState.Success(curatedResults)
                } else {
                    _uiState.value = SearchUiState.Success(emptyList())
                }
            }
        }
    }

    fun getRandomFamousMangaId(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val famousFallbackIds = listOf(
                105778, // Chainsaw Man
                30013,  // One Piece
                30002,  // Berserk
                101517, // Jujutsu Kaisen
                105398, // Solo Leveling
                53390,  // Attack on Titan
                87216,  // Demon Slayer
                63327,  // Tokyo Ghoul
                30012,  // Bleach
                30011,  // Naruto
                108556, // Spy x Family
                30025,  // Fullmetal Alchemist
                30001,  // Monster
                30003,  // Vagabond
                34367,  // Vinland Saga
                30026,  // Hunter x Hunter
                30021,  // Death Note
                85486,  // My Hero Academia
                108631, // Blue Lock
                118586, // Frieren
                116005  // Oshi no Ko
            )

            val currentResults = (_uiState.value as? SearchUiState.Success)?.results
            if (!currentResults.isNullOrEmpty()) {
                val selected = currentResults.random()
                onResult(selected.id)
            } else {
                try {
                    val popularList = repository.getPopularManga()
                    if (popularList.isNotEmpty()) {
                        onResult(popularList.random().id)
                    } else {
                        onResult(famousFallbackIds.random())
                    }
                } catch (_: Exception) {
                    onResult(famousFallbackIds.random())
                }
            }
        }
    }
}

