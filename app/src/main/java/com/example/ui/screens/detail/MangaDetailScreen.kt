package com.example.ui.screens.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChapterItem
import com.example.data.model.MangaItem

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailScreen(
    viewModel: MangaDetailViewModel,
    onBackClick: () -> Unit,
    onChapterClick: (MangaItem, ChapterItem) -> Unit,
    onGoToDownloads: () -> Unit = {},
    onMangaClick: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.syncReadStatusFromPreferences(context)
    }

    val isSelectionMode = (uiState as? MangaDetailUiState.Success)?.isSelectionMode == true
    BackHandler(enabled = isSelectionMode) {
        viewModel.exitSelectionMode()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is MangaDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is MangaDetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .padding(16.dp)
                    ) {
                        val isConnected = com.example.utils.NetworkUtils.isNetworkAvailable(context)
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isConnected) Icons.Default.Refresh else Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Text(
                                text = if (isConnected) "Unable to Load Details" else "You are Offline",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = if (isConnected) "Server took too long to respond. Tap Retry to reconnect or read your downloads." else "No internet connection detected. You can still read your downloaded chapters offline!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = onGoToDownloads,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_go_to_downloads_button")
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Go to Downloads", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.loadDetailData() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_retry_button")
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry Connection")
                            }
                        }
                    }
                }
            }
            is MangaDetailUiState.Success -> {
                val context = LocalContext.current
                var isSynopsisExpanded by remember { mutableStateOf(false) }
                var showAllChapters by rememberSaveable(state.manga.id) { mutableStateOf(false) }
                val primaryColor = MaterialTheme.colorScheme.primary

                val shouldLimitChapters = !showAllChapters && state.filteredChapters.size > 15
                val displayedChapters = remember(state.filteredChapters, shouldLimitChapters) {
                    if (shouldLimitChapters) {
                        state.filteredChapters.take(15)
                    } else {
                        state.filteredChapters
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Item 1: Hero Banner Header
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        ) {
                            AsyncImage(
                                model = state.manga.bannerImage ?: state.manga.coverImage,
                                contentDescription = state.manga.displayTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.35f),
                                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                            )

                            // Top Back & Bookmark Buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    IconButton(
                                        onClick = onBackClick,
                                        modifier = Modifier.testTag("detail_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = Color.White
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.toggleBookmark() },
                                            modifier = Modifier.testTag("bookmark_toggle_button")
                                        ) {
                                            Icon(
                                                imageVector = if (state.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = "Bookmark",
                                                tint = if (state.isBookmarked) primaryColor else Color.White
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        IconButton(
                                            onClick = { shareManga(context, state.manga) },
                                            modifier = Modifier.testTag("share_manga_top_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share Manga",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Poster and Primary Info
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                AsyncImage(
                                    model = state.manga.coverImage,
                                    contentDescription = state.manga.displayTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 100.dp, height = 145.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Text(
                                        text = state.manga.displayTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        state.manga.score?.let { score ->
                                            Surface(
                                                color = Color(0xFFFFD166).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = Color(0xFFFFD166),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "${score / 10.0}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFFD166)
                                                    )
                                                }
                                            }
                                        }

                                        Surface(
                                            color = primaryColor.copy(alpha = 0.22f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = state.manga.status,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = primaryColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    state.manga.startYear?.let { year ->
                                        Text(
                                            text = "Released $year • ${state.manga.format ?: "Manga"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Item 2: Metadata Badges Grid & Genres
                    item {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Genres chips
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                state.manga.genres.forEach { genre ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = genre,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            // Synopsis Collapsible Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Synopsis",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = state.manga.description.ifEmpty { "No description available." },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isSynopsisExpanded) "Show less" else "Read more",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = primaryColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Item 3: Chapter Section Header & Search / Sort / Batch controls
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (state.isSelectionMode) "Selected (${state.selectedChapterIds.size}/${state.filteredChapters.size})" else "Chapters (${state.filteredChapters.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.isSelectionMode) primaryColor else MaterialTheme.colorScheme.onBackground
                                    )
                                    if (shouldLimitChapters && !state.isSelectionMode) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = primaryColor.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "Showing 15",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = primaryColor,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (state.isSelectionMode) {
                                                viewModel.exitSelectionMode()
                                            } else {
                                                viewModel.enterSelectionMode()
                                            }
                                        },
                                        modifier = Modifier.testTag("multi_select_chapters_button")
                                    ) {
                                        Icon(
                                            imageVector = if (state.isSelectionMode) Icons.Default.Close else Icons.Default.Checklist,
                                            contentDescription = if (state.isSelectionMode) "Cancel Selection" else "Multi-Select Chapters",
                                            tint = if (state.isSelectionMode) MaterialTheme.colorScheme.error else primaryColor
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.toggleSortOrder() },
                                        modifier = Modifier.testTag("sort_chapters_button")
                                    ) {
                                        Icon(
                                            imageVector = if (state.isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            contentDescription = "Sort",
                                            tint = primaryColor
                                        )
                                    }
                                }
                            }

                            // Quick Batch Action Chips (Always accessible or when selecting)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    AssistChip(
                                        onClick = {
                                            viewModel.downloadNextChaptersDirectly(context, 5)
                                            Toast.makeText(context, "Queued next 5 chapters for download", Toast.LENGTH_SHORT).show()
                                        },
                                        label = { Text("Download Next 5") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = primaryColor
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = primaryColor.copy(alpha = 0.12f),
                                            labelColor = primaryColor
                                        ),
                                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
                                        modifier = Modifier.testTag("quick_download_next_5_chip")
                                    )
                                }
                                item {
                                    AssistChip(
                                        onClick = {
                                            viewModel.downloadUnreadChaptersDirectly(context)
                                            Toast.makeText(context, "Queued unread chapters for download", Toast.LENGTH_SHORT).show()
                                        },
                                        label = { Text("Download Unread") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.AutoStories,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = primaryColor
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                                        ),
                                        modifier = Modifier.testTag("quick_download_unread_chip")
                                    )
                                }
                                item {
                                    AssistChip(
                                        onClick = {
                                            if (state.isSelectionMode) {
                                                if (state.selectedChapterIds.size == state.filteredChapters.size) {
                                                    viewModel.clearSelection()
                                                } else {
                                                    viewModel.selectAllChapters()
                                                }
                                            } else {
                                                viewModel.selectAllChapters()
                                            }
                                        },
                                        label = {
                                            Text(
                                                if (state.isSelectionMode && state.selectedChapterIds.size == state.filteredChapters.size)
                                                    "Deselect All"
                                                else "Select All"
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.SelectAll,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                                        )
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = state.chapterSearchQuery,
                                onValueChange = { viewModel.filterChapters(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chapter_search_input"),
                                placeholder = { Text("Filter chapter number or title...") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                        }
                    }

                    // Item 4: Chapter Items
                    items(
                        items = if (state.isSelectionMode) state.filteredChapters else displayedChapters,
                        key = { it.id },
                        contentType = { "chapter_item" }
                    ) { chapter ->
                        val isDownloaded = state.downloadedChapterIds.contains(chapter.id)
                        val isDownloading = state.downloadingChapterId == chapter.id
                        val isQueued = state.downloadQueue.contains(chapter.id) && !isDownloading
                        val downloadProgress = state.activeDownloadsProgress[chapter.id]
                        val isRead = state.readChapterIds.contains(chapter.id)
                        val isSelected = state.selectedChapterIds.contains(chapter.id)

                        ChapterListItem(
                            chapter = chapter,
                            isLastRead = state.readingHistory?.chapterId == chapter.id,
                            isRead = isRead,
                            lastReadPage = if (state.readingHistory?.chapterId == chapter.id) state.readingHistory.pageNumber else null,
                            totalPages = if (state.readingHistory?.chapterId == chapter.id) state.readingHistory.totalPages else null,
                            isDownloaded = isDownloaded,
                            isDownloading = isDownloading,
                            isQueued = isQueued,
                            downloadProgress = downloadProgress,
                            primaryColor = primaryColor,
                            isSelectionMode = state.isSelectionMode,
                            isSelected = isSelected,
                            onDownloadClick = { viewModel.downloadChapter(context, chapter) },
                            onDeleteDownloadClick = { viewModel.deleteDownload(context, chapter) },
                            onClick = { onChapterClick(state.manga, chapter) },
                            onLongClick = {
                                if (!state.isSelectionMode) {
                                    viewModel.enterSelectionMode(chapter.id)
                                } else {
                                    viewModel.toggleChapterSelection(chapter.id)
                                }
                            },
                            onToggleSelect = {
                                viewModel.toggleChapterSelection(chapter.id)
                            }
                        )
                    }

                    // Item 4b: Show All Chapters button with fading background overlay
                    if (shouldLimitChapters && !state.isSelectionMode) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.background.copy(alpha = 0.0f),
                                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = { showAllChapters = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("show_all_chapters_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = primaryColor.copy(alpha = 0.12f),
                                        contentColor = primaryColor
                                    ),
                                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 1.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Show All Chapters (${state.filteredChapters.size})",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryColor
                                        )
                                    }
                                }
                            }
                        }
                    } else if (state.filteredChapters.size > 15 && showAllChapters && !state.isSelectionMode) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = { showAllChapters = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .testTag("collapse_chapters_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Show Less (Collapse to 15)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Item 5: Manga Recommendations Section (Horizontal Slide Right Side)
                    if (state.recommendations.isNotEmpty() || state.isLoadingRecommendations) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 28.dp, bottom = 24.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = primaryColor.copy(alpha = 0.15f),
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = primaryColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = "Recommended Manga",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = "Readers also enjoyed these titles",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "Slide",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (state.isLoadingRecommendations && state.recommendations.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(30.dp),
                                            strokeWidth = 2.5.dp,
                                            color = primaryColor
                                        )
                                    }
                                } else {
                                    LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("recommended_manga_carousel"),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        items(
                                            items = state.recommendations,
                                            key = { "rec_${it.id}" }
                                        ) { recManga ->
                                            RecommendationMangaCard(
                                                manga = recManga,
                                                primaryColor = primaryColor,
                                                onClick = { onMangaClick(recManga.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Action Button for Start Reading / Resume Reading (Hidden during Selection Mode)
                if (!state.isSelectionMode) {
                    val targetChapter = remember(state) {
                        if (state.readingHistory != null) {
                            state.chapters.find { it.id == state.readingHistory.chapterId } ?: state.chapters.firstOrNull()
                        } else {
                            state.chapters.lastOrNull() ?: state.chapters.firstOrNull()
                        }
                    }

                    targetChapter?.let { ch ->
                        val historyPageText = if (state.readingHistory != null && state.readingHistory.chapterId == ch.id && state.readingHistory.pageNumber > 1) {
                            " (Pg ${state.readingHistory.pageNumber}/${state.readingHistory.totalPages})"
                        } else ""

                        ExtendedFloatingActionButton(
                            onClick = { onChapterClick(state.manga, ch) },
                            icon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null) },
                            text = {
                                Text(
                                    text = if (state.readingHistory != null) "Resume Ch ${ch.chapterNumber}$historyPageText" else "Start Ch 1",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            containerColor = primaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .testTag("start_reading_fab")
                        )
                    }
                }

                // Docked Batch Actions Bottom Bar (Visible during Selection Mode)
                AnimatedVisibility(
                    visible = state.isSelectionMode,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = primaryColor.copy(alpha = 0.15f),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${state.selectedChapterIds.size}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = primaryColor
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Chapters Selected",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                TextButton(
                                    onClick = { viewModel.exitSelectionMode() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Done", color = primaryColor, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Preset Quick Selection Chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    AssistChip(
                                        onClick = { viewModel.selectNextChapters(5) },
                                        label = { Text("Next 5 Ch") },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                                        )
                                    )
                                }
                                item {
                                    AssistChip(
                                        onClick = { viewModel.selectNextChapters(10) },
                                        label = { Text("Next 10 Ch") },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                                        )
                                    )
                                }
                                item {
                                    AssistChip(
                                        onClick = { viewModel.selectUnreadChapters() },
                                        label = { Text("All Unread") },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                                        )
                                    )
                                }
                                item {
                                    AssistChip(
                                        onClick = {
                                            if (state.selectedChapterIds.size == state.filteredChapters.size) {
                                                viewModel.clearSelection()
                                            } else {
                                                viewModel.selectAllChapters()
                                            }
                                        },
                                        label = { Text(if (state.selectedChapterIds.size == state.filteredChapters.size) "Deselect All" else "Select All") },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                                        )
                                    )
                                }
                            }

                            // Primary Batch Action Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Batch Download
                                Button(
                                    onClick = {
                                        val count = state.selectedChapterIds.size
                                        viewModel.downloadSelectedChapters(context)
                                        Toast.makeText(context, "Queued $count chapters for download", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(44.dp)
                                        .testTag("batch_download_button"),
                                    enabled = state.selectedChapterIds.isNotEmpty(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                ) {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download (${state.selectedChapterIds.size})", fontWeight = FontWeight.Bold, maxLines = 1)
                                }

                                // Batch Mark Read
                                FilledTonalButton(
                                    onClick = {
                                        val count = state.selectedChapterIds.size
                                        viewModel.markSelectedAsRead(context)
                                        Toast.makeText(context, "Marked $count chapters as read", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("batch_mark_read_button"),
                                    enabled = state.selectedChapterIds.isNotEmpty(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Read", fontWeight = FontWeight.Bold, maxLines = 1)
                                }

                                // Batch Mark Unread
                                FilledTonalButton(
                                    onClick = {
                                        val count = state.selectedChapterIds.size
                                        viewModel.markSelectedAsUnread(context)
                                        Toast.makeText(context, "Marked $count chapters as unread", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("batch_mark_unread_button"),
                                    enabled = state.selectedChapterIds.isNotEmpty(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.RemoveDone, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Unread", fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChapterListItem(
    chapter: ChapterItem,
    isLastRead: Boolean,
    isRead: Boolean = false,
    lastReadPage: Int? = null,
    totalPages: Int? = null,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isQueued: Boolean = false,
    downloadProgress: Float? = null,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onDownloadClick: () -> Unit,
    onDeleteDownloadClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggleSelect: () -> Unit = {}
) {
    val (displayChapterNumber, displayChapterName) = remember(chapter.title, chapter.chapterNumber) {
        val chNumPart = if (chapter.chapterNumber.isNotBlank()) {
            if (chapter.chapterNumber.startsWith("Ch", ignoreCase = true) || chapter.chapterNumber.startsWith("Chapter", ignoreCase = true)) {
                chapter.chapterNumber
            } else {
                "Chapter ${chapter.chapterNumber}"
            }
        } else {
            chapter.title.substringBefore(":").substringBefore("-").trim()
        }

        var namePart: String? = when {
            chapter.title.contains(":") -> chapter.title.substringAfter(":").trim()
            chapter.title.contains(" - ") -> chapter.title.substringAfter(" - ").trim()
            else -> if (chapter.title.trim() != chNumPart.trim() && chapter.title.trim() != chapter.chapterNumber.trim()) chapter.title.trim() else null
        }

        if (namePart != null) {
            val cleanName = namePart.trim()
            val rawNum = chapter.chapterNumber.trim()
            if (cleanName.equals(chNumPart, ignoreCase = true) ||
                cleanName.equals(rawNum, ignoreCase = true) ||
                cleanName.equals("Chapter $rawNum", ignoreCase = true) ||
                cleanName.isBlank()
            ) {
                namePart = null
            } else {
                namePart = cleanName
            }
        }

        Pair(chNumPart, namePart)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    onLongClick()
                }
            )
            .testTag("chapter_item_${chapter.id}"),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(1.5.dp, primaryColor) else null,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> primaryColor.copy(alpha = 0.20f)
                isLastRead -> primaryColor.copy(alpha = 0.16f)
                isRead -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f)
                else -> MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background fill overlay during download progress
            if (isDownloading && downloadProgress != null && downloadProgress > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .fillMaxWidth(fraction = downloadProgress.coerceIn(0f, 1f))
                        .background(primaryColor.copy(alpha = 0.15f))
                )
            } else if (isQueued) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.10f))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = primaryColor,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = displayChapterNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isLastRead -> primaryColor
                                    isRead -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isRead && !isLastRead) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "READ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                        if (!displayChapterName.isNullOrBlank()) {
                            Text(
                                text = displayChapterName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isLastRead) primaryColor.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        chapter.releaseDate?.let { date ->
                            if (date.isNotBlank()) {
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isLastRead) {
                        Surface(
                            color = primaryColor,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            val badgeText = if (lastReadPage != null && lastReadPage > 0 && totalPages != null && totalPages > 0) {
                                "LAST READ • PG $lastReadPage/$totalPages"
                            } else {
                                "LAST READ"
                            }
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isDownloading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val percent = downloadProgress?.let { (it * 100).toInt() } ?: 0
                            Text(
                                text = "$percent%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.5.dp,
                                color = primaryColor
                            )
                        }
                    } else if (isQueued) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Queued",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else if (isDownloaded) {
                        IconButton(
                            onClick = onDeleteDownloadClick,
                            modifier = Modifier.testTag("delete_download_${chapter.id}")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Downloaded - Tap to remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        IconButton(
                            onClick = onDownloadClick,
                            modifier = Modifier.testTag("download_chapter_${chapter.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Chapter for Offline Reading",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Progress Bar
            if (isDownloading) {
                val prog = downloadProgress ?: 0f
                LinearProgressIndicator(
                    progress = { prog.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = primaryColor,
                    trackColor = primaryColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}

private fun getMangaShareUrl(mangaId: Int): String {
    return "https://apk-downloader.lovable.app/app/manga/mymanga"
}

private fun shareManga(context: Context, manga: MangaItem) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, manga.displayTitle)
        putExtra(
            Intent.EXTRA_TEXT,
            "Check out ${manga.displayTitle} on MyManga -> https://apk-downloader.lovable.app/app/manga/mymanga"
        )
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share ${manga.displayTitle}")
    context.startActivity(shareIntent)
}

private fun copyMangaLink(context: Context, manga: MangaItem) {
    val shareText = "Check out ${manga.displayTitle} on MyManga -> https://apk-downloader.lovable.app/app/manga/mymanga"
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Manga Link", shareText)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
}

@Composable
fun RecommendationMangaCard(
    manga: MangaItem,
    primaryColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(135.dp)
            .height(232.dp)
            .clickable { onClick() }
            .testTag("recommendation_card_${manga.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(162.dp)
            ) {
                AsyncImage(
                    model = manga.coverImage,
                    contentDescription = manga.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                )

                // Score Badge
                manga.score?.let { score ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "$score%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = manga.displayTitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val genreOrStatus = manga.genres.firstOrNull() ?: manga.status
                Text(
                    text = genreOrStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
        }
    }
}
