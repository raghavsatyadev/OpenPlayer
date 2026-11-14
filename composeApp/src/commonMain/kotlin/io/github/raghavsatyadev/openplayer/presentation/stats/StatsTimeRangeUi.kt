package io.github.raghavsatyadev.openplayer.presentation.stats

import io.github.raghavsatyadev.openplayer.data.stats.StatsTimeRange
import io.github.raghavsatyadev.openplayer.resources.*
import org.jetbrains.compose.resources.StringResource

fun StatsTimeRange.displayNameRes(): StringResource = when (this) {
    StatsTimeRange.DAY -> Res.string.presentation_batch_g_stats_range_today
    StatsTimeRange.WEEK -> Res.string.presentation_batch_g_stats_range_week_to_date
    StatsTimeRange.MONTH -> Res.string.presentation_batch_g_stats_range_month_to_date
    StatsTimeRange.YEAR -> Res.string.presentation_batch_g_stats_range_year_to_date
    StatsTimeRange.ALL -> Res.string.presentation_batch_g_stats_range_all_time
}
