package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.OrnamentalDivider
import com.example.ui.components.WirdProgressArc
import com.example.ui.viewmodel.DayPlanUiModel
import com.example.ui.viewmodel.WirdUiState
import com.example.util.formatAyahCountArabic
import com.example.util.formatDayCountArabic
import com.example.util.formatPercentArabic
import com.example.util.formatSurahCountArabic
import com.example.util.toArabicDigits

@Composable
fun PlanProgressScreen(
    uiState: WirdUiState,
    onJumpToDay: (Int) -> Unit,
    onOpenSurah: (Int) -> Unit,
    onToggleDayComplete: (Int, Boolean) -> Unit,
    onStartNewCycle: () -> Unit,
    onRestoreDefaultDistribution: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNewCycleDialog by remember { mutableStateOf(false) }
    var showRestoreDistDialog by remember { mutableStateOf(false) }

    if (showNewCycleDialog) {
        AlertDialog(
            onDismissRequest = { showNewCycleDialog = false },
            title = { Text(text = stringResource(R.string.new_cycle_dialog_title)) },
            text = { Text(text = stringResource(R.string.new_cycle_dialog_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showNewCycleDialog = false
                        onStartNewCycle()
                    },
                    modifier = Modifier.testTag("confirm_new_cycle_button")
                ) {
                    Text(text = stringResource(R.string.confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCycleDialog = false }) {
                    Text(text = stringResource(R.string.cancel_action))
                }
            }
        )
    }

    if (showRestoreDistDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDistDialog = false },
            title = { Text(text = stringResource(R.string.reset_dist_dialog_title)) },
            text = { Text(text = stringResource(R.string.reset_dist_dialog_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDistDialog = false
                        onRestoreDefaultDistribution()
                    },
                    modifier = Modifier.testTag("confirm_restore_dist_button")
                ) {
                    Text(text = stringResource(R.string.confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDistDialog = false }) {
                    Text(text = stringResource(R.string.cancel_action))
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Title
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.plan_screen_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.progress_overview_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Celebration Banner when Khatmah is 100% completed
        if (uiState.isKhatmahComplete) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = stringResource(R.string.khatmah_celebration_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = stringResource(R.string.khatmah_celebration_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { showNewCycleDialog = true },
                            shape = CircleShape,
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text(text = stringResource(R.string.start_new_cycle_action))
                        }
                    }
                }
            }
        }

        // Visual Progress Overview Card
        item {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    WirdProgressArc(
                        progress = uiState.overallProgress,
                        centerLabel = formatPercentArabic(uiState.overallProgress),
                        centerSubLabel = "${uiState.completedSurahs.toArabicDigits()} / ${uiState.totalSurahs.toArabicDigits()} سورة",
                        size = 156.dp,
                        strokeWidth = 11.dp
                    )

                    // 2x2 Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatTile(
                            label = stringResource(R.string.days_progress_label),
                            value = "${uiState.completedDaysCount.toArabicDigits()} / ١٤",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = stringResource(R.string.ayahs_progress_label),
                            value = "${uiState.completedAyahs.toArabicDigits()} / ${uiState.totalAyahs.toArabicDigits()}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatTile(
                            label = stringResource(R.string.streak_label),
                            value = formatDayCountArabic(uiState.streakDays),
                            accentIcon = true,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = stringResource(R.string.cycles_completed_label),
                            value = uiState.completedCycles.toArabicDigits(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OrnamentalDivider()

                    // Juz Tabarak & Juz Amma Split Progress
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        JuzProgressRow(
                            title = stringResource(R.string.juz_tabarak_label),
                            progress = uiState.juzTabarakProgress
                        )
                        JuzProgressRow(
                            title = stringResource(R.string.juz_amma_label),
                            progress = uiState.juzAmmaProgress
                        )
                    }
                }
            }
        }

        // 14-Day Visual Heatmap Card (2 rows x 7 days)
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.heatmap_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val firstWeek = uiState.days.take(7)
                    val secondWeek = uiState.days.drop(7)

                    HeatmapWeekRow(days = firstWeek, onJumpToDay = onJumpToDay)
                    HeatmapWeekRow(days = secondWeek, onJumpToDay = onJumpToDay)
                }
            }
        }

        // Schedule Section Title
        item {
            Text(
                text = stringResource(R.string.schedule_list_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 14-Day Schedule List
        items(uiState.days, key = { it.dayNumber }) { day ->
            DayScheduleCard(
                day = day,
                onClick = { onJumpToDay(day.dayNumber) },
                onOpenSurah = onOpenSurah,
                onToggleDayComplete = { onToggleDayComplete(day.dayNumber, !day.isCompleted) }
            )
        }

        // Plan Management Actions
        item {
            OrnamentalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showNewCycleDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("start_new_cycle_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.start_new_cycle_action),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                OutlinedButton(
                    onClick = { showRestoreDistDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("restore_distribution_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.reset_distribution_action),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentIcon: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (accentIcon) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun JuzProgressRow(
    title: String,
    progress: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatPercentArabic(progress),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun HeatmapWeekRow(
    days: List<DayPlanUiModel>,
    onJumpToDay: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { day ->
            val bgColor = when {
                day.isCompleted -> MaterialTheme.colorScheme.primary
                day.completionRatio > 0f -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainer
            }
            val fgColor = when {
                day.isCompleted -> MaterialTheme.colorScheme.onPrimary
                day.completionRatio > 0f -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val borderColor = if (day.isCurrentDay) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }

            Surface(
                onClick = { onJumpToDay(day.dayNumber) },
                shape = MaterialTheme.shapes.small,
                color = bgColor,
                border = BorderStroke(if (day.isCurrentDay) 1.5.dp else 0.7.dp, borderColor),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("heatmap_day_${day.dayNumber}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (day.isCompleted) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.day_short_format, day.dayNumber.toArabicDigits()),
                            tint = fgColor,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = day.dayNumber.toArabicDigits(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = fgColor
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayScheduleCard(
    day: DayPlanUiModel,
    onClick: () -> Unit,
    onOpenSurah: (Int) -> Unit,
    onToggleDayComplete: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (day.isCompleted) {
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.78f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (day.isCurrentDay) 1.2.dp else 0.8.dp,
            color = if (day.isCurrentDay) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("schedule_day_card_${day.dayNumber}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.day_short_format, day.dayNumber.toArabicDigits()),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (day.isCurrentDay) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.today_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${formatSurahCountArabic(day.surahs.size)} • ${formatAyahCountArabic(day.totalAyahs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onToggleDayComplete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("schedule_day_toggle_${day.dayNumber}")
                ) {
                    Icon(
                        imageVector = if (day.isCompleted) {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.RadioButtonUnchecked
                        },
                        contentDescription = stringResource(R.string.mark_day_complete),
                        tint = if (day.isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            if (day.surahs.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    day.surahs.forEach { surah ->
                        Surface(
                            onClick = { onOpenSurah(surah.surahNumber) },
                            shape = CircleShape,
                            color = if (surah.isCompleted) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = BorderStroke(
                                0.6.dp,
                                if (surah.isCompleted) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            modifier = Modifier.testTag("schedule_surah_chip_${surah.surahNumber}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (surah.isCompleted) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = surah.arabicName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (surah.isCompleted) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Subtle progress bar at bottom of day card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(day.completionRatio.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (day.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    }
}
