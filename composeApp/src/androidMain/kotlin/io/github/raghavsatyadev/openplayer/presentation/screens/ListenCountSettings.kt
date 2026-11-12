package io.github.raghavsatyadev.openplayer.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.raghavsatyadev.openplayer.data.history.ListenCountRule
import kotlin.math.roundToInt
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource

private const val STEP = 5

/** Minimum listen before a play goes into the history: seconds or a share of the track. */
@Composable
fun ListenCountSettings(
    rule: ListenCountRule,
    onMinSecondsChange: (Int) -> Unit,
    onMinPercentageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var seconds by remember(rule.minSeconds) { mutableFloatStateOf(rule.minSeconds.toFloat()) }
    var percentage by remember(rule.minPercentage) { mutableFloatStateOf(rule.minPercentage.toFloat()) }
    val secondsRange = ListenCountRule.SECONDS_RANGE
    val percentageRange = ListenCountRule.PERCENTAGE_RANGE

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SliderSettingsItem(
            label = stringResource(Res.string.setcat_listen_count_seconds),
            value = seconds,
            valueRange = secondsRange.first.toFloat()..secondsRange.last.toFloat(),
            steps = (secondsRange.last - secondsRange.first) / STEP - 1,
            onValueChange = { seconds = it },
            onValueChangeFinished = { onMinSecondsChange(seconds.roundToInt()) },
            valueText = { "${it.roundToInt()}s" }
        )
        SliderSettingsItem(
            label = stringResource(Res.string.setcat_listen_count_percentage),
            value = percentage,
            valueRange = percentageRange.first.toFloat()..percentageRange.last.toFloat(),
            steps = (percentageRange.last - percentageRange.first) / STEP - 1,
            onValueChange = { percentage = it },
            onValueChangeFinished = { onMinPercentageChange(percentage.roundToInt()) },
            valueText = { "${it.roundToInt()}%" }
        )
        Text(
            text = stringResource(Res.string.setcat_listen_count_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}
