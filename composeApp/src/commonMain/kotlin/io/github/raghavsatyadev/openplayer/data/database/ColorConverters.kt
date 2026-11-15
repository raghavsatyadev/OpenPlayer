package io.github.raghavsatyadev.openplayer.data.database

import androidx.compose.ui.graphics.Color

/** Parses "#RRGGBB" or "#AARRGGBB"; anything else gives black. */
fun String.toComposeColor(): Color {
    if (length != 7 && length != 9 || this[0] != '#') return Color.Black
    val value = substring(1).toLongOrNull(16) ?: return Color.Black
    val argb = if (length == 7) 0xFF000000L or value else value
    return Color(argb.toInt())
}
