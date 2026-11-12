package io.github.raghavsatyadev.openplayer.presentation.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.request.ImageRequest
import coil3.request.allowHardware

internal actual fun platformUriString(value: Any?): String? = (value as? Uri)?.toString()

internal actual fun platformBitmapAsImageBitmap(value: Any?): ImageBitmap? =
    (value as? Bitmap)?.asImageBitmap()

internal actual fun ImageRequest.Builder.allowHardwareCompat(enabled: Boolean): ImageRequest.Builder =
    allowHardware(enabled)
