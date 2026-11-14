package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import androidx.media3.session.MediaController
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.utils.MediaItemBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QueueUndoStateHolder() {
    private var queueItemUndoTimerJob: Job? = null

    /**
     * Removes [songId] from the player queue and offers undo. [queueIndex] is the row's player
     * index when the caller knows it: the same song can be queued more than once, so matching by
     * ID alone could remove the wrong copy (even the current song). Returns the removed index.
     */
    fun removeSongFromQueue(
        scope: CoroutineScope,
        mediaController: MediaController?,
        songId: String,
        queueIndex: Int?,
        getUiState: () -> PlayerUiState,
        updateUiState: (((PlayerUiState) -> PlayerUiState) -> Unit),
    ): Int? {
        val controller = mediaController ?: return null
        val indexToRemove = resolveQueueIndex(controller, songId, queueIndex) ?: return null

        val currentQueue = getUiState().currentPlaybackQueue
        val removedSong = currentQueue.getOrNull(indexToRemove)?.takeIf { it.id == songId }
            ?: currentQueue.find { it.id == songId }
            ?: return null

        controller.removeMediaItem(indexToRemove)

        updateUiState {
            it.copy(
                showQueueItemUndoBar = true,
                lastRemovedQueueSong = removedSong,
                lastRemovedQueueIndex = indexToRemove
            )
        }

        queueItemUndoTimerJob?.cancel()
        queueItemUndoTimerJob = scope.launch {
            delay(4000L)
            if (getUiState().showQueueItemUndoBar) {
                updateUiState {
                    it.copy(
                        showQueueItemUndoBar = false,
                        lastRemovedQueueSong = null,
                        lastRemovedQueueIndex = -1
                    )
                }
            }
        }
        return indexToRemove
    }

    /** [queueIndex] when it still holds [songId]; else the first copy that isn't the current song. */
    private fun resolveQueueIndex(controller: MediaController, songId: String, queueIndex: Int?): Int? {
        val count = controller.mediaItemCount
        if (queueIndex != null && queueIndex in 0 until count &&
            controller.getMediaItemAt(queueIndex).mediaId == songId
        ) return queueIndex
        val matches = (0 until count).filter { controller.getMediaItemAt(it).mediaId == songId }
        return matches.firstOrNull { it != controller.currentMediaItemIndex } ?: matches.firstOrNull()
    }

    fun undoRemoveSongFromQueue(
        mediaController: MediaController?,
        getUiState: () -> PlayerUiState,
        updateUiState: (((PlayerUiState) -> PlayerUiState) -> Unit),
    ) {
        val uiState = getUiState()
        val song = uiState.lastRemovedQueueSong ?: return
        val index = uiState.lastRemovedQueueIndex
        if (index < 0) return

        mediaController?.let { controller ->
            val mediaItem = MediaItemBuilder.build(song)
            val insertAt = index.coerceAtMost(controller.mediaItemCount)
            controller.addMediaItem(insertAt, mediaItem)
        }

        hideQueueItemUndoBar(updateUiState)
    }

    fun hideQueueItemUndoBar(
        updateUiState: (((PlayerUiState) -> PlayerUiState) -> Unit),
    ) {
        queueItemUndoTimerJob?.cancel()
        updateUiState {
            it.copy(
                showQueueItemUndoBar = false,
                lastRemovedQueueSong = null,
                lastRemovedQueueIndex = -1
            )
        }
    }

    fun onCleared() {
        queueItemUndoTimerJob?.cancel()
    }
}
