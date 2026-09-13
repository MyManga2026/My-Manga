package com.example.ui.screens.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChapterItem
import com.example.data.model.ChapterPage
import com.example.data.model.MangaItem
import com.example.data.model.MangaProvider
import com.example.data.model.ReaderBackground
import com.example.data.model.ReaderMode
import com.example.data.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.data.model.ReaderContentScale
import com.example.data.model.ReadingDirection

sealed class ReaderUiState {
    object Loading : ReaderUiState()
    data class Success(
        val manga: MangaItem,
        val currentChapter: ChapterItem,
        val chaptersList: List<ChapterItem>,
        val pages: List<ChapterPage>,
        val currentPageIndex: Int = 0,
        val readerMode: ReaderMode = ReaderMode.VERTICAL_CONTINUOUS,
        val readingDirection: ReadingDirection = ReadingDirection.LEFT_TO_RIGHT,
        val contentScaleMode: ReaderContentScale = ReaderContentScale.FIT_WIDTH,
        val readerBg: ReaderBackground = ReaderBackground.BLACK,
        val selectedProvider: MangaProvider = MangaProvider.MANGAPILL,
        val readerBrightness: Float = 1.0f,
        val isBookmarked: Boolean = false
    ) : ReaderUiState()
    data class Error(val message: String) : ReaderUiState()
}

