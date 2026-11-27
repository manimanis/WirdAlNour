package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.QuranReciter
import com.example.audio.RecitationStorageState
import com.example.audio.SurahAudioState
import com.example.data.local.entity.SurahAyahEntity
import com.example.ui.components.OrnamentalDivider
import com.example.ui.components.QuranFontPickerSheet
import com.example.ui.components.SurahAudioPlayerBar
import com.example.ui.components.SurahNumberMedallion
import com.example.ui.theme.QuranFontOption
import com.example.ui.viewmodel.SurahReaderUiState
import com.example.util.formatAyahCountArabic
import com.example.util.toArabicDigits
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahReaderScreen(
    readerState: SurahReaderUiState,
    audioState: SurahAudioState,
    recitationStorageState: RecitationStorageState,
    isDarkTheme: Boolean,
    hasAnyBookmarks: Boolean,
    onToggleNightMode: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    onOpenSurahInfo: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onToggleAyahBookmark: (SurahAyahEntity, (Boolean) -> Unit) -> Unit,
    onConsumeTargetScroll: () -> Unit,
    onOpenNextSurah: (Int) -> Unit,
    onIncreaseFont: () -> Unit,
    onDecreaseFont: () -> Unit,
    onSetFontSize: (Float) -> Unit,
    onSetFontOption: (QuranFontOption) -> Unit,
    onSetMushafMode: (Boolean) -> Unit,
    onToggleAudioPlayPause: () -> Unit,
    onSeekAudioTo: (Long) -> Unit,
    onRewindAudio10s: () -> Unit,
    onForwardAudio10s: () -> Unit,
    onSelectReciter: (QuranReciter) -> Unit,
    onDownloadRecitation: () -> Unit,
    onCancelRecitationDownload: () -> Unit,
    onDeleteRecitation: () -> Unit,
    onPlayFromAyah: (Int) -> Unit,
    onToggleAutoFollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val surah = readerState.surah
    val fontOption = readerState.fontOption
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showFontPickerSheet by remember { mutableStateOf(false) }
    var highlightedAyah by remember(surah.surahNumber) { mutableStateOf<Int?>(null) }
    var mushafLayoutResult by remember(surah.surahNumber) { mutableStateOf<TextLayoutResult?>(null) }
    var mushafAyahStartOffsets by remember(surah.surahNumber) { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    val activeRecitingAyah = if (audioState.loadedSurahNumber == surah.surahNumber) {
        audioState.activeAyahNumber
    } else {
        null
    }

    LaunchedEffect(surah.surahNumber, readerState.targetScrollAyahNumber, readerState.ayahs.size) {
        val targetAyah = readerState.targetScrollAyahNumber
        if (targetAyah != null && readerState.ayahs.isNotEmpty() && !readerState.isMushafMode) {
            val ayahIndex = readerState.ayahs.indexOfFirst { it.ayahNumber == targetAyah }
            if (ayahIndex >= 0) {
                highlightedAyah = targetAyah
                // Items before verses: 0 = Mode chips, 1 = Ornamental Header
                listState.animateScrollToItem(ayahIndex + 2)
                onConsumeTargetScroll()
            }
        } else if (targetAyah == null) {
            listState.scrollToItem(0)
        }
    }

    // Smooth auto-follow scroll synchronized with the Sheikh's recitation in both modes
    LaunchedEffect(
        activeRecitingAyah,
        audioState.isAutoFollowEnabled,
        readerState.isMushafMode,
        mushafLayoutResult,
        readerState.ayahs.size
    ) {
        if (!audioState.isAutoFollowEnabled || activeRecitingAyah == null || readerState.ayahs.isEmpty()) {
            return@LaunchedEffect
        }
        if (activeRecitingAyah == 0) {
            listState.animateScrollToItem(index = 1, scrollOffset = 0)
        } else if (readerState.isMushafMode) {
            val layout = mushafLayoutResult ?: return@LaunchedEffect
            val charOffset = mushafAyahStartOffsets[activeRecitingAyah] ?: return@LaunchedEffect
            if (charOffset in 0 until layout.layoutInput.text.length) {
                val boxTop = layout.getBoundingBox(charOffset).top
                val targetScrollOffset = (boxTop - 120f).toInt().coerceAtLeast(0)
                listState.animateScrollToItem(index = 2, scrollOffset = targetScrollOffset)
            }
        } else {
            val ayahIndex = readerState.ayahs.indexOfFirst { it.ayahNumber == activeRecitingAyah }
            if (ayahIndex >= 0) {
                listState.animateScrollToItem(index = ayahIndex + 2, scrollOffset = 0)
            }
        }
    }

    val handleAyahBookmarkTap: (SurahAyahEntity) -> Unit = { ayah ->
        onToggleAyahBookmark(ayah) { isAdded ->
            val msg = context.getString(
                if (isAdded) R.string.bookmark_added_format else R.string.bookmark_removed_format,
                ayah.ayahNumber.toArabicDigits()
            )
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    if (showFontPickerSheet) {
        QuranFontPickerSheet(
            selectedFont = fontOption,
            onSelectFont = onSetFontOption,
            onDismiss = { showFontPickerSheet = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            SurahAudioPlayerBar(
                surahNumber = surah.surahNumber,
                audioState = audioState,
                storageState = recitationStorageState,
                onTogglePlayPause = onToggleAudioPlayPause,
                onSeekTo = onSeekAudioTo,
                onRewind10s = onRewindAudio10s,
                onForward10s = onForwardAudio10s,
                onSelectReciter = onSelectReciter,
                onDownloadRecitation = onDownloadRecitation,
                onCancelDownload = onCancelRecitationDownload,
                onDeleteRecitation = onDeleteRecitation,
                onToggleAutoFollow = onToggleAutoFollow
            )
        },
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("reader_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back_cd)
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.surah_prefix, surah.arabicName),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${surah.revelationType} • ${formatAyahCountArabic(surah.ayahCount)} • ${
                                    stringResource(R.string.page_format, surah.startPage.toArabicDigits())
                                }",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onOpenBookmarks,
                            modifier = Modifier.testTag("reader_bookmarks_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Bookmarks,
                                contentDescription = stringResource(R.string.open_bookmarks_cd),
                                tint = if (readerState.bookmarkedAyahNumbers.isNotEmpty() || hasAnyBookmarks) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        IconButton(
                            onClick = onToggleNightMode,
                            modifier = Modifier.testTag("reader_night_mode_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = stringResource(R.string.toggle_night_mode_cd),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(
                            onClick = onOpenSurahInfo,
                            modifier = Modifier.testTag("reader_surah_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EditNote,
                                contentDescription = stringResource(R.string.surah_info_cd),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )

                // Pinned Font Size Bar — always visible while reading and never clips
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onDecreaseFont,
                            enabled = readerState.fontSizeSp > 18f,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("font_decrease_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = stringResource(R.string.font_size_decrease_cd),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = readerState.fontSizeSp.coerceIn(18f, 42f),
                            onValueChange = onSetFontSize,
                            valueRange = 18f..42f,
                            steps = 11,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reader_font_size_slider")
                        )

                        IconButton(
                            onClick = onIncreaseFont,
                            enabled = readerState.fontSizeSp < 42f,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("font_increase_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.font_size_increase_cd),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            onClick = { showFontPickerSheet = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("reader_font_picker_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.TextFields,
                                    contentDescription = stringResource(R.string.quran_font_picker_cd),
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = fontOption.arabicName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = readerState.fontSizeSp.roundToInt().toArabicDigits(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 0.7.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .testTag("surah_reader_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Reading Mode Toggle Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FilterChip(
                            selected = readerState.isMushafMode,
                            onClick = { onSetMushafMode(true) },
                            label = {
                                Text(
                                    text = stringResource(R.string.mushaf_view_mode),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("mode_mushaf_chip")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        FilterChip(
                            selected = !readerState.isMushafMode,
                            onClick = { onSetMushafMode(false) },
                            label = {
                                Text(
                                    text = stringResource(R.string.verses_view_mode),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("mode_verses_chip")
                        )
                    }
                }

                // Ornamental Surah Cartouche Header & Basmalah
                item {
                    SurahOrnamentalHeader(
                        surahNumber = surah.surahNumber,
                        surahName = surah.arabicName,
                        juzNumber = surah.juzNumber,
                        dayNumber = surah.dayNumber,
                        isCompleted = surah.isCompleted,
                        isBasmalahActive = activeRecitingAyah == 0,
                        fontOption = fontOption
                    )
                }

                // Verses Content State (Loading / Error / Ready)
                when {
                    readerState.isLoading && readerState.ayahs.isEmpty() -> {
                        item {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp, horizontal = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        text = stringResource(R.string.loading_surah_text),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    readerState.errorMessage != null && readerState.ayahs.isEmpty() -> {
                        item {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 36.dp, horizontal = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.surah_load_error),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = onRetry,
                                        shape = CircleShape,
                                        modifier = Modifier.testTag("retry_load_surah_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = stringResource(R.string.retry_action))
                                    }
                                }
                            }
                        }
                    }

                    readerState.isMushafMode -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.BookmarkBorder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.bookmark_tap_hint),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                MushafParagraphCard(
                                    ayahs = readerState.ayahs,
                                    bookmarkedAyahNumbers = readerState.bookmarkedAyahNumbers,
                                    activeRecitingAyah = activeRecitingAyah,
                                    fontSizeSp = readerState.fontSizeSp,
                                    fontOption = fontOption,
                                    onTapAyah = handleAyahBookmarkTap,
                                    onOffsetsComputed = { offsets -> mushafAyahStartOffsets = offsets },
                                    onTextLayout = { layout -> mushafLayoutResult = layout }
                                )
                            }
                        }
                    }

                    else -> {
                        items(readerState.ayahs, key = { it.ayahNumber }) { ayah ->
                            AyahCardItem(
                                ayah = ayah,
                                fontSizeSp = readerState.fontSizeSp,
                                fontOption = fontOption,
                                isBookmarked = ayah.ayahNumber in readerState.bookmarkedAyahNumbers,
                                isHighlighted = highlightedAyah == ayah.ayahNumber,
                                isRecitingNow = activeRecitingAyah == ayah.ayahNumber,
                                onToggleBookmark = { handleAyahBookmarkTap(ayah) },
                                onPlayFromAyah = { onPlayFromAyah(ayah.ayahNumber) }
                            )
                        }
                    }
                }

                // Sadaqallah Footer & Completion / Next Surah Actions
                if (readerState.ayahs.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OrnamentalDivider()
                            Text(
                                text = stringResource(R.string.sadaqallah_footer),
                                fontFamily = fontOption.fontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Button(
                                onClick = { onToggleCompleted(!surah.isCompleted) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("reader_toggle_complete_button"),
                                shape = MaterialTheme.shapes.medium,
                                colors = if (surah.isCompleted) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = if (surah.isCompleted) Icons.Rounded.CheckCircle else Icons.Outlined.CheckCircle,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(
                                        if (surah.isCompleted) R.string.surah_read_completed
                                        else R.string.mark_surah_as_read
                                    ),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            readerState.nextSurahInDay?.let { nextSurah ->
                                OutlinedButton(
                                    onClick = { onOpenNextSurah(nextSurah.surahNumber) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("reader_next_surah_button"),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text(
                                        text = "${stringResource(R.string.next_surah_action)}: ${
                                            stringResource(R.string.surah_prefix, nextSurah.arabicName)
                                        }",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
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

@Composable
private fun SurahOrnamentalHeader(
    surahNumber: Int,
    surahName: String,
    juzNumber: Int,
    dayNumber: Int,
    isCompleted: Boolean,
    isBasmalahActive: Boolean,
    fontOption: QuranFontOption
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SurahNumberMedallion(
                    surahNumber = surahNumber,
                    isCompleted = isCompleted,
                    size = 40.dp
                )
                Text(
                    text = stringResource(R.string.surah_prefix, surahName),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "${stringResource(R.string.day_short_format, dayNumber.toArabicDigits())} • ${
                    stringResource(R.string.juz_format, juzNumber.toArabicDigits())
                }",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OrnamentalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Surface(
                shape = CircleShape,
                color = if (isBasmalahActive) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                } else {
                    androidx.compose.ui.graphics.Color.Transparent
                },
                border = if (isBasmalahActive) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                } else {
                    null
                }
            ) {
                Text(
                    text = stringResource(R.string.bismillah_text),
                    fontFamily = fontOption.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = (24f * fontOption.lineHeightMultiplier).sp,
                    color = if (isBasmalahActive) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MushafParagraphCard(
    ayahs: List<SurahAyahEntity>,
    bookmarkedAyahNumbers: Set<Int>,
    activeRecitingAyah: Int?,
    fontSizeSp: Float,
    fontOption: QuranFontOption,
    onTapAyah: (SurahAyahEntity) -> Unit,
    onOffsetsComputed: (Map<Int, Int>) -> Unit,
    onTextLayout: (TextLayoutResult) -> Unit
) {
    val verseColor = MaterialTheme.colorScheme.onSurface
    val markerColor = MaterialTheme.colorScheme.secondary
    val bookmarkedBg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
    val bookmarkedFg = MaterialTheme.colorScheme.onSecondaryContainer
    val recitingBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)
    val recitingFg = MaterialTheme.colorScheme.onPrimaryContainer
    val recitingMarkerColor = MaterialTheme.colorScheme.primary

    val (mushafText, offsetsMap) = remember(
        ayahs,
        bookmarkedAyahNumbers,
        activeRecitingAyah,
        verseColor,
        markerColor,
        bookmarkedBg,
        bookmarkedFg,
        recitingBg,
        recitingFg,
        recitingMarkerColor,
        fontSizeSp
    ) {
        val starts = HashMap<Int, Int>(ayahs.size)
        val built = buildAnnotatedString {
            ayahs.forEachIndexed { index, ayah ->
                val isReciting = ayah.ayahNumber == activeRecitingAyah
                val isBookmarked = ayah.ayahNumber in bookmarkedAyahNumbers
                starts[ayah.ayahNumber] = length

                val bg = when {
                    isReciting -> recitingBg
                    isBookmarked -> bookmarkedBg
                    else -> androidx.compose.ui.graphics.Color.Transparent
                }
                val fg = when {
                    isReciting -> recitingFg
                    isBookmarked -> bookmarkedFg
                    else -> verseColor
                }

                withLink(
                    LinkAnnotation.Clickable(
                        tag = "ayah_${ayah.ayahNumber}",
                        linkInteractionListener = { onTapAyah(ayah) }
                    )
                ) {
                    withStyle(
                        SpanStyle(
                            color = fg,
                            background = bg,
                            fontWeight = if (isReciting) FontWeight.Bold else FontWeight.Normal
                        )
                    ) {
                        append(ayah.arabicText)
                    }
                    withStyle(
                        SpanStyle(
                            color = if (isReciting) recitingMarkerColor else markerColor,
                            background = bg,
                            fontSize = (fontSizeSp * 0.78f).sp,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        val badge = if (isBookmarked) " ✦﴿${ayah.ayahNumber.toArabicDigits()}﴾" else " ﴿${ayah.ayahNumber.toArabicDigits()}﴾"
                        append(badge)
                    }
                }
                if (index < ayahs.lastIndex) {
                    append("  ")
                }
            }
        }
        built to starts
    }

    LaunchedEffect(offsetsMap) {
        onOffsetsComputed(offsetsMap)
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(
            text = mushafText,
            fontFamily = fontOption.fontFamily,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * fontOption.lineHeightMultiplier).sp,
            textAlign = if (ayahs.size <= 6) TextAlign.Center else TextAlign.Justify,
            onTextLayout = onTextLayout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 24.dp)
                .testTag("mushaf_text_content")
        )
    }
}

@Composable
private fun AyahCardItem(
    ayah: SurahAyahEntity,
    fontSizeSp: Float,
    fontOption: QuranFontOption,
    isBookmarked: Boolean,
    isHighlighted: Boolean,
    isRecitingNow: Boolean,
    onToggleBookmark: () -> Unit,
    onPlayFromAyah: () -> Unit
) {
    val containerColor = when {
        isRecitingNow -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        isHighlighted -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        isBookmarked -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        isRecitingNow -> MaterialTheme.colorScheme.primary
        isHighlighted || isBookmarked -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(
            width = when {
                isRecitingNow -> 1.8.dp
                isHighlighted || isBookmarked -> 1.2.dp
                else -> 0.8.dp
            },
            color = borderColor
        ),
        modifier = Modifier.testTag("ayah_card_${ayah.ayahNumber}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        color = if (isRecitingNow) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Text(
                            text = "آية ${ayah.ayahNumber.toArabicDigits()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isRecitingNow) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (isRecitingNow) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = stringResource(R.string.audio_reciting_now),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPlayFromAyah,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("play_ayah_button_${ayah.ayahNumber}")
                    ) {
                        Icon(
                            imageVector = if (isRecitingNow) Icons.Outlined.GraphicEq else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.audio_play_from_ayah_cd),
                            tint = if (isRecitingNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("bookmark_ayah_button_${ayah.ayahNumber}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = stringResource(R.string.toggle_ayah_bookmark_cd),
                            tint = if (isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                text = ayah.arabicText,
                fontFamily = fontOption.fontFamily,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * fontOption.lineHeightMultiplier).sp,
                color = if (isRecitingNow) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
