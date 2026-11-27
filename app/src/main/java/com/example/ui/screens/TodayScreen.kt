package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.AyahBookmarkEntity
import com.example.data.local.entity.SurahPlanEntity
import com.example.ui.components.SurahNumberMedallion
import com.example.ui.components.WirdProgressArc
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.DeepEmeraldDark
import com.example.ui.theme.NightBackground
import com.example.ui.theme.NightSurfaceContainer
import com.example.ui.theme.SoftGold
import com.example.ui.viewmodel.DayPlanUiModel
import com.example.ui.viewmodel.WirdUiState
import com.example.util.formatAyahCountArabic
import com.example.util.formatMinutesArabic
import com.example.util.formatPercentArabic
import com.example.util.formatSurahCountArabic
import com.example.util.toArabicDigits

@Composable
fun TodayScreen(
    uiState: WirdUiState,
    isDarkTheme: Boolean,
    onToggleNightMode: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onToggleSurah: (Int, Boolean) -> Unit,
    onSetDayCompleted: (Int, Boolean) -> Unit,
    onOpenSurahDetail: (Int) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenBookmarkedAyah: (surahNumber: Int, ayahNumber: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDay = uiState.selectedDayPlan ?: return
    val stepperState = rememberLazyListState()

    LaunchedEffect(uiState.selectedDayNumber) {
        val targetIndex = (uiState.selectedDayNumber - 2).coerceAtLeast(0)
        stepperState.animateScrollToItem(targetIndex)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Serene Top Header with Bookmarks & Night Reading Mode toggles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        IconButton(
                            onClick = onOpenBookmarks,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_bookmarks_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Bookmarks,
                                contentDescription = stringResource(R.string.open_bookmarks_cd),
                                tint = if (uiState.bookmarks.isNotEmpty()) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        IconButton(
                            onClick = onToggleNightMode,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_night_mode_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = stringResource(R.string.toggle_night_mode_cd),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // Hero Manuscript Banner Card
        item {
            HeroWirdCard(
                dayPlan = selectedDay,
                overallProgress = uiState.overallProgress,
                completedDays = uiState.completedDaysCount,
                isDarkTheme = isDarkTheme
            )
        }

        // 14-Day Horizontal Stepper Strip
        item {
            LazyRow(
                state = stepperState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.testTag("day_stepper_row")
            ) {
                items(uiState.days, key = { it.dayNumber }) { day ->
                    DayStepperChip(
                        day = day,
                        isSelected = day.dayNumber == selectedDay.dayNumber,
                        onClick = { onSelectDay(day.dayNumber) }
                    )
                }
            }
        }

        // Section Header for Selected Day's Surahs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.surahs_section_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatMinutesArabic(selectedDay.estimatedMinutes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Gentle note on Days 12-14 where ultra-short closing surahs are grouped evenly
        if (selectedDay.surahs.size > 3) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = stringResource(R.string.short_surahs_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // Surah Cards
        items(selectedDay.surahs, key = { it.surahNumber }) { surah ->
            SurahReadingCard(
                surah = surah,
                onToggleCompleted = { onToggleSurah(surah.surahNumber, !surah.isCompleted) },
                onClick = { onOpenSurahDetail(surah.surahNumber) }
            )
        }

        // Primary Action / Completion Banner
        item {
            Spacer(modifier = Modifier.height(4.dp))
            if (selectedDay.isCompleted) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = stringResource(R.string.day_completed_message),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        OutlinedButton(
                            onClick = { onSetDayCompleted(selectedDay.dayNumber, false) },
                            shape = CircleShape,
                            modifier = Modifier.testTag("unmark_day_complete_button")
                        ) {
                            Text(text = stringResource(R.string.unmark_day_complete))
                        }
                    }
                }
            } else if (selectedDay.surahs.isNotEmpty()) {
                Button(
                    onClick = { onSetDayCompleted(selectedDay.dayNumber, true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("mark_day_complete_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.mark_day_complete),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Quick-return Saved Bookmarks section
        if (uiState.bookmarks.isNotEmpty()) {
            item {
                SavedBookmarksHomeCard(
                    bookmarks = uiState.bookmarks.take(3),
                    totalCount = uiState.bookmarks.size,
                    onOpenBookmarkedAyah = onOpenBookmarkedAyah,
                    onOpenAllBookmarks = onOpenBookmarks
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SavedBookmarksHomeCard(
    bookmarks: List<AyahBookmarkEntity>,
    totalCount: Int,
    onOpenBookmarkedAyah: (surahNumber: Int, ayahNumber: Int) -> Unit,
    onOpenAllBookmarks: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = Icons.Rounded.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.bookmarks_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (totalCount > 3) {
                    OutlinedButton(
                        onClick = onOpenAllBookmarks,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = totalCount.toArabicDigits(),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            bookmarks.forEach { bookmark ->
                Surface(
                    onClick = { onOpenBookmarkedAyah(bookmark.surahNumber, bookmark.ayahNumber) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_bookmark_${bookmark.surahNumber}_${bookmark.ayahNumber}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.bookmark_ayah_label_format,
                                bookmark.surahArabicName,
                                bookmark.ayahNumber.toArabicDigits()
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = bookmark.ayahPreviewText,
                            fontFamily = AmiriFontFamily,
                            fontSize = 17.sp,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroWirdCard(
    dayPlan: DayPlanUiModel,
    overallProgress: Float,
    completedDays: Int,
    isDarkTheme: Boolean
) {
    val baseColor = if (isDarkTheme) NightSurfaceContainer else DeepEmeraldDark
    val scrimTop = if (isDarkTheme) NightSurfaceContainer.copy(alpha = 0.78f) else DeepEmeraldDark.copy(alpha = 0.58f)
    val scrimBottom = if (isDarkTheme) NightBackground.copy(alpha = 0.92f) else DeepEmeraldDark.copy(alpha = 0.80f)

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = baseColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_wird_card")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_wird_1795689184678),
                contentDescription = stringResource(R.string.hero_banner_cd),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(scrimTop, scrimBottom)
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.bismillah_greeting),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                        color = SoftGold
                    )
                    Text(
                        text = stringResource(R.string.day_label_format, dayPlan.dayNumber.toArabicDigits()),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                    Text(
                        text = "${formatSurahCountArabic(dayPlan.surahs.size)} • ${formatAyahCountArabic(dayPlan.totalAyahs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.86f)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f),
                        border = BorderStroke(0.6.dp, SoftGold.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "${completedDays.toArabicDigits()} / ١٤ يوماً مكتملاً",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                WirdProgressArc(
                    progress = overallProgress,
                    centerLabel = formatPercentArabic(overallProgress),
                    centerSubLabel = stringResource(R.string.completed_badge),
                    size = 104.dp,
                    strokeWidth = 7.dp,
                    trackColor = Color.White.copy(alpha = 0.16f),
                    progressColor = SoftGold,
                    accentColor = Color.White,
                    contentTextColor = Color.White
                )
            }
        }
    }
}

@Composable
private fun DayStepperChip(
    day: DayPlanUiModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            day.isCompleted -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "day_chip_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            day.isCompleted -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "day_chip_fg"
    )
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        day.isCurrentDay -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(if (day.isCurrentDay || isSelected) 1.2.dp else 0.8.dp, borderColor),
        modifier = Modifier
            .width(56.dp)
            .height(66.dp)
            .testTag("day_chip_${day.dayNumber}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "يوم",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.78f)
            )
            if (day.isCompleted && !isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(R.string.completed_badge),
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = day.dayNumber.toArabicDigits(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun SurahReadingCard(
    surah: SurahPlanEntity,
    onToggleCompleted: () -> Unit,
    onClick: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (surah.isCompleted) {
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "surah_card_color"
    )
    val borderColor by animateColorAsState(
        targetValue = if (surah.isCompleted) {
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.outline
        },
        label = "surah_card_border"
    )

    Surface(
        shape = MaterialTheme.shapes.large,
        color = cardColor,
        border = BorderStroke(0.9.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("surah_card_${surah.surahNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SurahNumberMedallion(
                surahNumber = surah.surahNumber,
                isCompleted = surah.isCompleted
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.surah_prefix, surah.arabicName),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (surah.isCompleted) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (surah.readingNote.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = stringResource(R.string.reflection_note_label),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = "${surah.revelationType} • ${formatAyahCountArabic(surah.ayahCount)} • ${
                        stringResource(R.string.page_format, surah.startPage.toArabicDigits())
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onToggleCompleted,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("surah_check_${surah.surahNumber}")
            ) {
                Icon(
                    imageVector = if (surah.isCompleted) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Rounded.RadioButtonUnchecked
                    },
                    contentDescription = stringResource(R.string.toggle_surah_cd),
                    tint = if (surah.isCompleted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.9f)
                    },
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
