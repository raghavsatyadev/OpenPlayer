package io.github.raghavsatyadev.openplayer.data.service.player

import androidx.annotation.OptIn
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator

/**
 * [DefaultLoadControl] with a hard memory ceiling.
 *
 * The music player buffers by time. For a music video it plays only the sound, but the
 * loader still keeps the unused picture data ahead of the playhead, so a minute of a
 * high-bitrate video can fill the heap. Loading pauses once [maxBytes] are held.
 */
@OptIn(UnstableApi::class)
class CappedLoadControl(
    private val delegate: DefaultLoadControl,
    private val maxBytes: Int = DEFAULT_MAX_BYTES,
) : LoadControl {

    override fun shouldContinueLoading(parameters: LoadControl.Parameters): Boolean {
        val held = delegate.getAllocator(parameters.playerId).totalBytesAllocated
        return held < maxBytes && delegate.shouldContinueLoading(parameters)
    }

    override fun onPrepared(playerId: PlayerId) = delegate.onPrepared(playerId)

    override fun onTracksSelected(
        parameters: LoadControl.Parameters,
        trackGroups: TrackGroupArray,
        trackSelections: Array<out ExoTrackSelection?>,
    ) = delegate.onTracksSelected(parameters, trackGroups, trackSelections)

    override fun onStopped(playerId: PlayerId) = delegate.onStopped(playerId)

    override fun onReleased(playerId: PlayerId) = delegate.onReleased(playerId)

    override fun getAllocator(playerId: PlayerId): Allocator = delegate.getAllocator(playerId)

    override fun getBackBufferDurationUs(playerId: PlayerId): Long = delegate.getBackBufferDurationUs(playerId)

    override fun retainBackBufferFromKeyframe(playerId: PlayerId): Boolean =
        delegate.retainBackBufferFromKeyframe(playerId)

    override fun shouldContinuePreloading(
        playerId: PlayerId,
        timeline: Timeline,
        mediaPeriodId: MediaSource.MediaPeriodId,
        bufferedDurationUs: Long,
    ): Boolean = delegate.shouldContinuePreloading(playerId, timeline, mediaPeriodId, bufferedDurationUs)

    override fun shouldStartPlayback(parameters: LoadControl.Parameters): Boolean =
        delegate.shouldStartPlayback(parameters)

    companion object {
        /** Holds about 10 s of a 25 Mbit/s video, or minutes of any audio file. */
        const val DEFAULT_MAX_BYTES = 32 * 1024 * 1024
    }
}
