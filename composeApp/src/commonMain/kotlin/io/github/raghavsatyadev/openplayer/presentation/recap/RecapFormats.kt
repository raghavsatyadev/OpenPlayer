package io.github.raghavsatyadev.openplayer.presentation.recap

import androidx.compose.runtime.Composable
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.utils.formatListeningDurationCompact
import java.util.concurrent.TimeUnit

// The recap's single say on how listening time reads. Slides and the share card go through
// these helpers instead of picking a Formats.kt style each, so they cannot drift apart again.

/**
 * A listening duration as every recap slide writes it: the compact style the Stats screen
 * uses ("2h 19m"), so a recap figure reads exactly like the same figure in the Stats year view.
 */
@Composable
internal fun formatRecapDuration(milliseconds: Long): String =
    formatListeningDurationCompact(milliseconds)

/**
 * Whole minutes of listening, as the minutes slide counts up to and the share card shows.
 * Rounds down like [formatRecapDuration], so "1h 20m" elsewhere is 80 here, never 81.
 */
internal fun recapListenedMinutes(milliseconds: Long): Long = TimeUnit.MILLISECONDS.toMinutes(milliseconds)

/**
 * What a chart of listening durations reads out to screen readers in place of its bars: every
 * entry of [values] with any listening as "label: duration", in chart order, so no amount is
 * only visible. [labels] names the entries of [values] position by position.
 */
@Composable
internal fun recapChartEntries(labels: List<String>, values: List<Long>): List<String> =
    labels.zip(values).mapNotNull { (label, milliseconds) ->
        if (milliseconds <= 0L) return@mapNotNull null
        stringResource(Res.string.recap_cd_chart_entry, label, formatRecapDuration(milliseconds))
    }
