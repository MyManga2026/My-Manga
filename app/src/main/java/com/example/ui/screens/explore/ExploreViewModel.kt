package com.example.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookmarkEntity
import com.example.data.local.DownloadEntity
import com.example.data.local.HistoryEntity
import com.example.data.model.MangaItem
import com.example.data.repository.CuratedMangaCatalog
import com.example.data.repository.MangaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ExploreUiState {
    object Loading : ExploreUiState()
    data class Success(
        val trendingManga: List<MangaItem>,
        val popularManga: List<MangaItem>,
        val selectedGenre: String? = null
    ) : ExploreUiState()
    data class Error(val message: String) : ExploreUiState()
}

class ExploreViewModel(private val repository: MangaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading)
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String>("All")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    val genresList = listOf(
        "All", "Action", "Adventure", "Comedy", "Drama", "Fantasy", 
        "Mystery", "Romance", "Sci-Fi", "Slice of Life", "Supernatural", 
        "Sports", "Psychological", "Horror"
    )

    val recentHistory: StateFlow<List<HistoryEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userBookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userDownloads: StateFlow<List<DownloadEntity>> = repository.allDownloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadHomeData()
    }

    fun selectGenre(genre: String) {
        _selectedGenre.value = genre
        if (genre != "All") {
            viewModelScope.launch {
                try {
                    val genreItems = repository.searchManga(genre = genre, page = 1)
                    val currentState = _uiState.value
                    if (currentState is ExploreUiState.Success && genreItems.isNotEmpty()) {
                        val updatedTrending = (currentState.trendingManga + genreItems).distinctBy { it.id }
                        val updatedPopular = (currentState.popularManga + genreItems).distinctBy { it.id }
                        _uiState.value = currentState.copy(
                            trendingManga = updatedTrending,
                            popularManga = updatedPopular,
                            selectedGenre = genre
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = ExploreUiState.Loading
            try {
                val trending = repository.getTrendingManga()
                val popular = repository.getPopularManga()
                
                val finalTrending = if (trending.isNotEmpty()) trending else CuratedMangaCatalog.getTrending()
                val finalPopular = if (popular.isNotEmpty()) popular else CuratedMangaCatalog.getPopular()

                _uiState.value = ExploreUiState.Success(
                    trendingManga = finalTrending,
                    popularManga = finalPopular,
                    selectedGenre = _selectedGenre.value
                )
            } catch (e: Exception) {
                val fallbackTrending = CuratedMangaCatalog.getTrending()
                val fallbackPopular = CuratedMangaCatalog.getPopular()

                _uiState.value = ExploreUiState.Success(
                    trendingManga = fallbackTrending,
                    popularManga = fallbackPopular,
                    selectedGenre = _selectedGenre.value
                )
            }
        }
    }
}
