package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import io.github.raghavsatyadev.openplayer.data.model.Playlist
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleDefinition
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleResolver
import io.github.raghavsatyadev.openplayer.data.preferences.PlaylistPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Keeps the opened rule playlist's stored songs equal to what its rules match: [refresh]
 * evaluates once when the playlist is opened, and watches the library, play stats, user meta and
 * favorites so a change re-evaluates it while it stays open, calling [onSongsChanged] when its
 * songs differ.
 */
class RulePlaylistObserver(
    private val resolver: SmartRuleResolver,
    private val repository: PlaylistPreferencesRepository,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null
    private var watchedKey: String? = null
    private var knownIds: List<String> = emptyList()

    suspend fun refresh(
        playlist: Playlist,
        definition: SmartRuleDefinition,
        onSongsChanged: (playlistId: String) -> Unit,
    ): Playlist {
        val seed = SmartRulePlaylistViewModel.seedFor(playlist.id)
        val ids = resolver.resolve(definition, seed).take(SmartRuleResolver.MAX_SONGS).map { it.id }
        knownIds = ids
        watch(playlist, definition, seed, onSongsChanged)
        if (ids == playlist.songIds) return playlist
        // Songs only: the playlist's name, cover and lastModified stay as they are.
        repository.replacePlaylistSongs(playlist.id, ids)
        return playlist.copy(songIds = ids)
    }

    fun stop() {
        job?.cancel()
        job = null
        watchedKey = null
    }

    private fun watch(
        playlist: Playlist,
        definition: SmartRuleDefinition,
        seed: Long,
        onSongsChanged: (String) -> Unit,
    ) {
        val key = playlist.id + playlist.source
        if (key == watchedKey && job?.isActive == true) return
        job?.cancel()
        watchedKey = key
        job = scope.launch {
            resolver.observe(definition, seed).collect { songs ->
                val ids = songs.take(SmartRuleResolver.MAX_SONGS).map { it.id }
                if (ids != knownIds) {
                    knownIds = ids
                    repository.replacePlaylistSongs(playlist.id, ids)
                    onSongsChanged(playlist.id)
                }
            }
        }
    }
}
