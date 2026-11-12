package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import coil3.request.ImageRequest

/** String form of a platform URI object (android.net.Uri / java.net.URI), else null. */
internal expect fun platformUriString(value: Any?): String?

/** A platform bitmap (android.graphics.Bitmap / java.awt.image.BufferedImage) as ImageBitmap, else null. */
internal expect fun platformBitmapAsImageBitmap(value: Any?): ImageBitmap?

/** Android: allowHardware(enabled); desktop: no-op. */
internal expect fun ImageRequest.Builder.allowHardwareCompat(enabled: Boolean): ImageRequest.Builder

/** Draws [painter] tinted with [color] (used where a slot takes a Painter but the icon must be tinted). */
@Composable
fun rememberTintedPainter(painter: Painter, color: Color): Painter =
    remember(painter, color) {
        object : Painter() {
            override val intrinsicSize
                get() = painter.intrinsicSize

            override fun DrawScope.onDraw() {
                with(painter) { draw(size, colorFilter = ColorFilter.tint(color)) }
            }
        }
    }