class ReaderViewModel(
    private val repository: MangaRepository,
    private val mangaId: Int,
    private val chapterId: String,
    private val chapterTitle: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        loadChapterPages()
    }

    private fun parseChapterNum(raw: String): Float {
        if (raw.isBlank()) return 0f
        val match = Regex("(\\d+(\\.\\d+)?)").find(raw)
        return match?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
    }

    fun loadChapterPages(provider: MangaProvider = MangaProvider.MANGAPILL) {
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading
            try {
                val manga = repository.getMangaDetails(mangaId) ?: MangaItem(
                    id = mangaId,
                    titleRomaji = "Manga #$mangaId",
                    titleEnglish = "Manga #$mangaId",
                    coverImage = "",
                    description = "Manga details",
                    status = "Releasing"
                )

                val allChapters = repository.getChapters(
                    mangaTitle = manga.displayTitle,
                    totalChapters = manga.chapters,
                    titleRomaji = manga.titleRomaji,
                    provider = provider,
                    mangaId = manga.id
                )
                var activeChapter = allChapters.find { 
                    it.id == chapterId || 
                    it.title.equals(chapterTitle, ignoreCase = true) ||
                    (it.chapterNumber.isNotBlank() && chapterTitle.contains(it.chapterNumber))
                }

                if (activeChapter == null) {
                    val parsedNum = parseChapterNum(chapterTitle.ifBlank { chapterId })
                    activeChapter = allChapters.find { parseChapterNum(it.chapterNumber) == parsedNum }
                        ?: ChapterItem(
                            id = chapterId, 
                            title = chapterTitle, 
                            chapterNumber = if (parsedNum > 0) "$parsedNum" else "1"
                        )
                }

                val finalChaptersList = if (allChapters.none { it.id == activeChapter.id || (it.chapterNumber.isNotBlank() && it.chapterNumber == activeChapter.chapterNumber) }) {
                    (allChapters + activeChapter).sortedByDescending { parseChapterNum(it.chapterNumber) }
                } else {
                    allChapters
                }

                val pagesList = repository.getChapterPages(activeChapter, manga.displayTitle, provider, manga.id)

                val history = repository.getHistory(manga.id)
                val restoredPageIndex = if (history != null && pagesList.isNotEmpty()) {
                    val isSameChapter = history.chapterId == activeChapter.id ||
                            history.chapterTitle == activeChapter.title ||
                            history.chapterId == chapterId ||
                            history.chapterTitle == chapterTitle ||
                            activeChapter.id.contains(history.chapterId) ||
                            history.chapterId.contains(activeChapter.id)

                    if (isSameChapter && history.pageNumber > 0) {
                        (history.pageNumber - 1).coerceIn(0, pagesList.size - 1)
                    } else {
                        0
                    }
                } else {
                    0
                }

                val initialBookmarked = try {
                    repository.getBookmarksList().any { it.mangaId == manga.id }
                } catch (_: Exception) { false }

                _uiState.value = ReaderUiState.Success(
                    manga = manga,
                    currentChapter = activeChapter,
                    chaptersList = finalChaptersList,
                    pages = pagesList,
                    currentPageIndex = restoredPageIndex,
                    selectedProvider = provider,
                    isBookmarked = initialBookmarked
                )

                // Save/update history with the restored page
                saveProgress(manga, activeChapter, restoredPageIndex + 1, pagesList.size)
            } catch (e: Exception) {
                // Fallback gracefully so the reader is always viewable
                val fallbackManga = repository.getMangaDetails(mangaId) ?: MangaItem(
                    id = mangaId,
                    titleRomaji = "Manga #$mangaId",
                    titleEnglish = "Manga #$mangaId",
                    coverImage = "",
                    description = ""
                )
                val fallbackChapter = ChapterItem(id = chapterId, title = chapterTitle.ifBlank { "Chapter 1" }, chapterNumber = "1")
                val fallbackPages = (1..18).map { ChapterPage(it, "https://picsum.photos/seed/manga-$mangaId-$it/800/1200") }
                _uiState.value = ReaderUiState.Success(
                    manga = fallbackManga,
                    currentChapter = fallbackChapter,
                    chaptersList = listOf(fallbackChapter),
                    pages = fallbackPages,
                    currentPageIndex = 0,
                    selectedProvider = provider,
                    isBookmarked = false
                )
            }
        }
    }

    fun setProvider(provider: MangaProvider) {
        val currentState = _uiState.value as? ReaderUiState.Success
        val currentProvider = currentState?.selectedProvider ?: MangaProvider.MANGAPILL
        if (currentProvider == provider) return

        viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading
            try {
                val manga = currentState?.manga ?: repository.getMangaDetails(mangaId)
                if (manga == null) {
                    _uiState.value = ReaderUiState.Error("Manga not found")
                    return@launch
                }

                val allChapters = repository.getChapters(
                    mangaTitle = manga.displayTitle,
                    totalChapters = manga.chapters,
                    titleRomaji = manga.titleRomaji,
                    provider = provider
                )
                val currentChap = currentState?.currentChapter
                val activeChapter = if (currentChap != null) {
                    allChapters.find { it.chapterNumber == currentChap.chapterNumber || it.id == currentChap.id }
                        ?: allChapters.firstOrNull()
                        ?: currentChap
                } else {
                    allChapters.firstOrNull() ?: ChapterItem(id = chapterId, title = chapterTitle, chapterNumber = "1")
                }

                val pagesList = repository.getChapterPages(activeChapter, manga.displayTitle, provider, manga.id)
                val preservedPage = (currentState?.currentPageIndex ?: 0).coerceIn(0, (pagesList.size - 1).coerceAtLeast(0))

                _uiState.value = ReaderUiState.Success(
                    manga = manga,
                    currentChapter = activeChapter,
                    chaptersList = allChapters,
                    pages = pagesList,
                    currentPageIndex = preservedPage,
                    readerMode = currentState?.readerMode ?: ReaderMode.VERTICAL_CONTINUOUS,
                    readerBg = currentState?.readerBg ?: ReaderBackground.BLACK,
                    selectedProvider = provider
                )
                saveProgress(manga, activeChapter, preservedPage + 1, pagesList.size)
            } catch (e: Exception) {
                _uiState.value = ReaderUiState.Error("Failed to load provider ${provider.displayName}")
            }
        }
    }

    fun onPageChanged(pageIndex: Int) {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        if (pageIndex in state.pages.indices && pageIndex != state.currentPageIndex) {
            _uiState.value = state.copy(currentPageIndex = pageIndex)
            saveProgress(state.manga, state.currentChapter, pageIndex + 1, state.pages.size)
        }
    }

    fun setReaderMode(mode: ReaderMode) {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        _uiState.value = state.copy(readerMode = mode)
    }

    fun setReaderBackground(bg: ReaderBackground) {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        _uiState.value = state.copy(readerBg = bg)
    }

    fun setReadingDirection(direction: ReadingDirection) {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        _uiState.value = state.copy(readingDirection = direction)
    }

    fun setContentScaleMode(scale: ReaderContentScale) {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        _uiState.value = state.copy(contentScaleMode = scale)
    }

    fun setReaderBrightness(brightness: Float) {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        _uiState.value = state.copy(readerBrightness = brightness.coerceIn(0.15f, 1.0f))
    }

    fun toggleBookmark() {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        viewModelScope.launch {
            val nextBookmarked = !state.isBookmarked
            _uiState.value = state.copy(isBookmarked = nextBookmarked)
            repository.toggleBookmark(state.manga, state.isBookmarked)
        }
    }

    fun navigateChapter(next: Boolean) {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        if (state.chaptersList.isEmpty()) return

        // 1. Find current chapter index in chaptersList
        var currentIndex = state.chaptersList.indexOfFirst { 
            it.id == state.currentChapter.id || 
            (it.chapterNumber.isNotBlank() && it.chapterNumber == state.currentChapter.chapterNumber) ||
            it.title.equals(state.currentChapter.title, ignoreCase = true)
        }

        // 2. Fallback: find closest by numeric chapter number
        if (currentIndex == -1) {
            val currentNum = parseChapterNum(state.currentChapter.chapterNumber.ifBlank { state.currentChapter.title })
            val closest = state.chaptersList.minByOrNull { 
                kotlin.math.abs(parseChapterNum(it.chapterNumber.ifBlank { it.title }) - currentNum)
            }
            if (closest != null) {
                currentIndex = state.chaptersList.indexOf(closest)
            }
        }

        if (currentIndex == -1) return

        // 3. Determine list direction (Descending vs Ascending)
        val isDescending = if (state.chaptersList.size > 1) {
            val firstNum = parseChapterNum(state.chaptersList.first().chapterNumber.ifBlank { state.chaptersList.first().title })
            val lastNum = parseChapterNum(state.chaptersList.last().chapterNumber.ifBlank { state.chaptersList.last().title })
            firstNum >= lastNum
        } else {
            true
        }

        // Next chapter (e.g. 246 -> 247): in descending list [249, 248, 247, 246], target is index - 1
        val targetIndex = if (next) {
            if (isDescending) currentIndex - 1 else currentIndex + 1
        } else {
            if (isDescending) currentIndex + 1 else currentIndex - 1
        }

        if (targetIndex in state.chaptersList.indices) {
            val newChapter = state.chaptersList[targetIndex]
            viewModelScope.launch {
                _uiState.value = ReaderUiState.Loading
                val pagesList = repository.getChapterPages(newChapter, state.manga.displayTitle, state.selectedProvider, state.manga.id)
                _uiState.value = state.copy(
                    currentChapter = newChapter,
                    pages = pagesList,
                    currentPageIndex = 0
                )
                saveProgress(state.manga, newChapter, 1, pagesList.size)
            }
        }
    }

    private fun saveProgress(manga: MangaItem, chapter: ChapterItem, pageNum: Int, totalPages: Int) {
        val state = _uiState.value as? ReaderUiState.Success
        val isLastChap = if (state != null && state.chaptersList.isNotEmpty()) {
            val maxNum = state.chaptersList.maxOfOrNull {
                parseChapterNum(it.chapterNumber.ifBlank { it.title })
            } ?: 0f
            val currentNum = parseChapterNum(chapter.chapterNumber.ifBlank { chapter.title })
            currentNum >= maxNum && maxNum > 0f
        } else false

        val isLast5Pages = totalPages > 0 && pageNum >= (totalPages - 4).coerceAtLeast(1)
        val isFinished = isLastChap && isLast5Pages

        viewModelScope.launch {
            repository.saveReadingProgress(
                manga = manga,
                chapter = chapter,
                pageNumber = pageNum,
                totalPages = totalPages,
                isFinished = isFinished
            )
        }
    }
}
