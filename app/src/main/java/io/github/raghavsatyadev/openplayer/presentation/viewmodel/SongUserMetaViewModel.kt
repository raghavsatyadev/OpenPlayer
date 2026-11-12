package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.raghavsatyadev.openplayer.data.repository.SongUserMeta
import io.github.raghavsatyadev.openplayer.data.repository.SongUserMetaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Reads and saves a song's rating, moods and tags, and the values already used on other songs. */
class SongUserMetaViewModel(
    private val repository: SongUserMetaRepository
) : ViewModel() {

    fun metaFor(songId: String): Flow<SongUserMeta?> = repository.observe(songId)

    suspend fun moodSuggestions(): List<String> = repository.moodSuggestions()

    suspend fun tagSuggestions(): List<String> = repository.tagSuggestions()

    fun save(meta: SongUserMeta) {
        viewModelScope.launch { repository.save(meta) }
    }
}
