package io.github.raghavsatyadev.openplayer.data.video

/**
 * Keeps a muted video in step with the audio that is playing.
 *
 * The audio is the clock: the video follows it and never the other way round.
 */
object VideoSync {

    /** Drift smaller than this is left alone, since seeking a video costs a visible stutter. */
    const val DRIFT_TOLERANCE_MS = 250L

    /** A video loops only if it is shorter than this share of the track (Namida's rule). */
    const val LOOP_MAX_SHARE = 0.6

    /** Loop the video when it is clearly shorter than the track, like a short visualizer clip. */
    fun canLoop(videoDurationMs: Long, trackDurationMs: Long): Boolean {
        if (videoDurationMs <= 0 || trackDurationMs <= 0) return false
        return videoDurationMs < trackDurationMs * LOOP_MAX_SHARE
    }

    sealed interface Decision {
        /** The video is close enough: keep playing. */
        data object InSync : Decision

        /** Move the video to [positionMs]. */
        data class SeekTo(val positionMs: Long) : Decision

        /** The audio is past the end of a non-looping video: hold its last frame. */
        data object HoldAtEnd : Decision

        /** Small drift: play the video at [speedFactor] times the audio speed until it catches up. */
        data class Nudge(val speedFactor: Float) : Decision
    }

    /** Lip sync: drift under this is invisible. */
    const val LIP_SYNC_TOLERANCE_MS = 40L

    /** Lip sync: drift past this is fixed with a seek, below it with a speed nudge. */
    const val LIP_SYNC_SEEK_MS = 400L

    private const val NUDGE = 0.06f

    /**
     * For a video playing its own sound, where lips must match: tiny drift is ignored, small
     * drift is closed by running the video slightly fast or slow (no stutter), large drift seeks.
     */
    fun decideLipSync(audioPositionMs: Long, videoPositionMs: Long, videoDurationMs: Long): Decision {
        val target = targetPositionMs(audioPositionMs, videoDurationMs, loop = false) ?: return Decision.HoldAtEnd
        val drift = videoPositionMs - target
        return when {
            kotlin.math.abs(drift) > LIP_SYNC_SEEK_MS -> Decision.SeekTo(target)
            drift > LIP_SYNC_TOLERANCE_MS -> Decision.Nudge(1f - NUDGE)
            drift < -LIP_SYNC_TOLERANCE_MS -> Decision.Nudge(1f + NUDGE)
            else -> Decision.InSync
        }
    }

    /** Where the video should be for [audioPositionMs], or null when the video has ended. */
    fun targetPositionMs(audioPositionMs: Long, videoDurationMs: Long, loop: Boolean): Long? {
        val audio = audioPositionMs.coerceAtLeast(0)
        if (videoDurationMs <= 0) return audio
        if (loop) return audio % videoDurationMs
        return if (audio >= videoDurationMs) null else audio
    }

    fun decide(
        audioPositionMs: Long,
        videoPositionMs: Long,
        videoDurationMs: Long,
        loop: Boolean,
        toleranceMs: Long = DRIFT_TOLERANCE_MS,
    ): Decision {
        val target = targetPositionMs(audioPositionMs, videoDurationMs, loop) ?: return Decision.HoldAtEnd
        var drift = kotlin.math.abs(videoPositionMs - target)
        if (loop && videoDurationMs > 0) {
            // Near the loop point, 9.9s and 0.1s of a 10s clip are 0.2s apart, not 9.8s.
            drift = minOf(drift, videoDurationMs - drift)
        }
        return if (drift > toleranceMs) Decision.SeekTo(target) else Decision.InSync
    }
}
