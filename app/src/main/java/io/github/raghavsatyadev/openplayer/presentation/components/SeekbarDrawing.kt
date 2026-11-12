package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Draws [waveform] (values 0..1) as rounded bars across the track. Bars left of [progress] use
 * [playedColor]. [reveal] (0..1) grows the bars in from the left with a short stagger.
 */
internal fun DrawScope.drawWaveformBars(
    waveform: FloatArray,
    reveal: Float,
    progress: Float,
    trackStart: Float,
    trackWidth: Float,
    maxHeight: Float,
    playedColor: Color,
    unplayedColor: Color,
) {
    val n = waveform.size
    if (n == 0 || trackWidth <= 0f || reveal <= 0f) return
    val pitch = trackWidth / n
    val barWidth = (pitch * 0.62f).coerceAtLeast(1f)
    val minHeight = barWidth
    val centerY = size.height / 2f
    val progressX = trackStart + trackWidth * progress
    for (i in 0 until n) {
        val stagger = (reveal * 1.6f - (i.toFloat() / n) * 0.6f).coerceIn(0f, 1f)
        if (stagger <= 0f) continue
        val h = (minHeight + (maxHeight - minHeight) * waveform[i].coerceIn(0f, 1f)) * stagger
        val cx = trackStart + pitch * (i + 0.5f)
        drawRoundRect(
            color = if (cx <= progressX) playedColor else unplayedColor,
            topLeft = Offset(cx - barWidth / 2f, centerY - h / 2f),
            size = Size(barWidth, h),
            cornerRadius = CornerRadius(barWidth / 2f),
        )
    }
}

/** Small tick with a dot on top for every chapter fraction (0..1). */
internal fun DrawScope.drawChapterMarkers(
    markers: List<Float>,
    trackStart: Float,
    trackWidth: Float,
    color: Color,
    dotRadius: Float,
) {
    if (markers.isEmpty() || trackWidth <= 0f) return
    val top = dotRadius + 1f
    val tickBottom = top + dotRadius * 3f
    for (m in markers) {
        val x = trackStart + trackWidth * m.coerceIn(0f, 1f)
        drawLine(color, Offset(x, top), Offset(x, tickBottom), strokeWidth = dotRadius * 0.6f)
        drawCircle(color, dotRadius, Offset(x, top))
    }
}
