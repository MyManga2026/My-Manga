package com.example.ui.screens.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.HistoryEntity
import com.example.data.model.ChapterItem
import com.example.data.model.MangaItem
import com.example.data.repository.MangaRepository
import com.example.utils.ReadingAnalyticsManager
import com.example.utils.UserPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class MangaDetailUiState {
    object Loading : MangaDetailUiState()
    data class Success(
        val manga: MangaItem,
        val chapters: List<ChapterItem>,
        val filteredChapters: List<ChapterItem>,
        val isBookmarked: Boolean,
        val readingHistory: HistoryEntity?,
        val downloadedChapterIds: List<String> = emptyList(),
        val downloadingChapterId: String? = null,
        val activeDownloadsProgress: Map<String, Float> = emptyMap(),
        val downloadQueue: List<String> = emptyList(),
        val isAscending: Boolean = false,
        val chapterSearchQuery: String = "",
        val recommendations: List<MangaItem> = emptyList(),
        val isLoadingRecommendations: Boolean = true,
        val readChapterIds: Set<String> = emptySet(),
        val selectedChapterIds: Set<String> = emptySet(),
        val isSelectionMode: Boolean = false
    ) : MangaDetailUiState()
    data class Error(val message: String) : MangaDetailUiState()
}

class MangaDetailViewModel(
    private val repository: MangaRepository,
    private val mangaId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow<MangaDetailUiState>(MangaDetailUiState.Loading)
    val uiState: StateFlow<MangaDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetailData()
    }

    fun loadDetailData() {
        viewModelScope.launch {
            _uiState.value = MangaDetailUiState.Loading
            try {
                val manga = repository.getMangaDetails(mangaId) ?: MangaItem(
                    id = mangaId,
                    titleRomaji = "Manga #$mangaId",
                    titleEnglish = "Manga #$mangaId",
                    coverImage = "",
                    description = "Manga details",
                    status = "Releasing"
                )

                val chaptersList = repository.getChapters(
                    mangaTitle = manga.displayTitle,
                    totalChapters = manga.chapters,
                    titleRomaji = manga.titleRomaji,
                    mangaId = manga.id
                )

                // Launch recommendations fetch concurrently
                viewModelScope.launch {
                    try {
                        val recs = repository.getMangaRecommendations(mangaId, manga.genres.firstOrNull())
                        val curr = _uiState.value
                        if (curr is MangaDetailUiState.Success) {
                            _uiState.value = curr.copy(
                                recommendations = recs,
                                isLoadingRecommendations = false
                            )
                        }
                    } catch (e: Exception) {
                        val curr = _uiState.value
                        if (curr is MangaDetailUiState.Success) {
                            _uiState.value = curr.copy(isLoadingRecommendations = false)
                        }
                    }
                }

                // Observe bookmark state, history, and downloads
                combine(
                    repository.isBookmarked(mangaId),
                    repository.getHistoryForManga(mangaId),
                    repository.getDownloadedChapterIdsForManga(mangaId)
                ) { isBookmarked, history, downloadedIds ->
                    Triple(isBookmarked, history, downloadedIds)
                }.collect { (isBookmarked, history, downloadedIds) ->
                    val currentState = _uiState.value
                    if (currentState is MangaDetailUiState.Success) {
                        val initialRead = currentState.readChapterIds.toMutableSet()
                        if (history?.chapterId != null) {
                            initialRead.add(history.chapterId)
                        }
                        _uiState.value = currentState.copy(
                            isBookmarked = isBookmarked,
                            readingHistory = history,
                            downloadedChapterIds = downloadedIds,
                            readChapterIds = initialRead
                        )
                    } else {
                        val initialRead = if (history?.chapterId != null) setOf(history.chapterId) else emptySet()
                        _uiState.value = MangaDetailUiState.Success(
                            manga = manga,
                            chapters = chaptersList,
                            filteredChapters = chaptersList,
                            isBookmarked = isBookmarked,
                            readingHistory = history,
                            downloadedChapterIds = downloadedIds,
                            readChapterIds = initialRead,
                            isAscending = false
                        )
                    }
                }
            } catch (e: Exception) {
                // If an unexpected error occurs, build fallback state from local DB / cache
                val fallbackManga = repository.getMangaDetails(mangaId) ?: MangaItem(
                    id = mangaId,
                    titleRomaji = "Manga #$mangaId",
                    titleEnglish = "Manga #$mangaId",
                    coverImage = "",
                    description = "Manga details",
                    status = "Releasing"
                )
                val fallbackChapters = repository.getChapters(
                    mangaTitle = fallbackManga.displayTitle,
                    totalChapters = fallbackManga.chapters,
                    titleRomaji = fallbackManga.titleRomaji,
                    mangaId = fallbackManga.id
                )
                _uiState.value = MangaDetailUiState.Success(
                    manga = fallbackManga,
                    chapters = fallbackChapters,
                    filteredChapters = fallbackChapters,
                    isBookmarked = false,
                    readingHistory = null,
                    downloadedChapterIds = emptyList(),
                    isAscending = false
                )
            }
        }
    }

    fun syncReadStatusFromPreferences(context: Context) {
        val curr = _uiState.value as? MangaDetailUiState.Success ?: return
        val savedRead = UserPreferences.getReadChapterIds(context, mangaId)
        val historyId = curr.readingHistory?.chapterId
        val combined = if (historyId != null) savedRead + historyId else savedRead
        if (combined != curr.readChapterIds) {
            _uiState.value = curr.copy(readChapterIds = combined)
        }
    }

    // --- Multi-Select & Batch Mode State Operations ---

    fun enterSelectionMode(initialChapterId: String? = null) {
        val curr = _uiState.value as? MangaDetailUiState.Success ?: return
        val newSelection = if (initialChapterId != null) setOf(initialChapterId) else emptySet()
        _uiState.value = curr.copy(
            isSelectionMode = true,
            selectedChapterIds = newSelection
        )
    }

    fun exitSelectionMode() {
        val curr = _uiState.value as? MangaDetailUiState.Success ?: return
        _uiState.value = curr.copy(
            isSelectionMode = false,
            selectedChapterIds = emptySet()
        )
    }

    fun toggleChapterSelection(chapterId: String) {
        val curr = _uiState.value as? MangaDetailUiState.Success ?: return
        val newSelection = if (curr.selectedChapterIds.contains(chapterId)) {
            curr.selectedChapterIds - chapterId
        } else {
            curr.selectedChapterIds + chapterId
        }
        _uiState.value = curr.copy(
            selectedChapterIds = newSelection,
            isSelectionMode = newSelection.isNotEmpty()
        )
    }

    fun selectAllChapters() {
        val curr = _uiState.value as? MangaDetailUiState.Success ?: return
        _uiState.value = curr.copy(
            isSelectionMode = true,
            selectedChapterIds = curr.filteredChapters.map { it.id }.toSet()
        )
    }

    fun clearSelection() {
        val curr = _uiState.value as? MangaDetailUiState.Success ?: return
        _uiState.value = curr.copy(
            selectedChapterIds = emptySet(),
            isSelectionMode = false
        )
    }

    fun selectNextChapters(count: Int) {
        val curr = _uiState.value as? MangaDetailUiState.Success ?: return
        val sortedAscending = curr.chapters.sortedBy { parseChapterNumber(it) }
        val unreadChapters = sortedAscending.filter { !curr.readChapterIds.contains(it.id) }
        val toSelect = if (unreadChapters.isNotEmpty()) {
            unreadChapters.take(count)
        } else {
            val undownloaded = sortedAscending.filter { !curr.downloadedChapterIds.contains(it.id) }
            if (undownloaded.isNotEmpty()) undownloaded.take(count) else sortedAscending.take(count)
        }

        _uiState.value = curr.copy(
            isSelectionMode = true,
            selectedChapterIds = toSelect.map { it.id }.toSet()
        )
    }

    fun selectUnreadChapters() {
        val curr = _uiState.value as? MangaDetailUiState.Success ?: return
        val unread = curr.chapters.filter { !curr.readChapterIds.contains(it.id) }
        val toSelect = if (unread.isNotEmpty()) unread else curr.chapters
        _uiState.value = curr.copy(
            isSelectionMode = true,
            selectedChapterIds = toSelect.map { it.id }.toSet()
        )
    }

    fun downloadSelectedChapters(context: Context) {
        val curr = _uiState.value as? MangaDetailUiState.Success ?: return
        if (curr.selectedChapterIds.isEmpty()) return

        val chaptersToDownload = curr.chapters.filter { ch ->
            curr.selectedChapterIds.contains(ch.id) &&
            !curr.downloadedChapterIds.contains(ch.id) &&
            !curr.downloadQueue.contains(ch.id) &&
            curr.downloadingChapterId != ch.id
        }

        if (chaptersToDownload.isNotEmpty()) {
            val newQueue = curr.downloadQueue + chaptersToDownload.map { it.id }
            val newProgress = curr.activeDownloadsProgress.toMutableMap()
            chaptersToDownload.forEach { newProgress[it.id] = 0f }

            _uiState.value = curr.copy(
                downloadQueue = newQueue,
                activeDownloadsProgress = newProgress,
                isSelectionMode = false,
                selectedChapterIds = emptySet()
            )
            processDownloadQueue(context)
        } else {
            _uiState.value = curr.copy(
                isSelectionMode = false,
                selectedChapterIds = emptySet()
            )
        }
    }

    fun downloadNextChaptersDirectly(context: Context, count: Int) {
        val curr = _uiState.value as? MangaDetailUiState.Success ?: return
        val sortedAscending = curr.chapters.sortedBy { parseChapterNumber(it) }
        val candidates = sortedAscending.filter { ch ->
            !curr.downloadedChapterIds.contains(ch.id) &&
            !curr.downloadQueue.contains(ch.id) &&
            curr.downloadingChapterId != ch.id &&
            !curr.readChapterIds.contains(ch.id)
        }.ifEmpty {
            sortedAscending.filter { ch ->
                !curr.downloadedChapterIds.contains(ch.id) &&
                !curr.downloadQueue.contains(ch.id) &&
                curr.downloadingChapterId != ch.id
            }
        }.take(count)

        if (candidates.isEmpty()) return

        val newQueue = curr.downloadQueue + candidates.map { it.id }
        val newProgress = curr.activeDownloadsProgress.toMutableMap()
        candidates.forEach { newProgress[it.id] = 0f }

        _uiState.value = curr.copy(
            downloadQueue = newQueue,
            activeDownloadsProgress = newProgress,
            isSelectionMode = false,
            selectedChapterIds = emptySet()
        )
        processDownloadQueue(context)
    }

    fun downloadUnreadChaptersDirectly(context: Context) {
        val curr = _uiState.value as? MangaDetailUiState.Success ?: return
        val sortedAscending = curr.chapters.sortedBy { parseChapterNumber(it) }
        val candidates = sortedAscending.filter { ch ->
            !curr.downloadedChapterIds.contains(ch.id) &&
            !curr.downloadQueue.contains(ch.id) &&
            curr.downloadingChapterId != ch.id &&
            !curr.readChapterIds.contains(ch.id)
        }

        if (candidates.isEmpty()) return

        val newQueue = curr.downloadQueue + candidates.map { it.id }
        val newProgress = curr.activeDownloadsProgress.toMutableMap()
        candidates.forEach { newProgress[it.id] = 0f }

        _uiState.value = curr.copy(
            downloadQueue = newQueue,
            activeDownloadsProgress = newProgress,
            isSelectionMode = false,
            selectedChapterIds = emptySet()
        )
        processDownloadQueue(context)
    }

    fun markSelectedAsRead(context: Context) {
        val curr = _uiState.value as? MangaDetailUiState.Success ?: return
        if (curr.selectedChapterIds.isEmpty()) return

        UserPreferences.markChaptersRead(context, curr.manga.id, curr.selectedChapterIds)
        val updatedRead = curr.readChapterIds + curr.selectedChapterIds

        curr.selectedChapterIds.forEach { _ ->
            ReadingAnalyticsManager.recordChapterRead(context, curr.manga.genres)
        }

        _uiState.value = curr.copy(
            readChapterIds = updatedRead,
            isSelectionMode = false,
            selectedChapterIds = emptySet()
        )
    }

    fun markSelectedAsUnread(context: Context) {
        val curr = _uiState.value as? MangaDetailUiState.Success ?: return
        if (curr.selectedChapterIds.isEmpty()) return

        UserPreferences.markChaptersUnread(context, curr.manga.id, curr.selectedChapterIds)
        val updatedRead = curr.readChapterIds - curr.selectedChapterIds

        _uiState.value = curr.copy(
            readChapterIds = updatedRead,
            isSelectionMode = false,
            selectedChapterIds = emptySet()
        )
    }

    fun deleteSelectedDownloads(context: Context) {
        val curr = _uiState.value as? MangaDetailUiState.Success ?: return
        val toDelete = curr.chapters.filter { curr.selectedChapterIds.contains(it.id) && curr.downloadedChapterIds.contains(it.id) }
        
        viewModelScope.launch {
            toDelete.forEach { ch ->
                val downloadId = "${curr.manga.id}_${ch.id}"
                repository.deleteDownload(context, downloadId, curr.manga.id, ch.id)
            }
        }

        _uiState.value = curr.copy(
            isSelectionMode = false,
            selectedChapterIds = emptySet()
        )
    }

    private fun parseChapterNumber(chapter: ChapterItem): Float {
        val raw = chapter.chapterNumber.ifBlank { chapter.title }
        val cleaned = raw.replace(Regex("[^0-9.]"), "")
        return cleaned.toFloatOrNull() ?: 0f
    }

    private var isWorkerActive = false

    fun downloadChapter(context: Context, chapter: ChapterItem) {
        val currentState = _uiState.value as? MangaDetailUiState.Success ?: return
        if (currentState.downloadedChapterIds.contains(chapter.id)) return
        if (currentState.downloadQueue.contains(chapter.id) || currentState.downloadingChapterId == chapter.id) return

        _uiState.value = currentState.copy(
            downloadQueue = currentState.downloadQueue + chapter.id,
            activeDownloadsProgress = currentState.activeDownloadsProgress + (chapter.id to 0f)
        )

        processDownloadQueue(context)
    }

    private fun processDownloadQueue(context: Context) {
        if (isWorkerActive) return
        isWorkerActive = true

        viewModelScope.launch {
            while (true) {
                val state = _uiState.value as? MangaDetailUiState.Success
                if (state == null) break
                val nextChapterId = state.downloadQueue.firstOrNull()
                if (nextChapterId == null) break

                val chapterToDownload = state.chapters.find { it.id == nextChapterId }
                if (chapterToDownload == null) {
                    val curr = _uiState.value as? MangaDetailUiState.Success
                    if (curr != null) {
                        _uiState.value = curr.copy(
                            downloadQueue = curr.downloadQueue.filter { it != nextChapterId },
                            activeDownloadsProgress = curr.activeDownloadsProgress - nextChapterId
                        )
                    }
                    continue
                }

                // Update active downloading chapter ID
                val activeState = _uiState.value as? MangaDetailUiState.Success
                if (activeState != null) {
                    _uiState.value = activeState.copy(
                        downloadingChapterId = nextChapterId
                    )
                }

                // Execute download
                repository.downloadChapter(
                    context = context,
                    manga = state.manga,
                    chapter = chapterToDownload,
                    onProgress = { downloaded, total ->
                        val progress = if (total > 0) downloaded.toFloat() / total else 0f
                        val currentSuccess = _uiState.value as? MangaDetailUiState.Success
                        if (currentSuccess != null) {
                            _uiState.value = currentSuccess.copy(
                                activeDownloadsProgress = currentSuccess.activeDownloadsProgress + (nextChapterId to progress)
                            )
                        }
                    }
                )

                // Clean up state after download completes or fails
                val finalSuccess = _uiState.value as? MangaDetailUiState.Success
                if (finalSuccess != null) {
                    _uiState.value = finalSuccess.copy(
                        downloadingChapterId = null,
                        downloadQueue = finalSuccess.downloadQueue.filter { it != nextChapterId },
                        activeDownloadsProgress = finalSuccess.activeDownloadsProgress - nextChapterId
                    )
                }
            }
            isWorkerActive = false
        }
    }

    fun deleteDownload(context: Context, chapter: ChapterItem) {
        val currentState = _uiState.value as? MangaDetailUiState.Success ?: return
        viewModelScope.launch {
            val downloadId = "${currentState.manga.id}_${chapter.id}"
            repository.deleteDownload(context, downloadId, currentState.manga.id, chapter.id)
        }
    }

    fun toggleBookmark() {
        val currentState = _uiState.value as? MangaDetailUiState.Success ?: return
        viewModelScope.launch {
            repository.toggleBookmark(currentState.manga, currentState.isBookmarked)
        }
    }

    fun filterChapters(query: String) {
        val currentState = _uiState.value as? MangaDetailUiState.Success ?: return
        val filtered = filterAndSort(currentState.chapters, query, currentState.isAscending)
        _uiState.value = currentState.copy(
            chapterSearchQuery = query,
            filteredChapters = filtered
        )
    }

    fun toggleSortOrder() {
        val currentState = _uiState.value as? MangaDetailUiState.Success ?: return
        val newAsc = !currentState.isAscending
        val filtered = filterAndSort(currentState.chapters, currentState.chapterSearchQuery, newAsc)
        _uiState.value = currentState.copy(
            isAscending = newAsc,
            filteredChapters = filtered
        )
    }

    private fun filterAndSort(chapters: List<ChapterItem>, query: String, ascending: Boolean): List<ChapterItem> {
        var list = if (query.isBlank()) {
            chapters
        } else {
            chapters.filter { it.title.contains(query, ignoreCase = true) || it.chapterNumber.contains(query) }
        }

        return if (ascending) {
            list.reversed()
        } else {
            list
        }
    }
}
