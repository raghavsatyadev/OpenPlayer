package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import coil3.request.ImageRequest
import java.awt.image.BufferedImage
import java.net.URI

internal actual fun platformUriString(value: Any?): String? = (value as? URI)?.toString()

internal actual fun platformBitmapAsImageBitmap(value: Any?): ImageBitmap? =
    (value as? BufferedImage)?.toComposeImageBitmap()

internal actual fun ImageRequest.Builder.allowHardwareCompat(enabled: Boolean): ImageRequest.Builder = this
