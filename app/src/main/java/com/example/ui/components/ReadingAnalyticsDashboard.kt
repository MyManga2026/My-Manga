package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.GenreStatsItem
import com.example.utils.ReadingAnalyticsSummary
import com.example.utils.ReadingHeatmapDay

enum class HeatmapViewMode {
    WEEKLY,
    MONTHLY
}

@Composable
fun ReadingAnalyticsDashboard(
    analytics: ReadingAnalyticsSummary,
    modifier: Modifier = Modifier
) {
    var heatmapMode by remember { mutableStateOf(HeatmapViewMode.WEEKLY) }
    var selectedDayDetail by remember { mutableStateOf<ReadingHeatmapDay?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header with Title & Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFFE53935).copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = "Reading Activity & Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Surface(
                color = if (analytics.currentDailyStreak > 0) Color(0xFFE53935).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (analytics.currentDailyStreak > 0) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (analytics.currentDailyStreak > 0) "${analytics.currentDailyStreak} Day Streak" else "0 Day Streak",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (analytics.currentDailyStreak > 0) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1. Primary Metrics Grid (Chapters Read, Time Spent, Current Streak, Best Streak)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricStatCard(
                title = "Chapters Read",
                value = "${analytics.totalChaptersRead}",
                subtitle = if (analytics.totalChaptersRead > 0) "Total read" else "No chapters yet",
                icon = Icons.Default.MenuBook,
                iconTint = Color(0xFF43A047),
                bgGradient = listOf(Color(0xFF43A047).copy(alpha = 0.12f), Color(0xFF43A047).copy(alpha = 0.03f)),
                modifier = Modifier.weight(1f),
                testTag = "stat_chapters_read"
            )
            MetricStatCard(
                title = "Reading Time",
                value = analytics.formattedReadingTime,
                subtitle = if (analytics.totalReadingSeconds > 0) "Active time" else "0m recorded",
                icon = Icons.Default.Schedule,
                iconTint = Color(0xFF1E88E5),
                bgGradient = listOf(Color(0xFF1E88E5).copy(alpha = 0.12f), Color(0xFF1E88E5).copy(alpha = 0.03f)),
                modifier = Modifier.weight(1f),
                testTag = "stat_reading_time"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricStatCard(
                title = "Current Streak",
                value = "${analytics.currentDailyStreak} ${if (analytics.currentDailyStreak == 1) "day" else "days"}",
                subtitle = if (analytics.currentDailyStreak > 0) "Keep reading daily!" else "Start reading today!",
                icon = Icons.Default.LocalFireDepartment,
                iconTint = Color(0xFFFF6B6B),
                bgGradient = listOf(Color(0xFFFF6B6B).copy(alpha = 0.12f), Color(0xFFFF6B6B).copy(alpha = 0.03f)),
                modifier = Modifier.weight(1f),
                testTag = "stat_current_streak"
            )
            MetricStatCard(
                title = "Best Streak",
                value = "${analytics.bestDailyStreak} ${if (analytics.bestDailyStreak == 1) "day" else "days"}",
                subtitle = "All-time record",
                icon = Icons.Default.EmojiEvents,
                iconTint = Color(0xFFFFB300),
                bgGradient = listOf(Color(0xFFFFB300).copy(alpha = 0.12f), Color(0xFFFFB300).copy(alpha = 0.03f)),
                modifier = Modifier.weight(1f),
                testTag = "stat_best_streak"
            )
        }

        // 2. Reading Streak Heatmap Card (Weekly & Monthly Toggle)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Reading Streak Heatmap",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (heatmapMode == HeatmapViewMode.WEEKLY) "Past 7 days activity" else "Past 28 days activity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Segmented Toggle Button
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            HeatmapTabButton(
                                label = "Weekly",
                                isSelected = heatmapMode == HeatmapViewMode.WEEKLY,
                                onClick = {
                                    heatmapMode = HeatmapViewMode.WEEKLY
                                    selectedDayDetail = null
                                },
                                testTag = "heatmap_tab_weekly"
                            )
                            HeatmapTabButton(
                                label = "Monthly",
                                isSelected = heatmapMode == HeatmapViewMode.MONTHLY,
                                onClick = {
                                    heatmapMode = HeatmapViewMode.MONTHLY
                                    selectedDayDetail = null
                                },
                                testTag = "heatmap_tab_monthly"
                            )
                        }
                    }
                }

                // Heatmap Display
                if (heatmapMode == HeatmapViewMode.WEEKLY) {
                    WeeklyHeatmapGrid(
                        days = analytics.weeklyHeatmap,
                        selectedDay = selectedDayDetail,
                        onDayClick = { selectedDayDetail = it }
                    )
                } else {
                    MonthlyHeatmapGrid(
                        days = analytics.monthlyHeatmap,
                        selectedDay = selectedDayDetail,
                        onDayClick = { selectedDayDetail = it }
                    )
                }

                // Selected Day Details Banner (Interactive Tooltip / Expanded Info)
                selectedDayDetail?.let { day ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE53935).copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (day.chaptersReadCount > 0) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (day.chaptersReadCount > 0) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${day.dateKey}${if (day.isToday) " (Today)" else ""}: ",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (day.chaptersReadCount > 0) {
                                        "${day.chaptersReadCount} chs • ~${(day.readingSeconds / 60).coerceAtLeast(1)} mins"
                                    } else {
                                        "No reading activity"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (day.chaptersReadCount > 0) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(
                                onClick = { selectedDayDetail = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Heatmap Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Less",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    HeatmapLegendSquare(color = MaterialTheme.colorScheme.surfaceContainerHighest)
                    Spacer(modifier = Modifier.width(3.dp))
                    HeatmapLegendSquare(color = Color(0xFFE53935).copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.width(3.dp))
                    HeatmapLegendSquare(color = Color(0xFFE53935).copy(alpha = 0.65f))
                    Spacer(modifier = Modifier.width(3.dp))
                    HeatmapLegendSquare(color = Color(0xFFE53935))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "More",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Top Favorite Genres Breakdown
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = Color(0xFFAB47BC),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Top Favorite Genres",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "By frequency",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Genre Progress Bars or Empty State
                if (analytics.topGenres.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoGraph,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "No genre statistics yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Read manga in reader to calculate your genre preferences.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val genreColors = listOf(
                            Color(0xFFE53935), // Red
                            Color(0xFF1E88E5), // Blue
                            Color(0xFF43A047), // Green
                            Color(0xFFFFB300), // Amber
                            Color(0xFF8E24AA)  // Purple
                        )

                        analytics.topGenres.forEachIndexed { index, genreStat ->
                            val barColor = genreColors.getOrElse(index) { MaterialTheme.colorScheme.primary }
                            GenreProgressBarRow(
                                genre = genreStat.genre,
                                count = genreStat.count,
                                percentage = genreStat.percentage,
                                barColor = barColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color,
    bgGradient: List<Color>,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(bgGradient))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = iconTint.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapTabButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFFE53935) else Color.Transparent,
        modifier = Modifier.testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun WeeklyHeatmapGrid(
    days: List<ReadingHeatmapDay>,
    selectedDay: ReadingHeatmapDay?,
    onDayClick: (ReadingHeatmapDay) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEach { day ->
            val isSelected = selectedDay?.dateKey == day.dateKey
            val boxColor = getHeatmapCellColor(day.chaptersReadCount)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onDayClick(day) }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                Text(
                    text = day.dayOfWeek,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (day.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (day.isToday) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(boxColor)
                        .then(
                            if (isSelected || day.isToday) {
                                Modifier.background(
                                    brush = Brush.radialGradient(
                                        listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(5.dp)
                        ) {}
                    } else if (day.chaptersReadCount > 0) {
                        Text(
                            text = "${day.chaptersReadCount}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = "${day.dayOfMonth}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (day.isToday) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthlyHeatmapGrid(
    days: List<ReadingHeatmapDay>,
    selectedDay: ReadingHeatmapDay?,
    onDayClick: (ReadingHeatmapDay) -> Unit
) {
    // 4 Weeks x 7 Days Grid
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Weekday header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { col ->
                Box(
                    modifier = Modifier.width(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = col,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        val weeks = days.chunked(7)
        weeks.forEach { weekDays ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekDays.forEach { day ->
                    val isSelected = selectedDay?.dateKey == day.dateKey
                    val boxColor = getHeatmapCellColor(day.chaptersReadCount)

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(boxColor)
                            .clickable { onDayClick(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(6.dp)
                            ) {}
                        } else if (day.chaptersReadCount > 0) {
                            Text(
                                text = "${day.chaptersReadCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "${day.dayOfMonth}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreProgressBarRow(
    genre: String,
    count: Int,
    percentage: Float,
    barColor: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "genre_prog"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = barColor,
                    modifier = Modifier.size(8.dp)
                ) {}
                Text(
                    text = genre,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "${count} read (${percentage.toInt()}%)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Custom rounded progress track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(barColor.copy(alpha = 0.7f), barColor)
                        )
                    )
            )
        }
    }
}

@Composable
private fun HeatmapLegendSquare(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
    )
}

@Composable
private fun getHeatmapCellColor(count: Int): Color {
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHighest
    return when {
        count <= 0 -> surfaceHigh
        count == 1 -> Color(0xFFE53935).copy(alpha = 0.45f)
        count in 2..3 -> Color(0xFFE53935).copy(alpha = 0.75f)
        else -> Color(0xFFE53935)
    }
}
