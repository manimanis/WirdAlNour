package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val AmiriQuranFontFamily = FontFamily(
    Font(R.font.amiri_quran, FontWeight.Normal),
    Font(R.font.amiri_quran, FontWeight.Medium),
    Font(R.font.amiri_quran, FontWeight.Bold)
)

val AmiriFontFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri, FontWeight.Medium),
    Font(R.font.amiri, FontWeight.Bold)
)

val ScheherazadeNewFontFamily = FontFamily(
    Font(R.font.scheherazade_new, FontWeight.Normal),
    Font(R.font.scheherazade_new, FontWeight.Medium),
    Font(R.font.scheherazade_new, FontWeight.Bold)
)

val NotoNaskhArabicFontFamily = FontFamily(
    Font(R.font.noto_naskh_arabic, FontWeight.Normal),
    Font(R.font.noto_naskh_arabic, FontWeight.Medium),
    Font(R.font.noto_naskh_arabic, FontWeight.Bold)
)

val LateefFontFamily = FontFamily(
    Font(R.font.lateef, FontWeight.Normal),
    Font(R.font.lateef, FontWeight.Medium),
    Font(R.font.lateef, FontWeight.Bold)
)

val IbmPlexArabicFontFamily = FontFamily(
    Font(R.font.ibm_plex_sans_arabic, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_arabic, FontWeight.Bold)
)

enum class QuranFontOption(
    val storageKey: String,
    val arabicName: String,
    val arabicDescription: String,
    val fontFamily: FontFamily,
    val lineHeightMultiplier: Float
) {
    AMIRI_QURAN(
        storageKey = "amiri_quran",
        arabicName = "أميري للقرآن",
        arabicDescription = "خط مصحفي كلاسيكي فائق الجمال",
        fontFamily = AmiriQuranFontFamily,
        lineHeightMultiplier = 2.28f
    ),
    SCHEHERAZADE_NEW(
        storageKey = "scheherazade_new",
        arabicName = "شهرزاد",
        arabicDescription = "نسخ تقليدي شديد الوضوح للتشكيل",
        fontFamily = ScheherazadeNewFontFamily,
        lineHeightMultiplier = 2.16f
    ),
    AMIRI(
        storageKey = "amiri",
        arabicName = "أميري",
        arabicDescription = "نسخ متوازن يجمع بين الأناقة والبساطة",
        fontFamily = AmiriFontFamily,
        lineHeightMultiplier = 2.08f
    ),
    NOTO_NASKH(
        storageKey = "noto_naskh_arabic",
        arabicName = "نسخ نوتو",
        arabicDescription = "عصري ومريح جداً للقراءة الطويلة",
        fontFamily = NotoNaskhArabicFontFamily,
        lineHeightMultiplier = 2.00f
    ),
    LATEEF(
        storageKey = "lateef",
        arabicName = "لطيف",
        arabicDescription = "حروف ناعمة ومفتوحة مريحة للعين",
        fontFamily = LateefFontFamily,
        lineHeightMultiplier = 2.08f
    );

    companion object {
        fun fromStorageKey(key: String?): QuranFontOption =
            entries.firstOrNull { it.storageKey == key } ?: AMIRI_QURAN
    }
}

val WirdTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 48.sp
    ),
    displayMedium = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 40.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle(
        fontFamily = IbmPlexArabicFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = IbmPlexArabicFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = IbmPlexArabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = IbmPlexArabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = IbmPlexArabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = IbmPlexArabicFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = IbmPlexArabicFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = IbmPlexArabicFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)
