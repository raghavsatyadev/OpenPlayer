package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.raghavsatyadev.openplayer.data.history.recommend.HistoryIndex
import io.github.raghavsatyadev.openplayer.data.history.recommend.HistoryPicks
import io.github.raghavsatyadev.openplayer.data.history.recommend.HistoryPicksRepository
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** How the "more like" section picks songs for its seed track. */
enum class SeedPickMode { ALONGSIDE, TIME_RANGE, DISCOVERED, ERA }

data class HistoryPicksState(
    /** Songs you usually play around this time of day. */
    val timeOfDay: List<Song> = emptyList(),
    /** Years with songs played around today's date, newest first, and their songs. */
    val lostMemories: Map<Int, List<Song>> = emptyMap(),
    val selectedYear: Int? = null,
    /** The track the "more like" section is built on: the current one, else the last heard. */
    val seed: Song? = null,
    /** Only the modes that found something. */
    val seedPicks: Map<SeedPickMode, List<Song>> = emptyMap(),
    val selectedMode: SeedPickMode? = null,
)

/** Home sections built from the listen history (see [HistoryPicks]). */
@HiltViewModel
class HistoryPicksViewModel @Inject constructor(
    private val repository: HistoryPicksRepository,
    private val musicRepository: MusicRepository,
    playbackStateHolder: PlaybackStateHolder,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryPicksState())
    val state: StateFlow<HistoryPicksState> = _state.asStateFlow()

    init {
        val currentSongId = playbackStateHolder.stablePlayerState
            .map { it.currentSong?.id }
            .distinctUntilChanged()
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            combine(repository.changes, currentSongId) { _, id -> id }
                .debounce(REBUILD_DEBOUNCE_MS)
                .collect { rebuild(it) }
        }
    }

    fun selectYear(year: Int) = _state.update { it.copy(selectedYear = year) }

    fun selectMode(mode: SeedPickMode) = _state.update { it.copy(selectedMode = mode) }

    private suspend fun rebuild(currentSongId: String?) = withContext(Dispatchers.Default) {
        val index = runCatching { repository.index() }.getOrNull() ?: return@withContext
        if (index.isEmpty) {
            _state.value = HistoryPicksState()
            return@withContext
        }
        val now = System.currentTimeMillis()
        val seedId = currentSongId ?: index.listens.last().songId
        val library = musicRepository.getAllSongsOnce()
        val byId = library.associateBy { it.id }
        fun songs(ids: List<String>) = ids.asSequence().mapNotNull(byId::get).take(SECTION_SIZE).toList()

        val timeOfDay = songs(HistoryPicks.aroundTimeOfDay(index, now, SECTION_SIZE * 2))
        val lost = HistoryPicks.lostMemoryYears(index, now)
            .associateWith { songs(HistoryPicks.lostMemories(index, it, now)) }
            .filterValues { it.isNotEmpty() }
        val seed = byId[seedId]
        val seedPicks = if (seed == null) emptyMap() else seedPicks(index, seed, library, now, ::songs)

        _state.update { old ->
            HistoryPicksState(
                timeOfDay = timeOfDay,
                lostMemories = lost,
                selectedYear = old.selectedYear?.takeIf { it in lost } ?: lost.keys.firstOrNull(),
                seed = seed,
                seedPicks = seedPicks,
                selectedMode = old.selectedMode?.takeIf { it in seedPicks } ?: seedPicks.keys.firstOrNull(),
            )
        }
    }

    private fun seedPicks(
        index: HistoryIndex,
        seed: Song,
        library: List<Song>,
        now: Long,
        songs: (List<String>) -> List<Song>,
    ): Map<SeedPickMode, List<Song>> = linkedMapOf(
        SeedPickMode.ALONGSIDE to songs(HistoryPicks.playedAlongside(index, seed.id, now)),
        SeedPickMode.TIME_RANGE to songs(HistoryPicks.sameTimeRange(index, seed.id)),
        SeedPickMode.DISCOVERED to songs(HistoryPicks.discoveredTogether(index, seed.id)),
        SeedPickMode.ERA to songs(
            HistoryPicks.sameEra(seed.year, seed.id, library.associate { it.id to it.year }, index)
        ),
    ).filterValues { it.size >= MIN_SECTION_SIZE }

    private companion object {
        const val SECTION_SIZE = 10
        const val MIN_SECTION_SIZE = 3
        const val REBUILD_DEBOUNCE_MS = 400L
    }
}
