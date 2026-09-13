package com.example.ui.screens.search

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.example.data.model.ChapterRangeFilter
import com.example.data.model.MangaItem
import com.example.data.model.MangaStatusFilter
import com.example.data.model.SearchFilter
import com.example.data.model.SortOption
import com.example.data.model.YearRangeFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onMangaClick: (Int) -> Unit,
    onGoToDownloads: () -> Unit = {}
) {
    val context = LocalContext.current
    val query by viewModel.query.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Input Field & Filter / Dice Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_text_input"),
                        placeholder = { Text("Search Manga") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onQueryChange("") },
                                    modifier = Modifier.testTag("clear_search_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    // Filter Button with Active Count Badge
                    FilledTonalIconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("open_filter_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (filter.activeFilterCount > 0)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        BadgedBox(
                            badge = {
                                if (filter.activeFilterCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    ) {
                                        Text("${filter.activeFilterCount}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Open Filters",
                                tint = if (filter.activeFilterCount > 0)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Quick Active Filter Bar or Quick Genre Pills
                ActiveFilterHorizontalBar(
                    filter = filter,
                    genresList = viewModel.genresList,
                    onOpenFilterSheet = { showFilterSheet = true },
                    onCycleGenre = { viewModel.cycleGenreState(it) },
                    onRemoveGenre = { viewModel.removeGenreFilter(it) },
                    onResetStatus = { viewModel.setStatusFilter(MangaStatusFilter.ALL) },
                    onResetYear = { viewModel.setYearRangeFilter(YearRangeFilter.ALL) },
                    onResetChapter = { viewModel.setChapterRangeFilter(ChapterRangeFilter.ALL) },
                    onResetAll = { viewModel.resetFilters() },
                    getGenreState = { viewModel.getGenreState(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is SearchUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is SearchUiState.Error -> {
                    val isConnected = com.example.utils.NetworkUtils.isNetworkAvailable(context)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .padding(16.dp)
                        ) {
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
                                    text = if (isConnected) "Search Server Issue" else "You are Offline",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = if (isConnected) "Search servers took too long to respond. Tap Retry to reconnect or search your downloads." else "Internet connection is required to search online. Check your downloaded manga offline!",
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
                                        .testTag("search_go_to_downloads_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Go to Downloads", fontWeight = FontWeight.Bold)
                                }

                                if (isConnected) {
                                    OutlinedButton(
                                        onClick = { viewModel.performSearch(viewModel.query.value, viewModel.filter.value) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("search_retry_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Retry Search")
                                    }
                                }
                            }
                        }
                    }
                }
                is SearchUiState.Success -> {
                    if (state.results.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "No manga found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (filter.activeFilterCount > 0)
                                        "No titles matched your active filters. Try adjusting your tags or clearing exclusions."
                                    else
                                        "Try searching for a different title or exploring popular genres.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                if (filter.activeFilterCount > 0) {
                                    FilledTonalButton(
                                        onClick = { viewModel.resetFilters() },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("clear_all_filters_empty_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Reset All Filters")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 105.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${state.results.size} Titles Found",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Sorted by ${filter.sortBy.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            items(
                                items = state.results,
                                key = { it.id },
                                contentType = { "manga_grid_card" }
                            ) { manga ->
                                SearchMangaGridCard(
                                    manga = manga,
                                    onClick = { onMangaClick(manga.id) }
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // Advanced Multi-Tag Search Filter Modal Bottom Sheet
    if (showFilterSheet) {
        SearchFilterBottomSheet(
            currentFilter = filter,
            genresList = viewModel.genresList,
            getGenreState = { viewModel.getGenreState(it) },
            onCycleGenre = { viewModel.cycleGenreState(it) },
            onStatusChange = { viewModel.setStatusFilter(it) },
            onYearChange = { viewModel.setYearRangeFilter(it) },
            onChapterChange = { viewModel.setChapterRangeFilter(it) },
            onSortChange = { viewModel.setSortOption(it) },
            onResetAll = { viewModel.resetFilters() },
            onDismiss = { showFilterSheet = false }
        )
    }
}

/**
 * Top Quick Filter Horizontal Bar
 */
@Composable
fun ActiveFilterHorizontalBar(
    filter: SearchFilter,
    genresList: List<String>,
    onOpenFilterSheet: () -> Unit,
    onCycleGenre: (String) -> Unit,
    onRemoveGenre: (String) -> Unit,
    onResetStatus: () -> Unit,
    onResetYear: () -> Unit,
    onResetChapter: () -> Unit,
    onResetAll: () -> Unit,
    getGenreState: (String) -> TagFilterState
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. Filter Trigger Pill
        item {
            FilterChip(
                selected = filter.activeFilterCount > 0,
                onClick = onOpenFilterSheet,
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(if (filter.activeFilterCount > 0) "Filters (${filter.activeFilterCount})" else "All Filters")
                    }
                },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("quick_filter_tray_button")
            )
        }

        // 2. Clear All Pill if active
        if (filter.activeFilterCount > 0) {
            item {
                Surface(
                    onClick = onResetAll,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    modifier = Modifier.testTag("quick_clear_all_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear All",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Clear All",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // 3. Active Included Genres (+Genre)
        items(filter.includedGenres.toList()) { genre ->
            ActiveTagChip(
                text = "+$genre",
                isInclude = true,
                onRemove = { onRemoveGenre(genre) }
            )
        }

        // 4. Active Excluded Genres (-Genre)
        items(filter.excludedGenres.toList()) { genre ->
            ActiveTagChip(
                text = "-$genre",
                isInclude = false,
                onRemove = { onRemoveGenre(genre) }
            )
        }

        // 5. Active Status Filter
        if (filter.status != MangaStatusFilter.ALL) {
            item {
                ActiveTagChip(
                    text = filter.status.label,
                    isInclude = true,
                    onRemove = onResetStatus
                )
            }
        }

        // 6. Active Year Filter
        if (filter.releaseYearRange != YearRangeFilter.ALL) {
            item {
                ActiveTagChip(
                    text = filter.releaseYearRange.label,
                    isInclude = true,
                    onRemove = onResetYear
                )
            }
        }

        // 7. Active Chapter Range Filter
        if (filter.chapterRange != ChapterRangeFilter.ALL) {
            item {
                ActiveTagChip(
                    text = filter.chapterRange.label,
                    isInclude = true,
                    onRemove = onResetChapter
                )
            }
        }

        // 8. Inactive genres for quick one-tap cycling if no filters active
        if (filter.activeFilterCount == 0) {
            items(genresList) { genre ->
                FilterChip(
                    selected = false,
                    onClick = { onCycleGenre(genre) },
                    label = { Text(genre) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("genre_chip_$genre")
                )
            }
        }
    }
}

/**
 * Compact Chip for active search filters with (X) removal
 */
@Composable
fun ActiveTagChip(
    text: String,
    isInclude: Boolean,
    onRemove: () -> Unit
) {
    val bgColor = if (isInclude)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.errorContainer

    val textColor = if (isInclude)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onErrorContainer

    Surface(
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier.testTag("active_chip_$text")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clickable { onRemove() }
                .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove $text",
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Comprehensive Multi-Tag & Search Filter Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterBottomSheet(
    currentFilter: SearchFilter,
    genresList: List<String>,
    getGenreState: (String) -> TagFilterState,
    onCycleGenre: (String) -> Unit,
    onStatusChange: (MangaStatusFilter) -> Unit,
    onYearChange: (YearRangeFilter) -> Unit,
    onChapterChange: (ChapterRangeFilter) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Filter Manga",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (currentFilter.activeFilterCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "${currentFilter.activeFilterCount}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onResetAll,
                    modifier = Modifier.testTag("filter_sheet_reset_button")
                ) {
                    Text(
                        "Reset All",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Scrollable Filters Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // SECTION 1: Multi-Tag Genres (Include / Exclude)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Genres & Tags",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap: (+) Include • (-) Exclude",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Tri-state Genre Chips Matrix
                        OptInGenreFlowRow(
                            genres = genresList,
                            getGenreState = getGenreState,
                            onGenreClick = onCycleGenre
                        )
                    }
                }

                // SECTION 2: Publication Status
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Publication Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(MangaStatusFilter.values()) { status ->
                                val isSelected = currentFilter.status == status
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onStatusChange(status) },
                                    label = { Text(status.label) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.testTag("status_filter_${status.name}")
                                )
                            }
                        }
                    }
                }

                // SECTION 3: Release Year
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Release Year",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(YearRangeFilter.values()) { yearRange ->
                                val isSelected = currentFilter.releaseYearRange == yearRange
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onYearChange(yearRange) },
                                    label = { Text(yearRange.label) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.testTag("year_filter_${yearRange.name}")
                                )
                            }
                        }
                    }
                }

                // SECTION 4: Chapter Count / Length
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Chapter Count & Length",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ChapterRangeFilter.values()) { chapterRange ->
                                val isSelected = currentFilter.chapterRange == chapterRange
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onChapterChange(chapterRange) },
                                    label = { Text(chapterRange.label) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.testTag("chapter_filter_${chapterRange.name}")
                                )
                            }
                        }
                    }
                }

                // SECTION 5: Sort Order
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Sort Results By",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(SortOption.values()) { sortOption ->
                                val isSelected = currentFilter.sortBy == sortOption
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSortChange(sortOption) },
                                    label = { Text(sortOption.label) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.testTag("sort_filter_${sortOption.name}")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Apply Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("filter_sheet_apply_button")
                ) {
                    Text(
                        text = if (currentFilter.activeFilterCount > 0)
                            "Apply Filters (${currentFilter.activeFilterCount})"
                        else
                            "Show Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Grid layout of Tri-State genre tags (Neutral, Included (+), Excluded (-))
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OptInGenreFlowRow(
    genres: List<String>,
    getGenreState: (String) -> TagFilterState,
    onGenreClick: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        genres.forEach { genre ->
            val state = getGenreState(genre)
            TriStateGenreChip(
                genre = genre,
                state = state,
                onClick = { onGenreClick(genre) }
            )
        }
    }
}

/**
 * A sleek Tri-State chip that shows:
 * - Neutral: SurfaceContainer, grey text
 * - Included (+): PrimaryContainer / Green tint, + icon, high contrast
 * - Excluded (-): ErrorContainer / Red tint, - icon, strike-through look
 */
@Composable
fun TriStateGenreChip(
    genre: String,
    state: TagFilterState,
    onClick: () -> Unit
) {
    val containerColor = when (state) {
        TagFilterState.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainer
        TagFilterState.INCLUDED -> MaterialTheme.colorScheme.primaryContainer
        TagFilterState.EXCLUDED -> MaterialTheme.colorScheme.errorContainer
    }

    val contentColor = when (state) {
        TagFilterState.NEUTRAL -> MaterialTheme.colorScheme.onSurface
        TagFilterState.INCLUDED -> MaterialTheme.colorScheme.onPrimaryContainer
        TagFilterState.EXCLUDED -> MaterialTheme.colorScheme.onErrorContainer
    }

    val borderColor = when (state) {
        TagFilterState.NEUTRAL -> Color.Transparent
        TagFilterState.INCLUDED -> MaterialTheme.colorScheme.primary
        TagFilterState.EXCLUDED -> MaterialTheme.colorScheme.error
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = if (state != TagFilterState.NEUTRAL)
            androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
        else null,
        modifier = Modifier.testTag("tri_state_genre_$genre")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            when (state) {
                TagFilterState.INCLUDED -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Included",
                        tint = contentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                TagFilterState.EXCLUDED -> {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Excluded",
                        tint = contentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                TagFilterState.NEUTRAL -> {
                    // No leading icon for clean scanning
                }
            }

            Text(
                text = when (state) {
                    TagFilterState.INCLUDED -> "+ $genre"
                    TagFilterState.EXCLUDED -> "- $genre"
                    TagFilterState.NEUTRAL -> genre
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (state != TagFilterState.NEUTRAL) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
fun SearchMangaGridCard(
    manga: MangaItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("search_result_${manga.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                AsyncImage(
                    model = manga.coverImage,
                    contentDescription = manga.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Score badge
                manga.score?.let { score ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD166),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${score / 10.0}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Year badge
                manga.startYear?.let { year ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "$year",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = manga.displayTitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = manga.genres.firstOrNull() ?: manga.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f)
                    )
                    manga.chapters?.let { ch ->
                        Text(
                            text = "${ch} ch",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

