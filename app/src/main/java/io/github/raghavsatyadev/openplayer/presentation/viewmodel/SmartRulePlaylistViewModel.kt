package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleCodec
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleDefinition
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleResolver
import io.github.raghavsatyadev.openplayer.data.preferences.PlaylistPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Saves rule playlists and counts what a rule would match, for the rule editor sheet. */
@HiltViewModel
class SmartRulePlaylistViewModel @Inject constructor(
    private val playlistPreferencesRepository: PlaylistPreferencesRepository,
    private val resolver: SmartRuleResolver,
) : ViewModel() {

    /** How many songs [definition] matches now, and again whenever that changes. */
    fun previewCount(definition: SmartRuleDefinition): Flow<Int> =
        resolver.observe(definition, SEED).map { it.size }

    /**
     * Creates the playlist, or with [playlistId] updates the existing rule playlist's name and
     * rules. Its songs are filled in right away and kept current by [PlaylistViewModel].
     */
    fun save(playlistId: String?, name: String, definition: SmartRuleDefinition, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val source = SmartRuleCodec.toSource(definition)
            val seed = playlistId?.let(::seedFor) ?: SEED
            val songIds = resolver.resolve(definition, seed)
                .take(SmartRuleResolver.MAX_SONGS).map { it.id }
            val existing = playlistId?.let { id ->
                playlistPreferencesRepository.getPlaylistsOnce().firstOrNull { it.id == id }
            }
            if (existing != null) {
                playlistPreferencesRepository.updatePlaylist(
                    existing.copy(name = name.trim(), source = source, songIds = songIds)
                )
            } else {
                playlistPreferencesRepository.createPlaylist(
                    name = name.trim(),
                    songIds = songIds,
                    source = source,
                )
            }
            onSaved()
        }
    }

    companion object {
        private const val SEED = 0L

        /** Keeps a rule playlist's random order stable between re-evaluations. */
        fun seedFor(playlistId: String): Long = playlistId.hashCode().toLong()
    }
}
