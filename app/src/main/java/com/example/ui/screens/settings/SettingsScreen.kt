package com.example.ui.screens.settings

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.MangaRepository
import com.example.utils.AppDataBackupManager
import com.example.utils.AutoBackupScheduler
import com.example.utils.BackupExportResult
import com.example.utils.BackupRestoreResult
import com.example.utils.NotificationHelper
import com.example.utils.NotificationScheduler
import com.example.utils.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: MangaRepository,
    onClearHistorySuccess: () -> Unit,
    onNavigateToOwnerPanel: () -> Unit = {}
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var hasNotificationPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            NotificationHelper.sendNotification(
                context,
                "Notifications Enabled! 🎉",
                "You will now receive new manga chapter releases and reading alerts from MyManga.",
                bypassQuietHours = true
            )
        }
    }

    var newChapterAlertsEnabled by remember { mutableStateOf(true) }
    var dailyStreakRemindersEnabled by remember { mutableStateOf(true) }
    var librarySyncAlertsEnabled by remember { mutableStateOf(true) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    // Reading & App Preferences state
    var defaultReaderMode by remember { mutableStateOf(UserPreferences.getDefaultReaderMode(context)) }
    var defaultReaderBg by remember { mutableStateOf(UserPreferences.getDefaultReaderBg(context)) }
    var keepScreenAwake by remember { mutableStateOf(UserPreferences.isKeepScreenAwake(context)) }
    var pagePreloadEnabled by remember { mutableStateOf(UserPreferences.isPagePreloadEnabled(context)) }
    var tapToScrollEnabled by remember { mutableStateOf(UserPreferences.isTapToScrollEnabled(context)) }

    // Accordion state: null initially so only headlines are shown first
    var expandedSection by remember { mutableStateOf<String?>(null) }

    // Backup & Restore states
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var exportResultDialog by remember { mutableStateOf<BackupExportResult?>(null) }
    var restoreResultDialog by remember { mutableStateOf<BackupRestoreResult?>(null) }

    // 24-Hour Auto-Backup states
    var autoBackupEnabled by remember { mutableStateOf(UserPreferences.isAutoBackupEnabled(context)) }
    var autoBackupFileName by remember { mutableStateOf(UserPreferences.getAutoBackupFileName(context)) }
    var autoBackupUri by remember { mutableStateOf(UserPreferences.getAutoBackupUri(context)) }
    var lastAutoBackupTime by remember { mutableLongStateOf(UserPreferences.getLastAutoBackupTime(context)) }
    var lastAutoBackupStatus by remember { mutableStateOf(UserPreferences.getLastAutoBackupStatus(context)) }
    var isAutoBackingUpNow by remember { mutableStateOf(false) }

    // Admin & Owner Secret Portal states (Hidden feature on J avatar)
    var adminTapCount by remember { mutableIntStateOf(0) }
    var lastAdminTapTime by remember { mutableLongStateOf(0L) }
    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var adminPasswordVisible by remember { mutableStateOf(false) }
    var adminPasswordError by remember { mutableStateOf(false) }

    val chooseAutoBackupLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            AppDataBackupManager.persistUriPermission(context, uri)
            val fileName = AppDataBackupManager.getFileNameFromUri(context, uri)
            UserPreferences.setAutoBackupUri(context, uri.toString())
            UserPreferences.setAutoBackupFileName(context, fileName)
            UserPreferences.setAutoBackupEnabled(context, true)
            autoBackupUri = uri.toString()
            autoBackupFileName = fileName
            autoBackupEnabled = true
            AutoBackupScheduler.schedule24HourAutoBackup(context)

            isAutoBackingUpNow = true
            CoroutineScope(Dispatchers.IO).launch {
                val result = AppDataBackupManager.performAutoBackup(context)
                withContext(Dispatchers.Main) {
                    isAutoBackingUpNow = false
                    lastAutoBackupTime = UserPreferences.getLastAutoBackupTime(context)
                    lastAutoBackupStatus = UserPreferences.getLastAutoBackupStatus(context)
                    if (result.success) {
                        Toast.makeText(context, "Auto-backup file configured & saved! ⏰", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Location saved. Initial backup: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            AppDataBackupManager.persistUriPermission(context, uri)
            val fileName = AppDataBackupManager.getFileNameFromUri(context, uri)
            UserPreferences.setAutoBackupUri(context, uri.toString())
            UserPreferences.setAutoBackupFileName(context, fileName)
            autoBackupUri = uri.toString()
            autoBackupFileName = fileName

            CoroutineScope(Dispatchers.IO).launch {
                val result = AppDataBackupManager.exportToFile(context, uri)
                if (result.success && UserPreferences.isAutoBackupEnabled(context)) {
                    UserPreferences.setLastAutoBackupTime(context, System.currentTimeMillis())
                    val statusMsg = "Success (${result.bookmarksCount} bookmarks, ${result.historyCount} history items)"
                    UserPreferences.setLastAutoBackupStatus(context, statusMsg)
                    AutoBackupScheduler.schedule24HourAutoBackup(context)
                }
                withContext(Dispatchers.Main) {
                    isExporting = false
                    exportResultDialog = result
                    lastAutoBackupTime = UserPreferences.getLastAutoBackupTime(context)
                    lastAutoBackupStatus = UserPreferences.getLastAutoBackupStatus(context)
                    if (result.success) {
                        Toast.makeText(context, "Backup exported successfully! 📦", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Export failed: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            CoroutineScope(Dispatchers.IO).launch {
                val result = AppDataBackupManager.importFromFile(context, uri)
                withContext(Dispatchers.Main) {
                    isImporting = false
                    restoreResultDialog = result
                    if (result.success) {
                        onClearHistorySuccess() // refresh triggers
                        Toast.makeText(context, "Data restored successfully! 🎉", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Import failed: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & About",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // MyManga Brand Header
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1017)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE53935)),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "My",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Manga",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFE53935)
                            )
                        }
                        Text(
                            text = "Ultimate Manga Reader Engine • v2.4.0 Ultra",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }
            }
            // 1. Notification & Alerts Card (Expandable Accordion)
            ExpandableSettingsCard(
                headline = "Notification & Alerts",
                subtitle = "Instant chapter releases & reading alerts",
                icon = Icons.Default.NotificationsActive,
                iconColor = if (hasNotificationPermission) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                isExpanded = expandedSection == "notifications",
                onToggle = {
                    expandedSection = if (expandedSection == "notifications") null else "notifications"
                },
                modifier = Modifier.testTag("settings_section_notifications")
            ) {
                Text(
                    text = "Receive instant alerts when new manga chapters release, daily reading streak reminders, and library updates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable Notifications Permission")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Notification Preferences
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "New Chapter Releases",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Get notified when followed manga uploads new chapters",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = newChapterAlertsEnabled,
                            onCheckedChange = { newChapterAlertsEnabled = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Reading Reminders",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Daily streak alerts for trending release updates",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = dailyStreakRemindersEnabled,
                            onCheckedChange = { dailyStreakRemindersEnabled = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Library Sync Alerts",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Notification updates for bookmarked series",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = librarySyncAlertsEnabled,
                            onCheckedChange = { librarySyncAlertsEnabled = it }
                        )
                    }
                }
            }

            // 2. Reading & App Preferences Card (Expandable Accordion)
            ExpandableSettingsCard(
                headline = "Reading & App Preferences",
                subtitle = "Reader direction, background & display options",
                icon = Icons.Default.Tune,
                iconColor = MaterialTheme.colorScheme.secondary,
                isExpanded = expandedSection == "preferences",
                onToggle = {
                    expandedSection = if (expandedSection == "preferences") null else "preferences"
                },
                modifier = Modifier.testTag("settings_section_preferences")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Default Reader Mode
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Default Reader Direction & Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Webtoon (Vertical)", "Paged (Horizontal)").forEach { mode ->
                                val isSelected = defaultReaderMode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        defaultReaderMode = mode
                                        UserPreferences.setDefaultReaderMode(context, mode)
                                    },
                                    label = { Text(mode, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Default Background
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Reader Canvas Background",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("OLED Pure Black", "Sepia Paper", "Pure White").forEach { bg ->
                                val isSelected = defaultReaderBg == bg
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        defaultReaderBg = bg
                                        UserPreferences.setDefaultReaderBg(context, bg)
                                    },
                                    label = { Text(bg, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Keep Screen Awake
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Keep Screen Awake",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Prevents display timeout while actively reading chapters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = keepScreenAwake,
                            onCheckedChange = {
                                keepScreenAwake = it
                                UserPreferences.setKeepScreenAwake(context, it)
                            }
                        )
                    }

                    // Preload Upcoming Pages
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Preload Upcoming Pages",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Pre-buffers next 5 pages in memory for zero waiting",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = pagePreloadEnabled,
                            onCheckedChange = {
                                pagePreloadEnabled = it
                                UserPreferences.setPagePreloadEnabled(context, it)
                            }
                        )
                    }

                    // Tap to Scroll / Advance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tap Navigation & Page Turn",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap left or right screen edges to turn pages smoothly",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = tapToScrollEnabled,
                            onCheckedChange = {
                                tapToScrollEnabled = it
                                UserPreferences.setTapToScrollEnabled(context, it)
                            }
                        )
                    }
                }
            }

            // 3. Backup & Restore Data Card (Expandable Accordion)
            ExpandableSettingsCard(
                headline = "Backup & Restore Data",
                subtitle = "Export or import bookmarks, history & profile",
                icon = Icons.Default.Backup,
                iconColor = MaterialTheme.colorScheme.primary,
                isExpanded = expandedSection == "backup",
                onToggle = {
                    expandedSection = if (expandedSection == "backup") null else "backup"
                },
                modifier = Modifier.testTag("settings_section_backup")
            ) {
                Text(
                    text = "Export all your bookmarked manga, reading history, and profile settings to a backup file. If you reinstall the app or switch devices, simply import the file to restore all your data instantly!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Export / Backup Button
                Button(
                    onClick = {
                        exportLauncher.launch(AppDataBackupManager.getDefaultBackupFileName())
                    },
                    enabled = !isExporting && !isImporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_backup_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Exporting Backup...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export App Data (Create Backup)", fontWeight = FontWeight.Bold)
                    }
                }

                // Import / Restore Button
                OutlinedButton(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    },
                    enabled = !isExporting && !isImporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_backup_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Restoring Data...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import App Data (Restore Backup)", fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // 24-Hour Auto-Backup to Same File Toggle & Settings
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(
                        1.dp,
                        if (autoBackupEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (autoBackupEnabled) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surface
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (autoBackupEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "24-Hour Auto-Backup",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Auto-save & update same file every 24 hours",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = autoBackupEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (!autoBackupUri.isNullOrBlank()) {
                                            autoBackupEnabled = true
                                            UserPreferences.setAutoBackupEnabled(context, true)
                                            AutoBackupScheduler.schedule24HourAutoBackup(context)
                                            Toast.makeText(context, "24-Hour Auto-Backup activated! ⏰", Toast.LENGTH_SHORT).show()
                                        } else {
                                            chooseAutoBackupLocationLauncher.launch(
                                                AppDataBackupManager.getDefaultBackupFileName()
                                            )
                                        }
                                    } else {
                                        autoBackupEnabled = false
                                        UserPreferences.setAutoBackupEnabled(context, false)
                                        AutoBackupScheduler.cancelAutoBackup(context)
                                        Toast.makeText(context, "24-Hour Auto-Backup paused", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("auto_backup_toggle")
                            )
                        }

                        // Target file location card & details
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.InsertDriveFile,
                                            contentDescription = null,
                                            tint = if (!autoBackupUri.isNullOrBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (!autoBackupFileName.isNullOrBlank()) autoBackupFileName!! else "No backup file selected",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if (!autoBackupUri.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            chooseAutoBackupLocationLauncher.launch(
                                                autoBackupFileName?.takeIf { it.isNotBlank() } ?: AppDataBackupManager.getDefaultBackupFileName()
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (autoBackupUri.isNullOrBlank()) "Choose File" else "Change File",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                if (lastAutoBackupTime > 0L) {
                                    val formattedDate = remember(lastAutoBackupTime) {
                                        try {
                                            val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                                            sdf.format(Date(lastAutoBackupTime))
                                        } catch (_: Exception) {
                                            "Recently"
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Last saved: $formattedDate",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else if (autoBackupEnabled) {
                                    Text(
                                        text = "⏳ Next automatic cycle scheduled within 24 hours",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Manual Trigger "Update Backup Now" button
                        if (autoBackupEnabled && !autoBackupUri.isNullOrBlank()) {
                            FilledTonalButton(
                                onClick = {
                                    isAutoBackingUpNow = true
                                    AutoBackupScheduler.triggerImmediateAutoBackup(context) { result ->
                                        isAutoBackingUpNow = false
                                        lastAutoBackupTime = UserPreferences.getLastAutoBackupTime(context)
                                        lastAutoBackupStatus = UserPreferences.getLastAutoBackupStatus(context)
                                        if (result.success) {
                                            Toast.makeText(
                                                context,
                                                "Backup file updated successfully! (${result.bookmarksCount} bookmarks, ${result.historyCount} history)",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(context, "Auto-backup failed: ${result.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                enabled = !isAutoBackingUpNow && !isExporting && !isImporting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auto_backup_now_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isAutoBackingUpNow) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Updating File...", style = MaterialTheme.typography.labelMedium)
                                } else {
                                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Update Backup Now in Same File", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Text(
                    text = "History & Cache Management",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { showClearHistoryDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_history_settings_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Reading History")
                }
            }

            // 4. Data Sources & Architecture Card (Expandable Accordion)
            ExpandableSettingsCard(
                headline = "Data Sources & Architecture",
                subtitle = "AniList GraphQL & Mangapill engine",
                icon = Icons.Default.Cloud,
                iconColor = MaterialTheme.colorScheme.tertiary,
                isExpanded = expandedSection == "sources",
                onToggle = {
                    expandedSection = if (expandedSection == "sources") null else "sources"
                },
                modifier = Modifier.testTag("settings_section_sources")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "AniList GraphQL API",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Provides trending manga, search, metadata, covers & ratings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Mangapill Source Engine",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Fetches manga chapters, page images, and releases.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 5. App Features & Specifications Card (Expandable Accordion)
            ExpandableSettingsCard(
                headline = "App Features & Specifications",
                subtitle = "Ultra performance, webtoon & offline downloads",
                icon = Icons.Default.Speed,
                iconColor = MaterialTheme.colorScheme.primary,
                isExpanded = expandedSection == "features",
                onToggle = {
                    expandedSection = if (expandedSection == "features") null else "features"
                },
                modifier = Modifier.testTag("settings_section_features")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureDetailItem(
                        icon = Icons.Default.Speed,
                        title = "60-120 FPS Ultra Performance",
                        description = "Hardware accelerated Compose rendering with lazy key indexing and memory caching."
                    )
                    FeatureDetailItem(
                        icon = Icons.Default.AutoStories,
                        title = "Smart Webtoon & Page Reader",
                        description = "Continuous vertical webtoon scrolling or horizontal paged mode with quick page slider."
                    )
                    FeatureDetailItem(
                        icon = Icons.Default.DownloadForOffline,
                        title = "Offline Downloads & Room SQLite",
                        description = "Download full manga chapters locally for offline reading anytime."
                    )
                }
            }

            // 6. Share MyManga App Card (Expandable Accordion)
            ExpandableSettingsCard(
                headline = "Share MyManga App",
                subtitle = "Spread the word to fellow readers",
                icon = Icons.Default.Share,
                iconColor = MaterialTheme.colorScheme.secondary,
                isExpanded = expandedSection == "share",
                onToggle = {
                    expandedSection = if (expandedSection == "share") null else "share"
                },
                modifier = Modifier.testTag("settings_section_share")
            ) {
                Text(
                    text = "MyManga is the ultimate manga & webtoon reader engine featuring high-speed chapter streaming, webtoon mode, offline downloads, and library tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val appShareUrl = "https://apk-downloader.lovable.app/app/manga/mymanga"

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = appShareUrl,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("MyManga Download Link", appShareUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy App Link",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "MyManga - Ultimate Manga Reader")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Hey! Check out MyManga, the best app for reading manga & webtoons with high-speed chapter loading!\nDownload now: $appShareUrl"
                                )
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share MyManga App")
                            context.startActivity(shareIntent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_app_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share App",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("MyManga Download Link", appShareUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "App download link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_app_link_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copy Link",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 7. Owner & Creator Profile Card (Expandable Accordion)
            ExpandableSettingsCard(
                headline = "Owner & Creator Profile",
                subtitle = "Crafted by Joydeep (@_joydeep11)",
                icon = Icons.Default.Person,
                iconColor = MaterialTheme.colorScheme.primary,
                isExpanded = expandedSection == "creator",
                onToggle = {
                    expandedSection = if (expandedSection == "creator") null else "creator"
                },
                modifier = Modifier.testTag("settings_section_creator")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val instagramHandle = "_joydeep11"
                    val instagramDpUrl = "https://unavatar.io/instagram/$instagramHandle"

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastAdminTapTime < 2000L) {
                                    adminTapCount++
                                } else {
                                    adminTapCount = 1
                                }
                                lastAdminTapTime = currentTime

                                if (adminTapCount >= 5) {
                                    adminTapCount = 0
                                    adminPasswordInput = ""
                                    adminPasswordError = false
                                    adminPasswordVisible = false
                                    showAdminPasswordDialog = true
                                }
                            }
                            .testTag("owner_j_avatar")
                    ) {
                        AsyncImage(
                            model = instagramDpUrl,
                            contentDescription = "Instagram Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Joydeep",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Lead App Developer & Creator",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "Crafted with passion for manga readers. Delivers a high-fps, buttery smooth reading experience with zero lag.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Social Links Row
                Text(
                    text = "Connect & Socials",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SocialChip(
                        label = "Instagram Profile",
                        painter = painterResource(id = R.drawable.ic_instagram),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("instagram_creator_button"),
                        containerColor = Color(0xFFE1306C).copy(alpha = 0.12f),
                        contentColor = Color(0xFFE1306C),
                        onClick = {
                            try { uriHandler.openUri("https://www.instagram.com/_joydeep11/") } catch (_: Exception) {}
                        }
                    )

                    SocialChip(
                        label = "Discord Profile",
                        painter = painterResource(id = R.drawable.ic_discord),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("discord_creator_button"),
                        containerColor = Color(0xFF5865F2).copy(alpha = 0.12f),
                        contentColor = Color(0xFF5865F2),
                        onClick = {
                            try { uriHandler.openUri("https://discord.com/users/1495322183088607294") } catch (_: Exception) {}
                        }
                    )
                }
            }

            // Dialogs for Backup, Restore & Clear History
            if (exportResultDialog != null) {
                val res = exportResultDialog!!
                AlertDialog(
                    onDismissRequest = { exportResultDialog = null },
                    icon = {
                        Icon(
                            imageVector = if (res.success) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (res.success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = {
                        Text(
                            text = if (res.success) "Backup Exported! 📦" else "Export Failed",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (res.success) {
                                Text(
                                    text = "Your app data has been successfully exported to your chosen file location.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "🔖 Bookmarks: ${res.bookmarksCount}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "📖 Reading History: ${res.historyCount}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "👤 Profile & Preferences: Saved",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Text(
                                    text = "Keep this file safe! If you reinstall the app or switch phones, tap 'Import App Data' to get all your data back.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = res.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { exportResultDialog = null },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("OK")
                        }
                    }
                )
            }

            if (restoreResultDialog != null) {
                val res = restoreResultDialog!!
                AlertDialog(
                    onDismissRequest = { restoreResultDialog = null },
                    icon = {
                        Icon(
                            imageVector = if (res.success) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (res.success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = {
                        Text(
                            text = if (res.success) "Data Restored Successfully! 🎉" else "Restore Failed",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (res.success) {
                                Text(
                                    text = "All your previous app data has been recovered and imported into MyManga!",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "🔖 Bookmarks Restored: ${res.bookmarksRestored}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "📖 History Entries Restored: ${res.historyRestored}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (res.username.isNotBlank()) {
                                            Text(
                                                text = "👤 User Profile: ${res.username}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = res.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { restoreResultDialog = null },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Awesome!")
                        }
                    }
                )
            }

            if (showClearHistoryDialog) {
                AlertDialog(
                    onDismissRequest = { showClearHistoryDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    title = {
                        Text(
                            text = "Are you sure?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "This action will permanently delete all your reading history. This cannot be undone.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showClearHistoryDialog = false
                                CoroutineScope(Dispatchers.IO).launch {
                                    repository.clearHistory()
                                    onClearHistorySuccess()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.testTag("confirm_clear_history_button")
                        ) {
                            Text("Clear")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showClearHistoryDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showAdminPasswordDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showAdminPasswordDialog = false
                        adminPasswordInput = ""
                        adminPasswordError = false
                    },
                    icon = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    },
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Owner Verification",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Restricted Area for Joydeep (App Owner)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = adminPasswordInput,
                                onValueChange = {
                                    adminPasswordInput = it
                                    if (adminPasswordError) adminPasswordError = false
                                },
                                label = { Text("Admin Password") },
                                placeholder = { Text("Enter owner password...") },
                                singleLine = true,
                                isError = adminPasswordError,
                                visualTransformation = if (adminPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { adminPasswordVisible = !adminPasswordVisible }) {
                                        Icon(
                                            imageVector = if (adminPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (adminPasswordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        val currentPass = UserPreferences.getOwnerPassword(context)
                                        if (adminPasswordInput == currentPass) {
                                            showAdminPasswordDialog = false
                                            adminPasswordInput = ""
                                            adminPasswordError = false
                                            onNavigateToOwnerPanel()
                                        } else {
                                            adminPasswordError = true
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_password_input")
                            )

                            if (adminPasswordError) {
                                Text(
                                    text = "Incorrect password. Access denied.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val currentPass = UserPreferences.getOwnerPassword(context)
                                if (adminPasswordInput == currentPass) {
                                    showAdminPasswordDialog = false
                                    adminPasswordInput = ""
                                    adminPasswordError = false
                                    onNavigateToOwnerPanel()
                                } else {
                                    adminPasswordError = true
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("admin_password_submit_button")
                        ) {
                            Text("Unlock")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showAdminPasswordDialog = false
                                adminPasswordInput = ""
                                adminPasswordError = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FeatureDetailItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SocialChip(
    label: String,
    painter: Painter,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painter,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun ExpandableSettingsCard(
    headline: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "chevron_rotation"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Headline Row (Always visible, tap anywhere or button to slide down / up)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = iconColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle != null) {
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

                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse section" else "Expand section",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }

            // Inside options: slides down on expand, slides up on collapse
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    content()
                }
            }
        }
    }
}
