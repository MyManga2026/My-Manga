package com.example.ui.screens.reader

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MangaProvider
import com.example.data.model.ReaderBackground
import com.example.data.model.ReaderContentScale
import com.example.data.model.ReaderMode
import com.example.data.model.ReadingDirection
import com.example.ui.theme.SepiaBackground
import com.example.ui.theme.SepiaText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showControls by remember { mutableStateOf(false) }
    var showSettingsBottomSheet by remember { mutableStateOf(false) }
    var showJumpToPageDialog by remember { mutableStateOf(false) }
    var showBrightnessQuickBar by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val view = LocalView.current
    val window = remember(context) { (context as? Activity)?.window }

    LaunchedEffect(showControls) {
        window?.let { win ->
            val controller = WindowInsetsControllerCompat(win, view)
            if (!showControls) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            window?.let { win ->
                val controller = WindowInsetsControllerCompat(win, view)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                when (val state = uiState) {
                    is ReaderUiState.Success -> when (state.readerBg) {
                        ReaderBackground.BLACK -> Color.Black
                        ReaderBackground.WHITE -> Color.White
                        ReaderBackground.SEPIA -> SepiaBackground
                    }
                    else -> Color.Black
                }
            )
    ) {
        when (val state = uiState) {
            is ReaderUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is ReaderUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { viewModel.loadChapterPages() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            is ReaderUiState.Success -> {
                val coroutineScope = rememberCoroutineScope()

                val initialPageIndex = remember(state.manga.id, state.currentChapter.id) {
                    state.currentPageIndex.coerceIn(0, (state.pages.size - 1).coerceAtLeast(0))
                }

                val listState = rememberLazyListState(
                    initialFirstVisibleItemIndex = initialPageIndex
                )
                val pagerState = rememberPagerState(
                    initialPage = initialPageIndex,
                    pageCount = { state.pages.size }
                )

                // Track reading active time periodically for analytics
                LaunchedEffect(state.manga.id, state.currentChapter.id) {
                    while (true) {
                        delay(10_000L) // every 10 seconds of active reading
                        com.example.utils.ReadingAnalyticsManager.recordReadingTime(
                            context = context,
                            secondsToAdd = 10L,
                            genreList = state.manga.genres
                        )
                    }
                }

                // Sync scroll position when currentPageIndex or mode changes
                LaunchedEffect(state.currentPageIndex, state.readerMode) {
                    if (state.pages.isNotEmpty()) {
                        val target = state.currentPageIndex.coerceIn(0, state.pages.size - 1)
                        if (state.readerMode == ReaderMode.VERTICAL_CONTINUOUS) {
                            if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != target) {
                                listState.scrollToItem(target)
                            }
                        } else {
                            if (!pagerState.isScrollInProgress && pagerState.currentPage != target) {
                                pagerState.scrollToPage(target)
                            }
                        }
                    }
                }



                val composeContentScale = when (state.contentScaleMode) {
                    ReaderContentScale.FIT_WIDTH -> ContentScale.FillWidth
                    ReaderContentScale.FIT_HEIGHT -> ContentScale.FillHeight
                    ReaderContentScale.FIT_SCREEN -> ContentScale.Fit
                }

                // Reader Body according to selected Mode
                if (state.readerMode == ReaderMode.VERTICAL_CONTINUOUS) {
                    // Update page progress as user scrolls
                    LaunchedEffect(listState.firstVisibleItemIndex) {
                        if (listState.isScrollInProgress) {
                            viewModel.onPageChanged(listState.firstVisibleItemIndex)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showControls = !showControls
                            },
                        verticalArrangement = Arrangement.Top
                    ) {
                        itemsIndexed(
                            items = state.pages,
                            key = { _, page -> page.imageUrl },
                            contentType = { _, _ -> "reader_page" }
                        ) { index, page ->
                            val imgRequest = remember(page.imageUrl) {
                                ImageRequest.Builder(context)
                                    .data(page.imageUrl)
                                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                                    .addHeader("Referer", "https://mangapill.com/")
                                    .crossfade(true)
                                    .build()
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = imgRequest,
                                    contentDescription = "Page ${page.pageNumber}",
                                    contentScale = composeContentScale,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                )
                            }
                        }
                    }
                } else {
                    // HORIZONTAL PAGED
                    LaunchedEffect(pagerState.currentPage) {
                        if (pagerState.isScrollInProgress) {
                            val targetIndex = if (state.readingDirection == ReadingDirection.RIGHT_TO_LEFT) {
                                (state.pages.size - 1) - pagerState.currentPage
                            } else {
                                pagerState.currentPage
                            }
                            viewModel.onPageChanged(targetIndex.coerceIn(0, (state.pages.size - 1).coerceAtLeast(0)))
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        key = { pageIndex -> state.pages.getOrNull(pageIndex)?.imageUrl ?: pageIndex },
                        reverseLayout = state.readingDirection == ReadingDirection.RIGHT_TO_LEFT,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showControls = !showControls
                            }
                    ) { pageIndex ->
                        val page = state.pages.getOrNull(pageIndex)
                        if (page != null) {
                            val imgRequest = remember(page.imageUrl) {
                                ImageRequest.Builder(context)
                                    .data(page.imageUrl)
                                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                                    .addHeader("Referer", "https://mangapill.com/")
                                    .crossfade(true)
                                    .build()
                            }

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = imgRequest,
                                    contentDescription = "Page ${page.pageNumber}",
                                    contentScale = composeContentScale,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Brightness Dimming Overlay
                if (state.readerBrightness < 0.98f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = (1.0f - state.readerBrightness).coerceIn(0f, 0.85f)))
                    )
                }



                // Floating Page Badge (when controls hidden)
                AnimatedVisibility(
                    visible = !showControls && state.pages.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "${state.currentPageIndex + 1} / ${state.pages.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Top Toolbar Overlay
                AnimatedVisibility(
                    visible = showControls,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.88f),
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.testTag("reader_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = state.manga.displayTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    color = Color.White
                                )
                                Text(
                                    text = "${state.currentChapter.title} • ${state.selectedProvider.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Quick Mode Switcher Button
                                IconButton(
                                    onClick = {
                                        val newMode = if (state.readerMode == ReaderMode.VERTICAL_CONTINUOUS)
                                            ReaderMode.HORIZONTAL_PAGED else ReaderMode.VERTICAL_CONTINUOUS
                                        viewModel.setReaderMode(newMode)
                                    },
                                    modifier = Modifier.testTag("reader_quick_mode_button")
                                ) {
                                    Icon(
                                        imageVector = if (state.readerMode == ReaderMode.VERTICAL_CONTINUOUS)
                                            Icons.Default.SwapVert else Icons.Default.SwapHoriz,
                                        contentDescription = "Toggle Reader Mode",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Bookmark Button
                                IconButton(
                                    onClick = { viewModel.toggleBookmark() },
                                    modifier = Modifier.testTag("reader_bookmark_button")
                                ) {
                                    Icon(
                                        imageVector = if (state.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (state.isBookmarked) MaterialTheme.colorScheme.primary else Color.White
                                    )
                                }

                                // Preferences Gear Button
                                IconButton(
                                    onClick = { showSettingsBottomSheet = true },
                                    modifier = Modifier.testTag("reader_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Reader Settings",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Control Bar Overlay with Custom Reader Controls
                AnimatedVisibility(
                    visible = showControls,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.88f),
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Quick Brightness Adjustment Bar
                            if (showBrightnessQuickBar) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BrightnessLow,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Slider(
                                        value = state.readerBrightness,
                                        onValueChange = { viewModel.setReaderBrightness(it) },
                                        valueRange = 0.15f..1.0f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Icon(
                                        imageVector = Icons.Default.BrightnessHigh,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Page Slider Row
                            if (state.pages.size > 1) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "${state.currentPageIndex + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Slider(
                                        value = state.currentPageIndex.toFloat().coerceIn(0f, (state.pages.size - 1).coerceAtLeast(0).toFloat()),
                                        onValueChange = { newValue ->
                                            val targetPage = newValue.toInt().coerceIn(0, (state.pages.size - 1).coerceAtLeast(0))
                                            viewModel.onPageChanged(targetPage)
                                            coroutineScope.launch {
                                                if (state.readerMode == ReaderMode.VERTICAL_CONTINUOUS) {
                                                    listState.scrollToItem(targetPage)
                                                } else {
                                                    pagerState.scrollToPage(targetPage)
                                                }
                                            }
                                        },
                                        valueRange = 0f..(state.pages.size - 1).coerceAtLeast(1).toFloat(),
                                        steps = (state.pages.size - 2).coerceAtLeast(0),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("page_slider"),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                        )
                                    )

                                    Text(
                                        text = "${state.pages.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )

                                    // Quick Jump Button
                                    IconButton(
                                        onClick = { showJumpToPageDialog = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GridOn,
                                            contentDescription = "Jump To Page",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Prev / Next Chapter Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.navigateChapter(next = false) },
                                    modifier = Modifier.testTag("prev_chapter_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NavigateBefore,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Prev Ch", color = Color.White)
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Page ${state.currentPageIndex + 1} of ${state.pages.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                TextButton(
                                    onClick = { viewModel.navigateChapter(next = true) },
                                    modifier = Modifier.testTag("next_chapter_button")
                                ) {
                                    Text("Next Ch", color = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.NavigateNext,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Jump To Page Grid Dialog
                if (showJumpToPageDialog) {
                    AlertDialog(
                        onDismissRequest = { showJumpToPageDialog = false },
                        title = {
                            Text(
                                text = "Jump To Page (Total: ${state.pages.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Box(modifier = Modifier.heightIn(max = 300.dp)) {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 52.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(state.pages) { page ->
                                        val isCurrent = page.pageNumber - 1 == state.currentPageIndex
                                        Surface(
                                            onClick = {
                                                val targetPage = page.pageNumber - 1
                                                viewModel.onPageChanged(targetPage)
                                                coroutineScope.launch {
                                                    if (state.readerMode == ReaderMode.VERTICAL_CONTINUOUS) {
                                                        listState.scrollToItem(targetPage)
                                                    } else {
                                                        pagerState.scrollToPage(targetPage)
                                                    }
                                                }
                                                showJumpToPageDialog = false
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${page.pageNumber}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showJumpToPageDialog = false }) {
                                Text("Close")
                            }
                        }
                    )
                }

                // Reader Settings Bottom Sheet Modal
                if (showSettingsBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showSettingsBottomSheet = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text(
                                text = "Custom Reader Controls & Preferences",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Reading Mode & Direction
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Reading Format & Direction",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FilterChip(
                                        selected = state.readerMode == ReaderMode.VERTICAL_CONTINUOUS,
                                        onClick = { viewModel.setReaderMode(ReaderMode.VERTICAL_CONTINUOUS) },
                                        label = { Text("Webtoon Vertical") },
                                        leadingIcon = { Icon(imageVector = Icons.Default.SwapVert, contentDescription = null) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("mode_vertical_chip")
                                    )

                                    FilterChip(
                                        selected = state.readerMode == ReaderMode.HORIZONTAL_PAGED,
                                        onClick = { viewModel.setReaderMode(ReaderMode.HORIZONTAL_PAGED) },
                                        label = { Text("Paged Horizontal") },
                                        leadingIcon = { Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("mode_horizontal_chip")
                                    )
                                }

                                if (state.readerMode == ReaderMode.HORIZONTAL_PAGED) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        FilterChip(
                                            selected = state.readingDirection == ReadingDirection.LEFT_TO_RIGHT,
                                            onClick = { viewModel.setReadingDirection(ReadingDirection.LEFT_TO_RIGHT) },
                                            label = { Text("LTR (Standard)") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = state.readingDirection == ReadingDirection.RIGHT_TO_LEFT,
                                            onClick = { viewModel.setReadingDirection(ReadingDirection.RIGHT_TO_LEFT) },
                                            label = { Text("RTL (Manga)") },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }



                            // Content Scaling Mode
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Image Fit & Scale",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FilterChip(
                                        selected = state.contentScaleMode == ReaderContentScale.FIT_WIDTH,
                                        onClick = { viewModel.setContentScaleMode(ReaderContentScale.FIT_WIDTH) },
                                        label = { Text("Fit Width") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = state.contentScaleMode == ReaderContentScale.FIT_HEIGHT,
                                        onClick = { viewModel.setContentScaleMode(ReaderContentScale.FIT_HEIGHT) },
                                        label = { Text("Fit Height") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = state.contentScaleMode == ReaderContentScale.FIT_SCREEN,
                                        onClick = { viewModel.setContentScaleMode(ReaderContentScale.FIT_SCREEN) },
                                        label = { Text("Fit Screen") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Reader Brightness Slider
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Screen Brightness",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${(state.readerBrightness * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Slider(
                                    value = state.readerBrightness,
                                    onValueChange = { viewModel.setReaderBrightness(it) },
                                    valueRange = 0.15f..1.0f,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            // Background Color Selection
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Reader Background Theme",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FilterChip(
                                        selected = state.readerBg == ReaderBackground.BLACK,
                                        onClick = { viewModel.setReaderBackground(ReaderBackground.BLACK) },
                                        label = { Text("Dark Black") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("bg_black_chip")
                                    )

                                    FilterChip(
                                        selected = state.readerBg == ReaderBackground.SEPIA,
                                        onClick = { viewModel.setReaderBackground(ReaderBackground.SEPIA) },
                                        label = { Text("Sepia Warm") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("bg_sepia_chip")
                                    )

                                    FilterChip(
                                        selected = state.readerBg == ReaderBackground.WHITE,
                                        onClick = { viewModel.setReaderBackground(ReaderBackground.WHITE) },
                                        label = { Text("White Light") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("bg_white_chip")
                                    )
                                }
                            }

                            // Manga Provider Selection
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Manga Source Provider",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val providers = listOf(
                                    MangaProvider.MANGAPILL,
                                    MangaProvider.WEEBCENTRAL,
                                    MangaProvider.MANGAFIRE,
                                    MangaProvider.MANGADEX
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    providers.take(2).forEach { provider ->
                                        FilterChip(
                                            selected = state.selectedProvider == provider,
                                            onClick = {
                                                viewModel.setProvider(provider)
                                                showSettingsBottomSheet = false
                                            },
                                            label = { Text(provider.displayName) },
                                            leadingIcon = if (state.selectedProvider == provider) {
                                                { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("provider_${provider.id}_chip")
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    providers.drop(2).forEach { provider ->
                                        FilterChip(
                                            selected = state.selectedProvider == provider,
                                            onClick = {
                                                viewModel.setProvider(provider)
                                                showSettingsBottomSheet = false
                                            },
                                            label = { Text(provider.displayName) },
                                            leadingIcon = if (state.selectedProvider == provider) {
                                                { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("provider_${provider.id}_chip")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
