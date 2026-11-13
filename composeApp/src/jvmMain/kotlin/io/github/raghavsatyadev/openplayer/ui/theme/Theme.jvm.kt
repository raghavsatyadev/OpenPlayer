package io.github.raghavsatyadev.openplayer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Desktop windows have no system bars to tint. */
@Composable
actual fun OpenPlayerStatusBarStyle(
    color: Color,
    useDarkIcons: Boolean,
    navigationColor: Color?,
    useDarkNavigationIcons: Boolean
) = Unit

@Composable
internal actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? = null
