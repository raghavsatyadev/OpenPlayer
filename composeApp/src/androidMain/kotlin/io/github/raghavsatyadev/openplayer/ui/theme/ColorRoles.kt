package io.github.raghavsatyadev.openplayer.ui.theme

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.scale
import kotlin.math.max
import kotlin.math.roundToInt

private val extractedColorCache = LruCache<Int, Color>(32)

fun clearExtractedColorCache() {
    extractedColorCache.evictAll()
}

fun extractSeedColor(
    bitmap: Bitmap,
    config: ColorExtractionConfig = ColorExtractionConfig()
): Color {
    val cacheKey = 31 * bitmap.hashCode() + config.hashCode()
    extractedColorCache.get(cacheKey)?.let { return it }

    val workingBitmap = resizeForExtraction(bitmap, config.downscaleMaxDimension)

    val seedColor = runCatching {
        val pixels = IntArray(workingBitmap.width * workingBitmap.height)
        workingBitmap.getPixels(
            pixels,
            0,
            workingBitmap.width,
            0,
            0,
            workingBitmap.width,
            workingBitmap.height
        )

        Color(selectSeedColorArgbFromPixels(pixels, config))
    }.getOrElse { DarkColorScheme.primary }

    extractedColorCache.put(cacheKey, seedColor)
    if (workingBitmap !== bitmap) {
        workingBitmap.recycle()
    }
    return seedColor
}

private fun resizeForExtraction(bitmap: Bitmap, maxDimension: Int): Bitmap {
    if (maxDimension <= 0) return bitmap
    if (bitmap.width <= maxDimension && bitmap.height <= maxDimension) return bitmap
    val scale = maxDimension.toFloat() / max(bitmap.width, bitmap.height).toFloat()
    return bitmap.scale(
        width = (bitmap.width * scale).roundToInt().coerceAtLeast(1),
        height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
    )
}
