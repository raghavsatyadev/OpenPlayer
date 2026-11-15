package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.service.MusicNotificationProvider

/**
 * [PlaybackController] over the Media3 [MediaController]. [controller] is read on every call
 * because the view model swaps controllers (session reconnects, external playback).
 * [resolvedMediaItem] builds the playable (engine-resolved) item for a song and playlist id.
 */
class MediaControllerPlaybackController(
    private val controller: () -> MediaController?,
    private val resolvedMediaItem: suspend (Song, String?) -> MediaItem,
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

    override suspend fun loadAndPlay(
        song: Song,
        playlistId: String?,
        startPositionMs: Long?,
        resumeIfCurrent: Boolean,
    ) {
        val mediaItem = resolvedMediaItem(song, playlistId)
        if (resumeIfCurrent && currentMediaId == song.id) {
            if (!isPlaying) play()
            return
        }
        val player = connected ?: return
        if (startPositionMs != null) player.setMediaItem(mediaItem, startPositionMs) else player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    override fun syncShuffleState(enabled: Boolean) {
        val player = connected ?: return
        val args = Bundle().apply {
            putBoolean(MusicNotificationProvider.EXTRA_SHUFFLE_ENABLED, enabled)
        }
        player.sendCustomCommand(
            SessionCommand(MusicNotificationProvider.CUSTOM_COMMAND_SET_SHUFFLE_STATE, Bundle()),
            args
        )
    }

    override fun startRepeatN(times: Int) {
        connected?.sendCustomCommand(
            SessionCommand(MusicNotificationProvider.CUSTOM_COMMAND_REPEAT_N, Bundle.EMPTY),
            Bundle().apply { putInt(MusicNotificationProvider.EXTRA_REPEAT_N_TIMES, times) },
        )
    }

    override fun cancelRepeatN() {
        connected?.sendCustomCommand(
            SessionCommand(MusicNotificationProvider.CUSTOM_COMMAND_CANCEL_REPEAT_N, Bundle.EMPTY),
            Bundle.EMPTY,
        )
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        connected?.moveMediaItem(fromIndex, toIndex)
    }

    override fun removeQueueItem(index: Int) { connected?.removeMediaItem(index) }
    override fun clearQueue() { connected?.clearMediaItems() }
}
