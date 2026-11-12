package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.presentation.model.RecentlyPlayedSongUiModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.HistoryPicksState
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SeedPickMode
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ThemeStateHolder
import kotlinx.collections.immutable.toImmutableList

/** What the history sections need from the Home screen. */
class HistoryPickActions(
    val play: (songs: List<Song>, start: Song, queueName: String) -> Unit,
    val onSongLongClick: (Song) -> Unit,
    val selectYear: (Int) -> Unit,
    val selectMode: (SeedPickMode) -> Unit,
)

/**
 * Home sections from your own history: songs for this time of day, songs like the current track,
 * and what you played around today in past years. Each shows only when it has songs.
 */
fun LazyListScope.historyPickSections(
    state: HistoryPicksState,
    actions: HistoryPickActions,
    themeStateHolder: ThemeStateHolder,
    currentSongId: String?,
) {
    if (state.timeOfDay.isNotEmpty()) {
        item(key = "history_time_of_day", contentType = "history_pick_section") {
            val title = stringResource(R.string.history_picks_time_of_day)
            PickSection(
                title = title,
                songs = state.timeOfDay,
                actions = actions,
                themeStateHolder = themeStateHolder,
                currentSongId = currentSongId,
                modifier = Modifier.animateItem(),
            )
        }
    }

    val mode = state.selectedMode
    val seed = state.seed
    val seedSongs = mode?.let(state.seedPicks::get)
    if (seed != null && mode != null && !seedSongs.isNullOrEmpty()) {
        item(key = "history_more_like", contentType = "history_pick_section") {
            PickSection(
                title = stringResource(R.string.history_picks_more_like, seed.title),
                songs = seedSongs,
                actions = actions,
                themeStateHolder = themeStateHolder,
                currentSongId = currentSongId,
                modifier = Modifier.animateItem(),
                chips = {
                    ChoiceChips(
                        options = state.seedPicks.keys.toList(),
                        selected = mode,
                        label = { stringResource(it.labelRes) },
                        onSelect = actions.selectMode,
                    )
                },
            )
        }
    }

    val year = state.selectedYear
    val yearSongs = year?.let(state.lostMemories::get)
    if (year != null && !yearSongs.isNullOrEmpty()) {
        item(key = "history_lost_memories", contentType = "history_pick_section") {
            PickSection(
                title = stringResource(R.string.history_picks_lost_memories),
                songs = yearSongs,
                actions = actions,
                themeStateHolder = themeStateHolder,
                currentSongId = currentSongId,
                modifier = Modifier.animateItem(),
                chips = if (state.lostMemories.size > 1) {
                    {
                        ChoiceChips(
                            options = state.lostMemories.keys.toList(),
                            selected = year,
                            label = { it.toString() },
                            onSelect = actions.selectYear,
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun PickSection(
    title: String,
    songs: List<Song>,
    actions: HistoryPickActions,
    themeStateHolder: ThemeStateHolder,
    currentSongId: String?,
    modifier: Modifier,
    chips: (@Composable () -> Unit)? = null,
) {
    val items = remember(songs) { songs.map { RecentlyPlayedSongUiModel(it, 0L) }.toImmutableList() }
    RecentlyPlayedSection(
        songs = items,
        onSongClick = { actions.play(songs, it, title) },
        onSongLongClick = actions.onSongLongClick,
        onOpenAllClick = { songs.firstOrNull()?.let { actions.play(songs, it, title) } },
        themeStateHolder = themeStateHolder,
        currentSongId = currentSongId,
        contentPadding = PaddingValues(start = 8.dp, end = 24.dp),
        modifier = modifier,
        title = title,
        actionIconRes = R.drawable.rounded_play_arrow_24,
        actionDescription = stringResource(R.string.history_picks_play_all),
        minSongs = 1,
        headerContent = chips,
        animateChanges = true,
    )
}

@Composable
private fun <T> ChoiceChips(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

private val SeedPickMode.labelRes: Int
    get() = when (this) {
        SeedPickMode.ALONGSIDE -> R.string.history_picks_mode_alongside
        SeedPickMode.TIME_RANGE -> R.string.history_picks_mode_time_range
        SeedPickMode.DISCOVERED -> R.string.history_picks_mode_discovered
        SeedPickMode.ERA -> R.string.history_picks_mode_era
    }
