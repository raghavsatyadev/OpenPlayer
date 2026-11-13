package io.github.raghavsatyadev.openplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.raghavsatyadev.openplayer.data.preferences.ThemePreset
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ColorSchemePair

val LocalOpenPlayerDarkTheme = staticCompositionLocalOf { false }
val LocalOpenPlayerAmoledTheme = staticCompositionLocalOf { false }
val LocalShowScrollbar = staticCompositionLocalOf { true }

@Composable
expect fun OpenPlayerStatusBarStyle(
    color: Color,
    useDarkIcons: Boolean = color.luminance() > 0.55f,
    navigationColor: Color? = null,
    useDarkNavigationIcons: Boolean = navigationColor
        ?.let { it.luminance() > 0.55f }
        ?: useDarkIcons
)

/** Wallpaper-based dynamic scheme where the platform has one (Android 12+), else null. */
@Composable
internal expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?

val DarkColorScheme = darkColorScheme(
    primary = OpenPlayerPurplePrimary,
    secondary = OpenPlayerPink,
    tertiary = OpenPlayerOrange,
    background = OpenPlayerPurpleDark,
    surface = OpenPlayerSurface,
    onPrimary = OpenPlayerWhite,
    onSecondary = OpenPlayerWhite,
    onTertiary = OpenPlayerWhite,
    onBackground = OpenPlayerWhite,
    onSurface = OpenPlayerLightPurple,
    error = Color(0xFFFF5252),
    onError = OpenPlayerWhite
)

val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = OpenPlayerWhite,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = OpenPlayerPink,
    onSecondary = OpenPlayerWhite,
    secondaryContainer = OpenPlayerPink.copy(alpha = 0.15f),
    onSecondaryContainer = OpenPlayerPink.copy(alpha = 0.85f),
    tertiary = OpenPlayerOrange,
    onTertiary = OpenPlayerBlack,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.6f),
    surfaceTint = LightPrimary,
    error = Color(0xFFD32F2F),
    onError = OpenPlayerWhite
)

fun isAmoledThemePreset(preset: String): Boolean = preset == ThemePreset.AMOLED

/** Keep the source palette's accents and content colors, replacing only its surface hierarchy. */
fun ColorScheme.withAmoledSurfaces(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF242424),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF242424),
    surfaceVariant = Color(0xFF242424),
)

/**
 * AMOLED always uses a dark palette, even when the app is set to light mode. The accents come
 * from [colorSchemePairOverride] (the app-wide now-playing palette) when the user enabled it,
 * otherwise from the wallpaper/dynamic dark scheme.
 */
internal fun resolveAmoledColorScheme(
    colorSchemePairOverride: ColorSchemePair?,
    wallpaperDarkScheme: () -> ColorScheme,
): ColorScheme = (colorSchemePairOverride?.dark ?: wallpaperDarkScheme()).withAmoledSurfaces()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OpenPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorSchemePairOverride: ColorSchemePair? = null,
    themePreset: String = ThemePreset.DEFAULT,
    content: @Composable () -> Unit
) {
    val isAmoled = isAmoledThemePreset(themePreset)
    val dynamicScheme = if (isAmoled || colorSchemePairOverride == null) {
        platformDynamicColorScheme(darkTheme = isAmoled || darkTheme)
    } else {
        null
    }
    val finalColorScheme = when {
        isAmoled -> remember(dynamicScheme, colorSchemePairOverride) {
            resolveAmoledColorScheme(colorSchemePairOverride) { dynamicScheme ?: DarkColorScheme }
        }
        colorSchemePairOverride == null && dynamicScheme != null -> dynamicScheme
        colorSchemePairOverride != null -> {
            if (darkTheme) colorSchemePairOverride.dark else colorSchemePairOverride.light
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    OpenPlayerStatusBarStyle(
        color = finalColorScheme.background,
        navigationColor = finalColorScheme.background
    )

    CompositionLocalProvider(
        LocalOpenPlayerDarkTheme provides (darkTheme || isAmoled),
        LocalOpenPlayerAmoledTheme provides isAmoled
    ) {
        MaterialTheme(
            colorScheme = finalColorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
