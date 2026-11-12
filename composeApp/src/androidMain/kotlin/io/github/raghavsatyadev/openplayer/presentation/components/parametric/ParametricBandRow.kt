package io.github.raghavsatyadev.openplayer.presentation.components.parametric

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricBand
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricBandType
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource

@Composable
internal fun bandTypeLabel(type: ParametricBandType): String = stringResource(
    when (type) {
        ParametricBandType.PEAK -> R.string.parametric_eq_band_type_peak
        ParametricBandType.LOW_SHELF -> R.string.parametric_eq_band_type_low_shelf
        ParametricBandType.HIGH_SHELF -> R.string.parametric_eq_band_type_high_shelf
    },
)

/** One editable band: type dropdown, enable switch, remove, and frequency / gain / Q sliders. */
@Composable
fun ParametricBandRow(
    number: Int,
    band: ParametricBand,
    onChange: (ParametricBand) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.parametric_eq_band_number, number),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.width(72.dp),
                )
                var menuOpen by remember { mutableStateOf(false) }
                Column(Modifier.weight(1f)) {
                    TextButton(onClick = { menuOpen = true }) {
                        Text(bandTypeLabel(band.type))
                        Icon(ImageVector.vectorResource(R.drawable.rounded_expand_more_24), null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        ParametricBandType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(bandTypeLabel(type)) },
                                onClick = {
                                    menuOpen = false
                                    onChange(band.copy(type = type))
                                },
                            )
                        }
                    }
                }
                Switch(
                    checked = band.enabled,
                    onCheckedChange = { onChange(band.copy(enabled = it)) },
                    modifier = Modifier.padding(end = 4.dp),
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.rounded_delete_24),
                        stringResource(Res.string.parametric_eq_band_remove_cd),
                    )
                }
            }
            LabeledSlider(
                label = stringResource(Res.string.parametric_eq_frequency),
                valueText = stringResource(Res.string.parametric_eq_hz_value, formatHz(band.frequencyHz)),
                value = (log10(band.frequencyHz / ParametricBand.MIN_FREQ_HZ) / LOG_FREQ_SPAN).toFloat(),
                valueRange = 0f..1f,
                onValueChange = { onChange(band.copy(frequencyHz = (ParametricBand.MIN_FREQ_HZ * 10.0.pow(it * LOG_FREQ_SPAN)).toFloat())) },
            )
            LabeledSlider(
                label = stringResource(Res.string.parametric_eq_gain),
                valueText = stringResource(Res.string.parametric_eq_db_value, String.format(Locale.US, "%+.1f", band.gainDb)),
                value = band.gainDb,
                valueRange = -ParametricBand.MAX_GAIN_DB..ParametricBand.MAX_GAIN_DB,
                onValueChange = { onChange(band.copy(gainDb = Math.round(it * 10f) / 10f)) },
            )
            LabeledSlider(
                label = stringResource(Res.string.parametric_eq_q),
                valueText = String.format(Locale.US, "%.2f", band.q),
                value = (log10(band.q / ParametricBand.MIN_Q) / LOG_Q_SPAN).toFloat(),
                valueRange = 0f..1f,
                onValueChange = { onChange(band.copy(q = (ParametricBand.MIN_Q * 10.0.pow(it * LOG_Q_SPAN)).toFloat())) },
            )
        }
    }
}

@Composable
internal fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(72.dp))
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
        )
        Text(
            valueText,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(72.dp),
            maxLines = 1,
        )
    }
}

private val LOG_FREQ_SPAN = log10((ParametricBand.MAX_FREQ_HZ / ParametricBand.MIN_FREQ_HZ).toDouble())
private val LOG_Q_SPAN = log10((ParametricBand.MAX_Q / ParametricBand.MIN_Q).toDouble())

internal fun formatHz(hz: Float): String =
    if (hz >= 1000f) String.format(Locale.US, "%.2fk", hz / 1000f) else String.format(Locale.US, "%.0f", hz)
