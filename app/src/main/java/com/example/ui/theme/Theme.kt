package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LightWirdColorScheme = lightColorScheme(
    primary = DeepEmerald,
    onPrimary = Color.White,
    primaryContainer = SageContainer,
    onPrimaryContainer = OnSageContainer,
    secondary = AntiqueGold,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = DeepEmeraldDark,
    onTertiary = Color.White,
    background = ParchmentBackground,
    onBackground = WarmObsidian,
    surface = PaperSurface,
    onSurface = WarmObsidian,
    surfaceVariant = SandSurfaceVariant,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = PaperSurface,
    surfaceContainerLow = ParchmentBackground,
    surfaceContainer = SandSurfaceContainer,
    surfaceContainerHigh = SandSurfaceContainerHigh,
    outline = HairlineOutline,
    outlineVariant = HairlineOutlineVariant,
    error = CalmError,
    onError = Color.White,
    errorContainer = CalmErrorContainer
)

private val DarkWirdColorScheme = darkColorScheme(
    primary = SoftNightBlue,
    onPrimary = OnSoftNightBlue,
    primaryContainer = DarkBlueContainer,
    onPrimaryContainer = OnDarkBlueContainer,
    secondary = LuminousGold,
    onSecondary = OnLuminousGold,
    secondaryContainer = DarkGoldContainer,
    onSecondaryContainer = OnDarkGoldContainer,
    tertiary = SoftNightBlue,
    onTertiary = OnSoftNightBlue,
    background = NightBackground,
    onBackground = SoftIvory,
    surface = NightSurface,
    onSurface = SoftIvory,
    surfaceVariant = NightSurfaceContainer,
    onSurfaceVariant = MutedSand,
    surfaceContainerLowest = NightBackground,
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightSurfaceContainer,
    surfaceContainerHigh = NightSurfaceContainerHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = Color(0xFFE58A8A),
    onError = Color(0xFF4A1010)
)

private val WirdShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun WirdAlNoorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkWirdColorScheme else LightWirdColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WirdTypography,
            shapes = WirdShapes,
            content = content
        )
    }
}
