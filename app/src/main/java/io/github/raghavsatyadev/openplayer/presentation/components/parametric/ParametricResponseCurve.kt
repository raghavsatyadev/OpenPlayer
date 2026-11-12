package io.github.raghavsatyadev.openplayer.presentation.components.parametric

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricEqProfile
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricResponse
import kotlin.math.log10
import kotlin.math.pow

private const val POINTS = 96
private const val RANGE_DB = 15f

/** Response curve: log frequency axis 20 Hz to 20 kHz, plus or minus 15 dB, drawn from combinedGainDb. */
@Composable
fun ParametricResponseCurve(
    profile: ParametricEqProfile,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val target = remember(profile) {
        FloatArray(POINTS) { i ->
            val f = ParametricResponse.MIN_HZ * (ParametricResponse.MAX_HZ / ParametricResponse.MIN_HZ).pow(i / (POINTS - 1.0))
            ParametricResponse.combinedGainDb(profile, f).toFloat()
        }
    }
    val progress = remember { Animatable(1f) }
    val from = remember { arrayOf(target) }
    val current = remember { arrayOf(target) }
    LaunchedEffect(target) {
        from[0] = current[0]
        progress.snapTo(0f)
        progress.animateTo(1f, tween(220))
    }

    val grid = MaterialTheme.colorScheme.outlineVariant
    val zeroLine = MaterialTheme.colorScheme.outline
    val line = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)
    val strokePx = with(LocalDensity.current) { 2.5.dp.toPx() }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val left = 30.dp.toPx()
        val bottomPad = 16.dp.toPx()
        val w = size.width - left
        val h = size.height - bottomPad
        fun yOf(db: Float) = h * (1f - (db.coerceIn(-RANGE_DB, RANGE_DB) + RANGE_DB) / (2 * RANGE_DB))
        fun xOf(hz: Double) = left + w * (log10(hz / ParametricResponse.MIN_HZ) / 3.0).toFloat()

        for (db in -15..15 step 5) {
            val y = yOf(db.toFloat())
            drawLine(if (db == 0) zeroLine else grid, Offset(left, y), Offset(size.width, y), if (db == 0) 1.5.dp.toPx() else 1f)
            val text = if (db > 0) "+$db" else "$db"
            val layout = measurer.measure(text, labelStyle)
            drawText(layout, topLeft = Offset(left - layout.size.width - 4.dp.toPx(), y - layout.size.height / 2f))
        }
        listOf(100.0 to "100", 1000.0 to "1k", 10000.0 to "10k").forEach { (hz, label) ->
            val x = xOf(hz)
            drawLine(grid, Offset(x, 0f), Offset(x, h), 1f)
            val layout = measurer.measure(label, labelStyle)
            drawText(layout, topLeft = Offset(x - layout.size.width / 2f, h + 2.dp.toPx()))
        }

        val t = progress.value
        val shown = FloatArray(POINTS) { i -> from[0][i] + (target[i] - from[0][i]) * t }
        current[0] = shown
        val path = Path()
        shown.forEachIndexed { i, db ->
            val x = left + w * i / (POINTS - 1)
            if (i == 0) path.moveTo(x, yOf(db)) else path.lineTo(x, yOf(db))
        }
        val fill = Path().apply {
            addPath(path)
            lineTo(size.width, yOf(0f))
            lineTo(left, yOf(0f))
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(line.copy(alpha = 0.28f), Color.Transparent)))
        drawPath(path, line, style = Stroke(width = strokePx, cap = StrokeCap.Round))
    }
}
