package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AyahBookmarkEntity
import com.example.ui.theme.AmiriFontFamily
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.WirdSeedData
import com.example.data.local.entity.SurahPlanEntity
import com.example.util.formatAyahCountArabic
import com.example.util.formatPercentArabic
import com.example.util.toArabicDigits
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Geometric 8-point Rub el Hizb medallion framing the Arabic surah number.
 */
@Composable
fun SurahNumberMedallion(
    surahNumber: Int,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val borderColor = if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    val fillColor = if (isCompleted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    val textColor = if (isCompleted) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = this.size.minDimension * 0.68f
            val topLeft = Offset((this.size.width - side) / 2f, (this.size.height - side) / 2f)
            val squareSize = Size(side, side)

            drawRect(color = fillColor, topLeft = topLeft, size = squareSize)
            rotate(degrees = 45f) {
                drawRect(color = fillColor, topLeft = topLeft, size = squareSize)
                drawRect(
                    color = borderColor,
                    topLeft = topLeft,
                    size = squareSize,
                    style = Stroke(width = 1.4.dp.toPx())
                )
            }
            drawRect(
                color = borderColor,
                topLeft = topLeft,
                size = squareSize,
                style = Stroke(width = 1.4.dp.toPx())
            )
        }
        Text(
            text = surahNumber.toArabicDigits(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}

/**
 * Astrolabe-inspired circular progress gauge for visual tracking.
 */
@Composable
fun WirdProgressArc(
    progress: Float,
    centerLabel: String,
    centerSubLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 152.dp,
    strokeWidth: Dp = 11.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.secondary,
    contentTextColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "wird_progress_arc"
    )
    val progressDesc = stringResource(R.string.progress_ring_cd)

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "$progressDesc ${formatPercentArabic(animatedProgress)}" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f + 4.dp.toPx()
            val arcDiameter = this.size.minDimension - inset * 2f
            val topLeft = Offset((this.size.width - arcDiameter) / 2f, (this.size.height - arcDiameter) / 2f)
            val arcSize = Size(arcDiameter, arcDiameter)

            // Subtle inner dashed manuscript ring
            drawCircle(
                color = accentColor.copy(alpha = 0.28f),
                radius = arcDiameter / 2f - strokePx * 0.95f,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                )
            )

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active arc
            if (animatedProgress > 0.005f) {
                val sweep = animatedProgress * 360f
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Golden jewel dot at arc tip
                val angleRad = ((-90f + sweep) * PI / 180.0).toFloat()
                val radius = arcDiameter / 2f
                val tipX = center.x + radius * cos(angleRad)
                val tipY = center.y + radius * sin(angleRad)
                drawCircle(
                    color = accentColor,
                    radius = strokePx * 0.42f,
                    center = Offset(tipX, tipY)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.displayMedium,
                color = contentTextColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = centerSubLabel,
                style = MaterialTheme.typography.labelMedium,
                color = contentTextColor.copy(alpha = 0.72f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun OrnamentalDivider(modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.outline
    val goldColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 0.8.dp, color = lineColor)
        Canvas(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .size(10.dp)
        ) {
            rotate(45f) {
                val s = size.minDimension * 0.65f
                drawRect(
                    color = goldColor,
                    topLeft = Offset((size.width - s) / 2f, (size.height - s) / 2f),
                    size = Size(s, s)
                )
            }
        }
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 0.8.dp, color = lineColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahDetailSheet(
    surah: SurahPlanEntity,
    onDismiss: () -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    onMoveToDay: (Int) -> Unit,
    onSaveNote: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var noteText by remember(surah.surahNumber, surah.readingNote) {
        mutableStateOf(surah.readingNote)
    }
    val hasNoteChanged = noteText.trim() != surah.readingNote.trim()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SurahNumberMedallion(
                        surahNumber = surah.surahNumber,
                        isCompleted = surah.isCompleted,
                        size = 48.dp
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.surah_prefix, surah.arabicName),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${surah.revelationType} • ${formatAyahCountArabic(surah.ayahCount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_surah_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close_cd)
                    )
                }
            }

            // Metadata chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetadataPill(
                    text = stringResource(R.string.juz_format, surah.juzNumber.toArabicDigits()),
                    modifier = Modifier.weight(1f)
                )
                MetadataPill(
                    text = stringResource(R.string.page_format, surah.startPage.toArabicDigits()),
                    modifier = Modifier.weight(1f)
                )
                MetadataPill(
                    text = stringResource(R.string.day_short_format, surah.dayNumber.toArabicDigits()),
                    modifier = Modifier.weight(1f)
                )
            }

            // Theme summary box
            if (surah.themeSummary.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = surah.themeSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Primary read completion button
            Button(
                onClick = { onToggleCompleted(!surah.isCompleted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sheet_toggle_surah_button"),
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
                        if (surah.isCompleted) R.string.surah_completed_action else R.string.surah_mark_read_action
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            OrnamentalDivider()

            // Reassign day within 14-day plan
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.assign_day_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilledTonalIconButton(
                            onClick = { onMoveToDay(surah.dayNumber - 1) },
                            enabled = surah.dayNumber > 1,
                            modifier = Modifier.testTag("move_surah_prev_day_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.previous_day_cd)
                            )
                        }
                        Text(
                            text = stringResource(R.string.day_label_format, surah.dayNumber.toArabicDigits()),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FilledTonalIconButton(
                            onClick = { onMoveToDay(surah.dayNumber + 1) },
                            enabled = surah.dayNumber < WirdSeedData.TOTAL_DAYS,
                            modifier = Modifier.testTag("move_surah_next_day_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.next_day_cd)
                            )
                        }
                    }
                }
            }

            // Personal reflection note
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.reflection_note_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("surah_reflection_note_input"),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.reflection_note_placeholder),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    shape = MaterialTheme.shapes.medium
                )
                if (hasNoteChanged) {
                    OutlinedButton(
                        onClick = { onSaveNote(noteText) },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("save_reflection_note_button"),
                        shape = CircleShape
                    ) {
                        Text(text = stringResource(R.string.save_note_action))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

