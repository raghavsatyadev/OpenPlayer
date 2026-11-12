package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.repository.MusicRepository
import io.github.raghavsatyadev.openplayer.data.repository.SavedQueue
import io.github.raghavsatyadev.openplayer.data.repository.SavedQueueRepository
import io.github.raghavsatyadev.openplayer.data.repository.SavedQueueRestorePlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Saves the play queue under a name, lists saved queues, and turns one back into a playable plan. */
@HiltViewModel
class SavedQueuesViewModel @Inject constructor(
    private val repository: SavedQueueRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    val queues: StateFlow<List<SavedQueue>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(name: String, songs: List<Song>, currentIndex: Int, positionMs: Long) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            repository.save(name, songs.map { it.id }, currentIndex, positionMs)
        }
    }

    fun delete(queue: SavedQueue) {
        viewModelScope.launch { repository.delete(queue.id) }
    }

    suspend fun restorePlan(queue: SavedQueue): SavedQueueRestorePlan {
        val available = musicRepository.getSongsByIdsOnce(queue.songIds.toSet()).associateBy { it.id }
        return SavedQueueRestorePlan.build(queue, available)
    }
}
