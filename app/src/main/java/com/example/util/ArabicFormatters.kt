package com.example.util

import kotlin.math.roundToInt

private val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

fun Int.toArabicDigits(): String = toString().toArabicDigits()

fun String.toArabicDigits(): String {
    val sb = StringBuilder(length)
    for (ch in this) {
        if (ch in '0'..'9') {
            sb.append(arabicDigits[ch - '0'])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

fun formatSurahCountArabic(count: Int): String = when (count) {
    0 -> "لا توجد سور"
    1 -> "سورة واحدة"
    2 -> "سورتان"
    in 3..10 -> "${count.toArabicDigits()} سور"
    else -> "${count.toArabicDigits()} سورة"
}

fun formatAyahCountArabic(count: Int): String = when (count) {
    1 -> "آية واحدة"
    2 -> "آيتان"
    in 3..10 -> "${count.toArabicDigits()} آيات"
    else -> "${count.toArabicDigits()} آية"
}

fun formatDayCountArabic(count: Int): String = when (count) {
    0 -> "٠ يوم"
    1 -> "يوم واحد"
    2 -> "يومان"
    in 3..10 -> "${count.toArabicDigits()} أيام"
    else -> "${count.toArabicDigits()} يوماً"
}

fun formatMinutesArabic(minutes: Int): String {
    val safeMinutes = minutes.coerceAtLeast(1)
    return when (safeMinutes) {
        1 -> "دقيقة واحدة"
        2 -> "دقيقتان"
        in 3..10 -> "${safeMinutes.toArabicDigits()} دقائق"
        else -> "${safeMinutes.toArabicDigits()} دقيقة"
    }
}

fun formatTimeArabic(hour24: Int, minute: Int): String {
    val period = when {
        hour24 < 12 -> "صباحاً"
        hour24 == 12 -> "ظهراً"
        else -> "مساءً"
    }
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    val hh = hour12.toString().padStart(2, '0').toArabicDigits()
    val mm = minute.toString().padStart(2, '0').toArabicDigits()
    return "$hh:$mm $period"
}

fun formatPercentArabic(progress: Float): String {
    val pct = (progress.coerceIn(0f, 1f) * 100f).roundToInt()
    return "${pct.toArabicDigits()}٪"
}
