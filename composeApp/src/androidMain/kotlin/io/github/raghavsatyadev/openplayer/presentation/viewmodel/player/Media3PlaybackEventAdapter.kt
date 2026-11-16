package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import io.github.raghavsatyadev.openplayer.data.service.MusicNotificationProvider

/** Media3 `Player.STATE_*` as a [PlaybackEngineState]; null and unknown values read as IDLE. */
internal fun Int?.toPlaybackEngineState(): PlaybackEngineState = when (this) {
    Player.STATE_BUFFERING -> PlaybackEngineState.BUFFERING
    Player.STATE_READY -> PlaybackEngineState.READY
    Player.STATE_ENDED -> PlaybackEngineState.ENDED
    else -> PlaybackEngineState.IDLE
}

/** Translates Media3 player and session callbacks into [PlaybackEventListener] calls. */
class Media3PlaybackEventAdapter(private val listener: PlaybackEventListener) {

    /** Registered with the controller at build time so early session commands are not dropped. */
    val controllerListener: MediaController.Listener = object : MediaController.Listener {
        override fun onCustomCommand(
            controller: MediaController,
            command: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (command.customAction == MusicNotificationProvider.CUSTOM_COMMAND_SET_SHUFFLE_STATE) {
                val enabled = args.getBoolean(MusicNotificationProvider.EXTRA_SHUFFLE_ENABLED, false)
                listener.onSessionShuffleCommand(enabled)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
    }

    /** Added to the controller once it is connected and the initial sync has run. */
    val playerListener: Player.Listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) = listener.onPlayerError()

        override fun onIsPlayingChanged(isPlaying: Boolean) = listener.onIsPlayingChanged(isPlaying)

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) =
            listener.onPlayWhenReadyChanged(playWhenReady)

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val mapped = when (reason) {
                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> ItemTransitionReason.AUTO
                Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> ItemTransitionReason.REPEAT
                Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> ItemTransitionReason.SEEK
                else -> ItemTransitionReason.PLAYLIST_CHANGED
            }
            listener.onCurrentItemChanged(mediaItem?.mediaId, mapped)
        }

        override fun onPlaybackStateChanged(playbackState: Int) =
            listener.onEngineStateChanged(playbackState.toPlaybackEngineState())

        override fun onTracksChanged(tracks: Tracks) = listener.onTracksChanged()

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = listener.onMetadataChanged()

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) =
            listener.onNativeShuffleChanged(shuffleModeEnabled)

        override fun onRepeatModeChanged(repeatMode: Int) = listener.onRepeatModeChanged(repeatMode)

        override fun onTimelineChanged(timeline: Timeline, reason: Int) = listener.onQueueChanged(
            structural = reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED ||
                reason == Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE,
        )
    }
}
