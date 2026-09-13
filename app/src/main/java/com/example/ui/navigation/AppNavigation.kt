package com.example.ui.navigation

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import com.example.utils.UserPreferences
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import com.example.data.local.MangaDatabase
import com.example.data.network.AnilistClient
import com.example.data.network.MangaDexClient
import com.example.data.network.MangaFireClient
import com.example.data.network.MangapillClient
import com.example.data.network.WeebCentralClient
import com.example.data.repository.MangaRepository
import com.example.ui.screens.detail.MangaDetailScreen
import com.example.ui.screens.detail.MangaDetailViewModel
import com.example.ui.screens.explore.ExploreScreen
import com.example.ui.screens.explore.ExploreViewModel
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.library.LibraryViewModel
import com.example.ui.screens.profile.AnimeAvatarRepository
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.reader.ReaderScreen
import com.example.ui.screens.reader.ReaderViewModel
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.search.SearchViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.OwnerControlPanelScreen
import com.example.ui.screens.maintenance.MaintenanceScreen
import com.example.ui.components.AppUpdateDialog
import com.example.utils.NotificationHelper

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    intent: Intent? = null,
    onIntentHandled: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Instantiate Singletons / Repository
    val database = remember { MangaDatabase.getDatabase(context) }
    val anilistClient = remember { AnilistClient() }
    val mangapillClient = remember { MangapillClient() }
    val mangadexClient = remember { MangaDexClient() }
    val weebCentralClient = remember { WeebCentralClient(mangapillClient, mangadexClient) }
    val mangaFireClient = remember { MangaFireClient(mangapillClient, mangadexClient) }
    val repository = remember {
        MangaRepository(
            appContext = context.applicationContext,
            anilistClient = anilistClient,
            mangapillClient = mangapillClient,
            mangadexClient = mangadexClient,
            weebCentralClient = weebCentralClient,
            mangaFireClient = mangaFireClient,
            mangaDao = database.mangaDao()
        )
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Trigger progress saved notification when app is closed / backgrounded
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                NotificationHelper.triggerAppCloseProgressNotification(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle Notification & URL Deep Linking
    LaunchedEffect(intent) {
        if (intent != null) {
            var mangaId = intent.getIntExtra("EXTRA_MANGA_ID", -1)
            val chapterId = intent.getStringExtra("EXTRA_CHAPTER_ID")
            val chapterTitle = intent.getStringExtra("EXTRA_CHAPTER_TITLE")

            val dataUri = intent.data
            if (mangaId == -1 && dataUri != null) {
                val queryId = dataUri.getQueryParameter("id")?.toIntOrNull()
                    ?: dataUri.getQueryParameter("mangaId")?.toIntOrNull()
                if (queryId != null) {
                    mangaId = queryId
                } else {
                    val pathSegments = dataUri.pathSegments
                    if (pathSegments.size >= 2 && pathSegments[0] == "manga") {
                        mangaId = pathSegments[1].toIntOrNull() ?: -1
                    } else if (dataUri.host == "manga" && pathSegments.isNotEmpty()) {
                        mangaId = pathSegments[0].toIntOrNull() ?: -1
                    } else if (pathSegments.isNotEmpty()) {
                        mangaId = pathSegments.lastOrNull()?.toIntOrNull() ?: -1
                    }
                }
            }

            if (mangaId != -1) {
                if (!chapterId.isNullOrEmpty() && !chapterTitle.isNullOrEmpty()) {
                    navController.navigate(Screen.Reader.createRoute(mangaId, chapterId, chapterTitle))
                } else {
                    navController.navigate(Screen.MangaDetail.createRoute(mangaId))
                }
                onIntentHandled?.invoke()
            }
        }
    }

    // Automatic Notification Permission & Username Onboarding Logic on App Launch
    var hasNotificationPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }
    var currentUsername by remember {
        mutableStateOf(UserPreferences.getUsername(context))
    }
    var showNotificationDialog by remember {
        mutableStateOf(!hasNotificationPermission && UserPreferences.isFirstTime(context))
    }
    var showUsernameDialog by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(showNotificationDialog, currentUsername) {
        if (!showNotificationDialog && currentUsername.isBlank()) {
            showUsernameDialog = true
        }
    }

    LaunchedEffect(hasNotificationPermission) {
        if (hasNotificationPermission) {
            withContext(Dispatchers.IO) {
                val hist = repository.allHistory.firstOrNull() ?: emptyList()
                val bkmk = repository.allBookmarks.firstOrNull() ?: emptyList()
                NotificationHelper.checkAndSendSmartMangaNotifications(
                    context = context,
                    historyList = hist,
                    bookmarksList = bkmk
                )
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        showNotificationDialog = false
        if (currentUsername.isBlank()) {
            showUsernameDialog = true
        }
        if (isGranted) {
            NotificationHelper.sendNotification(
                context = context,
                title = "Notifications Enabled! 🎉",
                message = "You will now receive new manga chapter releases, daily reading reminders, and bookmark updates from MyManga."
            )
        }
    }

    if (showNotificationDialog && !hasNotificationPermission) {
        AlertDialog(
            onDismissRequest = {
                showNotificationDialog = false
                if (currentUsername.isBlank()) {
                    showUsernameDialog = true
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Turn On Notifications",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "Allow MyManga to send you notifications so you never miss new manga chapter releases, daily reading streak reminders, and library updates!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            hasNotificationPermission = true
                            showNotificationDialog = false
                            if (currentUsername.isBlank()) {
                                showUsernameDialog = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Allow Notifications", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showNotificationDialog = false
                        if (currentUsername.isBlank()) {
                            showUsernameDialog = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Not Now")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showUsernameDialog && currentUsername.isBlank()) {
        var usernameInput by remember { mutableStateOf("") }
        val previewAvatar = remember(usernameInput) {
            val name = usernameInput.trim()
            if (name.isNotBlank()) {
                AnimeAvatarRepository.getAvatarForUsername(name)
            } else {
                AnimeAvatarRepository.getAvatarForUsername("Manga Reader")
            }
        }

        AlertDialog(
            onDismissRequest = {
                // Mandatory popup: do not allow dismiss by tapping outside
            },
            icon = {
                Surface(
                    shape = CircleShape,
                    border = BorderStroke(3.dp, Color(0xFFE53935)),
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(previewAvatar.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Anime Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            title = {
                Text(
                    text = "Welcome to MyManga! 🌸",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enter your username to personalize your reading journey. An Anime Avatar will be automatically assigned to you!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Username") },
                        placeholder = { Text("e.g. MangaMaster") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input_field")
                    )

                    // Auto Avatar preview banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "🎭 Auto Avatar: ",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${previewAvatar.name} (${previewAvatar.anime})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53935),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = usernameInput.trim()
                        if (trimmed.isNotBlank()) {
                            val autoAvatar = AnimeAvatarRepository.getAvatarForUsername(trimmed)
                            UserPreferences.setUsername(context, trimmed)
                            UserPreferences.setAvatarUri(context, autoAvatar.imageUrl)
                            UserPreferences.setFirstTimeCompleted(context)
                            currentUsername = trimmed
                            showUsernameDialog = false

                            NotificationHelper.sendNotification(
                                context = context,
                                title = "Welcome $trimmed! 🎉",
                                message = "Your profile is set up with ${autoAvatar.name} as your Anime Avatar! Enjoy reading on MyManga."
                            )
                        }
                    },
                    enabled = usernameInput.trim().isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_username_button")
                ) {
                    Text("Save Username", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = null, // Mandatory dialog with no close or later option as requested!
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(20.dp)
        )
    }

    val bottomNavItems = listOf(
        BottomNavItem(
            route = Screen.Explore.route,
            label = "Explore",
            icon = { Icon(imageVector = Icons.Default.Explore, contentDescription = "Explore") },
            testTag = "nav_item_explore"
        ),
        BottomNavItem(
            route = Screen.Search.route,
            label = "Search",
            icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            testTag = "nav_item_search"
        ),
        BottomNavItem(
            route = Screen.Library.route,
            label = "Library",
            icon = { Icon(imageVector = Icons.Default.Bookmark, contentDescription = "Library") },
            testTag = "nav_item_library"
        ),
        BottomNavItem(
            route = Screen.Settings.route,
            label = "Settings",
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
            testTag = "nav_item_settings"
        )
    )

    // Hide bottom bar in Reader screen and Detail screen
    var isMaintenanceMode by remember {
        mutableStateOf(UserPreferences.isMaintenanceModeEnabled(context))
    }
    var isUpdateAvailable by remember {
        mutableStateOf(UserPreferences.isUpdateAvailable(context))
    }
    var showUpdateDialog by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(currentRoute) {
        isMaintenanceMode = UserPreferences.isMaintenanceModeEnabled(context)
        isUpdateAvailable = UserPreferences.isUpdateAvailable(context)
        if (!isMaintenanceMode && isUpdateAvailable && currentRoute != Screen.OwnerPanel.route) {
            val targetVersion = UserPreferences.getUpdateVersionName(context)
            val dismissedVersion = UserPreferences.getDismissedUpdateVersion(context)
            if (dismissedVersion != targetVersion) {
                showUpdateDialog = true
            }
        }
    }

    val shouldShowBottomBar = !isMaintenanceMode && ((currentRoute in listOf(
        Screen.Explore.route,
        Screen.Search.route,
        Screen.Library.route,
        Screen.Settings.route
    ) || currentRoute?.startsWith("library") == true))

    if (isMaintenanceMode && currentRoute != Screen.OwnerPanel.route) {
        MaintenanceScreen(
            onNavigateToOwnerPanel = {
                navController.navigate(Screen.OwnerPanel.route)
            }
        )
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (shouldShowBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                        tonalElevation = 8.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentRoute == item.route || (item.route == Screen.Library.route && currentRoute?.startsWith("library") == true)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        if (item.route == Screen.Explore.route) {
                                            val popped = navController.popBackStack(Screen.Explore.route, inclusive = false)
                                            if (!popped) {
                                                navController.navigate(Screen.Explore.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        inclusive = true
                                                    }
                                                    launchSingleTop = true
                                                }
                                            }
                                        } else {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                },
                                icon = item.icon,
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Explore.route,
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            ) {
            // 1. Explore Screen
            composable(Screen.Explore.route) {
                val exploreViewModel: ExploreViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return ExploreViewModel(repository) as T
                        }
                    }
                )
                ExploreScreen(
                    viewModel = exploreViewModel,
                    onMangaClick = { mangaId ->
                        navController.navigate(Screen.MangaDetail.createRoute(mangaId))
                    },
                    onSearchClick = {
                        if (currentRoute != Screen.Search.route) {
                            navController.navigate(Screen.Search.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onProfileClick = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onGoToDownloads = {
                        navController.navigate(Screen.Library.createRoute(2))
                    }
                )
            }

            // 1.5 Profile Screen
            composable(Screen.Profile.route) {
                val exploreViewModel: ExploreViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return ExploreViewModel(repository) as T
                        }
                    }
                )
                ProfileScreen(
                    viewModel = exploreViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 2. Search Screen
            composable(Screen.Search.route) {
                val searchViewModel: SearchViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return SearchViewModel(repository) as T
                        }
                    }
                )
                SearchScreen(
                    viewModel = searchViewModel,
                    onMangaClick = { mangaId ->
                        navController.navigate(Screen.MangaDetail.createRoute(mangaId))
                    },
                    onGoToDownloads = {
                        navController.navigate(Screen.Library.createRoute(2))
                    }
                )
            }

            // 3. Library Screen
            composable(
                route = Screen.Library.route,
                arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })
            ) { backStackEntry ->
                val tabArg = backStackEntry.arguments?.getInt("tab") ?: 0
                val libraryViewModel: LibraryViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return LibraryViewModel(repository) as T
                        }
                    }
                )
                LibraryScreen(
                    viewModel = libraryViewModel,
                    initialTab = tabArg,
                    onMangaClick = { mangaId ->
                        navController.navigate(Screen.MangaDetail.createRoute(mangaId))
                    },
                    onResumeReaderClick = { mangaId, chapterId, chapterTitle ->
                        navController.navigate(Screen.Reader.createRoute(mangaId, chapterId, chapterTitle))
                    }
                )
            }

            // 4. Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen(
                    repository = repository,
                    onClearHistorySuccess = {},
                    onNavigateToOwnerPanel = {
                        navController.navigate(Screen.OwnerPanel.route)
                    }
                )
            }

            // 4.5 Owner Control Panel (Full-Screen)
            composable(Screen.OwnerPanel.route) {
                OwnerControlPanelScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 5. Manga Detail Screen
            composable(
                route = Screen.MangaDetail.route,
                arguments = listOf(navArgument("mangaId") { type = NavType.IntType })
            ) { backStackEntry ->
                val mangaId = backStackEntry.arguments?.getInt("mangaId") ?: 0
                val detailViewModel: MangaDetailViewModel = viewModel(
                    key = "detail_$mangaId",
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return MangaDetailViewModel(repository, mangaId) as T
                        }
                    }
                )
                MangaDetailScreen(
                    viewModel = detailViewModel,
                    onBackClick = { navController.popBackStack() },
                    onChapterClick = { manga, chapter ->
                        navController.navigate(Screen.Reader.createRoute(manga.id, chapter.id, chapter.title))
                    },
                    onGoToDownloads = {
                        navController.navigate(Screen.Library.createRoute(2))
                    },
                    onMangaClick = { recMangaId ->
                        navController.navigate(Screen.MangaDetail.createRoute(recMangaId))
                    }
                )
            }

            // 6. Reader Screen
            composable(
                route = Screen.Reader.route,
                arguments = listOf(
                    navArgument("mangaId") { type = NavType.IntType },
                    navArgument("chapterId") { type = NavType.StringType },
                    navArgument("chapterTitle") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val mangaId = backStackEntry.arguments?.getInt("mangaId") ?: 0
                val encodedChapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
                val encodedChapterTitle = backStackEntry.arguments?.getString("chapterTitle") ?: ""

                val chapterId = java.net.URLDecoder.decode(encodedChapterId, "UTF-8")
                val chapterTitle = java.net.URLDecoder.decode(encodedChapterTitle, "UTF-8")

                val readerViewModel: ReaderViewModel = viewModel(
                    key = "reader_${mangaId}_$chapterId",
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return ReaderViewModel(repository, mangaId, chapterId, chapterTitle) as T
                        }
                    }
                )

                ReaderScreen(
                    viewModel = readerViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
    }

    if (showUpdateDialog && !isMaintenanceMode && currentRoute != Screen.OwnerPanel.route) {
        AppUpdateDialog(
            versionName = UserPreferences.getUpdateVersionName(context),
            featuresList = UserPreferences.getUpdateFeatures(context),
            updateUrl = UserPreferences.getUpdateUrl(context),
            onDismiss = {
                UserPreferences.setDismissedUpdateVersion(context, UserPreferences.getUpdateVersionName(context))
                showUpdateDialog = false
            }
        )
    }
}
