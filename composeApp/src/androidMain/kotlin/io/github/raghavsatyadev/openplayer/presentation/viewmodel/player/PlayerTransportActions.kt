package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import io.github.raghavsatyadev.openplayer.data.diagnostics.AdvancedPerformanceDiagnostics
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.preferences.restartsAtPosition
import io.github.raghavsatyadev.openplayer.data.repository.MusicRepository
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlaybackStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Queue-building entry points that stay in PlayerViewModel and are invoked by [PlayerTransportActions]. */
class PlayerTransportCallbacks(
    /** Rebuilds the player queue from [songs] and starts at [startSong]. */
    val playQueue: suspend (songs: List<Song>, startSong: Song, queueName: String) -> Unit,
    val loadAndPlaySong: (Song) -> Unit,
    val cancelTransitionScheduler: () -> Unit,
    val onQueueReplaced: () -> Unit,
)

/**
 * Transport controls (play/pause, seek, skip, repeat, clear queue) extracted from PlayerViewModel.
 * Talks to the player only through [PlaybackController], so it carries no Media3 types.
 */
class PlayerTransportActions(
    private val scope: CoroutineScope,
    private val controller: PlaybackController,
    private val playbackStateHolder: PlaybackStateHolder,
    private val musicRepository: MusicRepository,
    private val playerUiState: StateFlow<PlayerUiState>,
    private val callbacks: PlayerTransportCallbacks,
) {

    fun playPause() {
        if (!controller.isConnected) {
            playbackStateHolder.playPause()
            return
        }
        when {
            controller.isPlaying -> controller.pause()
            controller.currentMediaId == null -> startFromRestoredState()
            else -> {
                if (controller.engineState == PlaybackEngineState.IDLE && controller.queueSize > 0) {
                    controller.prepare()
                }
                controller.play()
            }
        }
    }

    /** The player has no item (fresh session): rebuild from the restored queue, else any playable song. */
    private fun startFromRestoredState() {
        val currentQueue = playerUiState.value.currentPlaybackQueue
        val currentSong = playbackStateHolder.stablePlayerState.value.currentSong
        when {
            currentQueue.isNotEmpty() && currentSong != null -> scope.launch {
                callbacks.cancelTransitionScheduler()
                callbacks.playQueue(
                    currentQueue.toList(),
                    currentSong,
                    playerUiState.value.currentQueueSourceName,
                )
            }
            currentSong != null -> callbacks.loadAndPlaySong(currentSong)
            else -> scope.launch {
                val fallbackSong = musicRepository.getFirstPlayableSong()
                if (fallbackSong != null) callbacks.loadAndPlaySong(fallbackSong) else controller.play()
            }
        }
    }

    fun seekTo(position: Long) = playbackStateHolder.seekTo(position)

    fun nextSong() {
        recordPlaybackEvent("next_requested")
        playbackStateHolder.nextSong()
    }

    fun previousSong() {
        recordPlaybackEvent("previous_requested")
        playbackStateHolder.previousSong()
    }

    /**
     * Whether [previousSong] at [positionMs] restarts the current track under the "Previous
     * button" preference, ignoring queue bounds. Read at event time, so it adds no UI state.
     */
    fun previousButtonRestartsAt(positionMs: Long): Boolean =
        playbackStateHolder.currentPreviousButtonBehavior().restartsAtPosition(positionMs)

    /** Steps back to the previous track even past the restart threshold of [previousSong]. */
    fun forcePreviousSong() {
        recordPlaybackEvent("previous_requested")
        playbackStateHolder.forcePreviousSong()
    }

    fun cycleRepeatMode() = playbackStateHolder.cycleRepeatMode()

    /**
     * Signal from the player sheet whether the slider-bearing UI is currently
     * rendered. Drives the position-ticker's resolution (250 ms vs 1 s).
     */
    fun setSliderUiMounted(mounted: Boolean) = playbackStateHolder.setSliderUiMounted(mounted)

    fun clearQueueExceptCurrent() {
        if (controller.currentIndex == PlaybackController.NO_INDEX) return
        callbacks.onQueueReplaced()
        controller.removeAllExceptCurrent()
    }

    private fun recordPlaybackEvent(name: String) {
        AdvancedPerformanceDiagnostics.recordEventIfEnabled(
            type = AdvancedPerformanceDiagnostics.EventTypes.PLAYBACK,
            name = name,
        )
    }
}
