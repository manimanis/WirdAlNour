package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.RecitationStorageState
import com.example.audio.SurahAudioState
import com.example.data.local.BundledQuranAssetLoader
import com.example.data.local.WirdDatabase
import com.example.data.local.entity.PlanConfigEntity
import com.example.data.repository.WirdRepository
import com.example.reminder.DailyReminderScheduler
import com.example.ui.components.BookmarksSheet
import com.example.ui.components.SurahDetailSheet
import com.example.ui.screens.PlanProgressScreen
import com.example.ui.screens.ReminderSettingsScreen
import com.example.ui.screens.SurahReaderScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.WirdAlNoorTheme
import com.example.ui.viewmodel.WirdScreenState
import com.example.ui.viewmodel.WirdTab
import com.example.ui.viewmodel.WirdViewModel
import com.example.ui.viewmodel.WirdViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = WirdDatabase.getInstance(applicationContext)
        val repository = WirdRepository(
            surahPlanDao = database.surahPlanDao(),
            planConfigDao = database.planConfigDao(),
            surahAyahDao = database.surahAyahDao(),
            ayahBookmarkDao = database.ayahBookmarkDao(),
            bundledQuranLoader = BundledQuranAssetLoader(applicationContext)
        )
        val reminderScheduler = DailyReminderScheduler(applicationContext)
        val factory = WirdViewModelFactory(applicationContext, repository, reminderScheduler)

        setContent {
            val viewModel: WirdViewModel = viewModel(factory = factory)
            val screenState by viewModel.screenState.collectAsStateWithLifecycle()
            val audioState by viewModel.audioState.collectAsStateWithLifecycle()
            val recitationStorageState by viewModel.recitationStorageState.collectAsStateWithLifecycle()

            val themeMode = (screenState as? WirdScreenState.Ready)?.data?.themeMode
                ?: PlanConfigEntity.THEME_SYSTEM
            val useDarkTheme = when (themeMode) {
                PlanConfigEntity.THEME_DARK -> true
                PlanConfigEntity.THEME_LIGHT -> false
                else -> isSystemInDarkTheme()
            }

            DisposableEffect(useDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (useDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (useDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            WirdAlNoorTheme(darkTheme = useDarkTheme) {
                WirdAlNoorApp(
                    screenState = screenState,
                    audioState = audioState,
                    recitationStorageState = recitationStorageState,
                    isDarkTheme = useDarkTheme,
                    onToggleNightMode = {
                        viewModel.setThemeMode(
                            if (useDarkTheme) PlanConfigEntity.THEME_LIGHT else PlanConfigEntity.THEME_DARK
                        )
                    },
                    viewModel = viewModel
                )
            }
        }
    }
}

private data class NavDestinationItem(
    val tab: WirdTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val navDestinations = listOf(
    NavDestinationItem(
        tab = WirdTab.TODAY,
        labelRes = R.string.nav_today,
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook,
        testTag = "nav_today_tab"
    ),
    NavDestinationItem(
        tab = WirdTab.PLAN,
        labelRes = R.string.nav_plan,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        testTag = "nav_plan_tab"
    ),
    NavDestinationItem(
        tab = WirdTab.REMINDER,
        labelRes = R.string.nav_reminder,
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
        testTag = "nav_reminder_tab"
    )
)

@Composable
private fun WirdAlNoorApp(
    screenState: WirdScreenState,
    audioState: SurahAudioState,
    recitationStorageState: RecitationStorageState,
    isDarkTheme: Boolean,
    onToggleNightMode: () -> Unit,
    viewModel: WirdViewModel
) {
    var currentTab by rememberSaveable { mutableStateOf(WirdTab.TODAY) }
    var showBookmarksSheet by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val readyData = (screenState as? WirdScreenState.Ready)?.data
    val activeReader = readyData?.readerState

    if (activeReader != null) {
        SurahReaderScreen(
            readerState = activeReader,
            audioState = audioState,
            recitationStorageState = recitationStorageState,
            isDarkTheme = isDarkTheme,
            hasAnyBookmarks = readyData.bookmarks.isNotEmpty(),
            onToggleNightMode = onToggleNightMode,
            onBack = viewModel::closeSurahReader,
            onRetry = viewModel::retryLoadSurahText,
            onToggleCompleted = { completed ->
                viewModel.toggleSurah(activeReader.surah.surahNumber, completed)
            },
            onOpenSurahInfo = {
                viewModel.openSurahDetail(activeReader.surah.surahNumber)
            },
            onOpenBookmarks = { showBookmarksSheet = true },
            onToggleAyahBookmark = { ayah, onResult ->
                viewModel.toggleAyahBookmark(
                    surahNumber = activeReader.surah.surahNumber,
                    ayahNumber = ayah.ayahNumber,
                    surahArabicName = activeReader.surah.arabicName,
                    ayahText = ayah.arabicText,
                    onResult = onResult
                )
            },
            onConsumeTargetScroll = viewModel::consumeTargetScrollAyah,
            onOpenNextSurah = { nextNum ->
                viewModel.openSurahReader(nextNum)
            },
            onIncreaseFont = viewModel::increaseFontSize,
            onDecreaseFont = viewModel::decreaseFontSize,
            onSetFontSize = viewModel::setReaderFontSize,
            onSetFontOption = viewModel::setReaderFont,
            onSetMushafMode = viewModel::setMushafMode,
            onToggleAudioPlayPause = {
                viewModel.toggleAudioPlayPause(activeReader.surah.surahNumber)
            },
            onSeekAudioTo = viewModel::seekAudioTo,
            onRewindAudio10s = viewModel::rewindAudio10s,
            onForwardAudio10s = viewModel::forwardAudio10s,
            onSelectReciter = { reciter ->
                viewModel.selectReciter(reciter, activeReader.surah.surahNumber)
            },
            onDownloadRecitation = {
                viewModel.downloadRecitation(activeReader.surah.surahNumber)
            },
            onCancelRecitationDownload = {
                viewModel.cancelRecitationDownload(activeReader.surah.surahNumber)
            },
            onDeleteRecitation = {
                viewModel.deleteRecitation(activeReader.surah.surahNumber)
            },
            onPlayFromAyah = { ayahNumber ->
                viewModel.playFromAyah(activeReader.surah.surahNumber, ayahNumber)
            },
            onToggleAutoFollow = viewModel::toggleAudioAutoFollow
        )

        readyData.selectedSurah?.let { activeSurah ->
            SurahDetailSheet(
                surah = activeSurah,
                onDismiss = viewModel::closeSurahDetail,
                onToggleCompleted = { completed ->
                    viewModel.toggleSurah(activeSurah.surahNumber, completed)
                },
                onMoveToDay = { newDay ->
                    viewModel.moveSurahToDay(activeSurah.surahNumber, newDay)
                },
                onSaveNote = { note ->
                    viewModel.saveSurahNote(activeSurah.surahNumber, note)
                }
            )
        }

        if (showBookmarksSheet) {
            BookmarksSheet(
                bookmarks = readyData.bookmarks,
                onDismiss = { showBookmarksSheet = false },
                onSelectBookmark = { surahNum, ayahNum ->
                    showBookmarksSheet = false
                    viewModel.openSurahAtAyah(surahNum, ayahNum)
                },
                onRemoveBookmark = viewModel::removeAyahBookmark
            )
        }
        return
    }

    BackHandler(enabled = currentTab != WirdTab.TODAY) {
        currentTab = WirdTab.TODAY
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        navDestinations.forEach { item ->
                            val selected = currentTab == item.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = item.tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.labelRes),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navDestinations.forEach { item ->
                            val selected = currentTab == item.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = item.tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.labelRes),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (screenState) {
                        WirdScreenState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        is WirdScreenState.Ready -> {
                            val uiState = screenState.data
                            Crossfade(
                                targetState = currentTab,
                                label = "main_tab_crossfade",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 680.dp)
                            ) { tab ->
                                when (tab) {
                                    WirdTab.TODAY -> TodayScreen(
                                        uiState = uiState,
                                        isDarkTheme = isDarkTheme,
                                        onToggleNightMode = onToggleNightMode,
                                        onSelectDay = viewModel::selectDay,
                                        onToggleSurah = viewModel::toggleSurah,
                                        onSetDayCompleted = viewModel::setDayCompleted,
                                        onOpenSurahDetail = viewModel::openSurahReader,
                                        onOpenBookmarks = { showBookmarksSheet = true },
                                        onOpenBookmarkedAyah = viewModel::openSurahAtAyah
                                    )

                                    WirdTab.PLAN -> PlanProgressScreen(
                                        uiState = uiState,
                                        onJumpToDay = { dayNum ->
                                            viewModel.selectDay(dayNum)
                                            currentTab = WirdTab.TODAY
                                        },
                                        onOpenSurah = viewModel::openSurahReader,
                                        onToggleDayComplete = viewModel::setDayCompleted,
                                        onStartNewCycle = viewModel::startNewCycle,
                                        onRestoreDefaultDistribution = viewModel::restoreDefaultDistribution
                                    )

                                    WirdTab.REMINDER -> ReminderSettingsScreen(
                                        uiState = uiState,
                                        selectedReciter = audioState.selectedReciter,
                                        recitationStorageState = recitationStorageState,
                                        onSelectReciter = { reciter ->
                                            viewModel.selectReciter(reciter, null)
                                        },
                                        onDownloadAllForReciter = viewModel::downloadAllRecitationsForReciter,
                                        onCancelBulkDownload = viewModel::cancelBulkRecitationDownload,
                                        onDeleteAllSavedRecitations = viewModel::deleteAllSavedRecitations,
                                        onSetReminderEnabled = viewModel::setReminderEnabled,
                                        onSetReminderTime = viewModel::setReminderTime,
                                        onSendTestNotification = viewModel::sendTestNotification,
                                        onSetThemeMode = viewModel::setThemeMode,
                                        onSetReaderFontSize = viewModel::setReaderFontSize,
                                        onSetReaderFont = viewModel::setReaderFont,
                                        onShowMessage = { msg ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        }
                                    )
                                }
                            }

                            uiState.selectedSurah?.let { activeSurah ->
                                SurahDetailSheet(
                                    surah = activeSurah,
                                    onDismiss = viewModel::closeSurahDetail,
                                    onToggleCompleted = { completed ->
                                        viewModel.toggleSurah(activeSurah.surahNumber, completed)
                                    },
                                    onMoveToDay = { newDay ->
                                        viewModel.moveSurahToDay(activeSurah.surahNumber, newDay)
                                    },
                                    onSaveNote = { note ->
                                        viewModel.saveSurahNote(activeSurah.surahNumber, note)
                                    }
                                )
                            }

                            if (showBookmarksSheet) {
                                BookmarksSheet(
                                    bookmarks = uiState.bookmarks,
                                    onDismiss = { showBookmarksSheet = false },
                                    onSelectBookmark = { surahNum, ayahNum ->
                                        showBookmarksSheet = false
                                        viewModel.openSurahAtAyah(surahNum, ayahNum)
                                    },
                                    onRemoveBookmark = viewModel::removeAyahBookmark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
