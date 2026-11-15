package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController

/**
 * [PlaybackController] over the Media3 [MediaController]. [controller] is read on every call
 * because the view model swaps controllers (session reconnects, external playback).
 */
class MediaControllerPlaybackController(
    private val controller: () -> MediaController?,
) : PlaybackController {

    private val connected: MediaController?
        get() = controller()?.takeIf { it.isConnected }

    override val isConnected: Boolean get() = connected != null
    override val isPlaying: Boolean get() = connected?.isPlaying == true
    override val playWhenReady: Boolean get() = connected?.playWhenReady == true

    override val engineState: PlaybackEngineState
        get() = when (connected?.playbackState) {
            Player.STATE_BUFFERING -> PlaybackEngineState.BUFFERING
            Player.STATE_READY -> PlaybackEngineState.READY
            Player.STATE_ENDED -> PlaybackEngineState.ENDED
            else -> PlaybackEngineState.IDLE
        }

    override val currentMediaId: String? get() = connected?.currentMediaItem?.mediaId

    override val currentIndex: Int
        get() = connected?.currentMediaItemIndex
            ?.takeIf { it != C.INDEX_UNSET }
            ?: PlaybackController.NO_INDEX

    override val queueSize: Int get() = connected?.mediaItemCount ?: 0
    override val positionMs: Long get() = connected?.currentPosition ?: 0L

    override val durationMs: Long
        get() = connected?.duration
            ?.takeIf { it != C.TIME_UNSET }
            ?: PlaybackController.UNKNOWN_DURATION

    override var repeatMode: Int
        get() = connected?.repeatMode ?: PlaybackController.REPEAT_OFF
        set(value) { connected?.repeatMode = value }

    override var shuffleEnabled: Boolean
        get() = connected?.shuffleModeEnabled == true
        set(value) { connected?.shuffleModeEnabled = value }

    override fun mediaIdAt(index: Int): String? {
        val player = connected ?: return null
        if (index !in 0 until player.mediaItemCount) return null
        return runCatching { player.getMediaItemAt(index).mediaId }.getOrNull()
    }

    override fun prepare() { connected?.prepare() }
    override fun play() { connected?.play() }
    override fun pause() { connected?.pause() }
    override fun stop() { connected?.stop() }
    override fun seekTo(positionMs: Long) { connected?.seekTo(positionMs) }
    override fun seekTo(index: Int, positionMs: Long) { connected?.seekTo(index, positionMs) }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        connected?.moveMediaItem(fromIndex, toIndex)
    }

    override fun removeQueueItem(index: Int) { connected?.removeMediaItem(index) }
    override fun clearQueue() { connected?.clearMediaItems() }
}
