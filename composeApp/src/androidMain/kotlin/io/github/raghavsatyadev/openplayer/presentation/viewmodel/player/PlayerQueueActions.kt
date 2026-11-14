package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import androidx.media3.session.MediaController
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.utils.MediaItemBuilder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlaybackStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerUiState
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.QueueUndoStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.moveQueueIndex
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.moveSong
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.queue.PlayNextInserter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Queue edits (add, play next, remove with undo, reorder) extracted from PlayerViewModel.
 * Reorder goes through [PlaybackController]; adding and undo still need the Media3
 * [MediaController] because [PlayNextInserter] and [QueueUndoStateHolder] work on it directly.
 */
class PlayerQueueActions(
    private val scope: CoroutineScope,
    private val controller: PlaybackController,
    private val mediaController: () -> MediaController?,
    private val playbackStateHolder: PlaybackStateHolder,
    private val queueUndoStateHolder: QueueUndoStateHolder,
    private val playNextInserter: PlayNextInserter,
    private val playerUiState: MutableStateFlow<PlayerUiState>,
) {

    fun addSongToQueue(song: Song) {
        mediaController()?.addMediaItem(MediaItemBuilder.build(song))
    }

    fun addSongNextToQueue(song: Song) {
        val player = mediaController() ?: return
        playNextInserter.insert(player, MediaItemBuilder.build(song))
    }

    /** Queues [songs] right after the current song (or the last one queued this way), in order. */
    fun addSongsAsNext(songs: List<Song>) {
        playNextInserter.inInsertOrder(songs).forEach(::addSongNextToQueue)
    }

    /** Removes [songId]; pass the row's [queueIndex] so duplicate copies resolve to the right one. */
    fun removeSongFromQueue(songId: String, queueIndex: Int? = null) {
        val removedIndex = queueUndoStateHolder.removeSongFromQueue(
            scope = scope,
            mediaController = mediaController(),
            songId = songId,
            queueIndex = queueIndex,
            getUiState = { playerUiState.value },
            updateUiState = { mutation -> playerUiState.update(mutation) },
        )
        if (removedIndex != null) playNextInserter.onItemRemoved(removedIndex)
    }

    fun undoRemoveSongFromQueue() {
        queueUndoStateHolder.undoRemoveSongFromQueue(
            mediaController = mediaController(),
            getUiState = { playerUiState.value },
            updateUiState = { mutation -> playerUiState.update(mutation) },
        )
    }

    fun hideQueueItemUndoBar() {
        queueUndoStateHolder.hideQueueItemUndoBar { mutation -> playerUiState.update(mutation) }
    }

    fun reorderQueueItem(fromIndex: Int, toIndex: Int) {
        if (!controller.isConnected) return
        val size = controller.queueSize
        if (fromIndex !in 0 until size || toIndex !in 0 until size) return

        val currentIndexBeforeMove = controller.currentIndex
            .takeIf { it != PlaybackController.NO_INDEX }
            ?: playbackStateHolder.stablePlayerState.value.currentMediaItemIndex
        val updatedCurrentIndex = moveQueueIndex(currentIndexBeforeMove, fromIndex, toIndex)

        playNextInserter.onItemMoved(fromIndex, toIndex)
        controller.moveQueueItem(fromIndex, toIndex)

        playerUiState.update { state ->
            val updatedQueue = state.currentPlaybackQueue.moveSong(fromIndex, toIndex)
            if (updatedQueue === state.currentPlaybackQueue) state
            else state.copy(currentPlaybackQueue = updatedQueue)
        }
        playbackStateHolder.updateStablePlayerState { state ->
            if (updatedCurrentIndex == PlaybackController.NO_INDEX ||
                state.currentMediaItemIndex == updatedCurrentIndex
            ) state
            else state.copy(currentMediaItemIndex = updatedCurrentIndex)
        }
    }
}
