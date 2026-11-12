package io.github.raghavsatyadev.openplayer.data.history

/**
 * When a play counts as a listen, after Namida's rule: a listen counts once you have heard
 * [minSeconds] of the track or [minPercentage] of its length, whichever comes first.
 *
 * After a listen has counted, going back to the start of the track (or back by a quarter of it,
 * as repeat-one does) starts a new listen.
 */
data class ListenCountRule(
    val minSeconds: Int = DEFAULT_MIN_SECONDS,
    val minPercentage: Int = DEFAULT_MIN_PERCENTAGE
) {

    /** Listening needed for [durationMs]; an unknown length falls back to the seconds rule. */
    fun requiredListenMs(durationMs: Long): Long {
        val secondsMs = minSeconds.coerceIn(SECONDS_RANGE) * 1000L
        if (durationMs <= 0L) return secondsMs
        val percentageMs = durationMs * minPercentage.coerceIn(PERCENTAGE_RANGE) / 100L
        return minOf(secondsMs, percentageMs)
    }

    fun isCounted(listenedMs: Long, durationMs: Long): Boolean =
        listenedMs > 0L && listenedMs >= requiredListenMs(durationMs)

    /**
     * True when moving from [previousPositionMs] to [newPositionMs] starts the track over: the
     * position went back and landed within [RESTART_START_WINDOW_MS] of the start, or went
     * back by at least a quarter of the track.
     */
    fun isRestart(previousPositionMs: Long, newPositionMs: Long, durationMs: Long): Boolean {
        if (newPositionMs >= previousPositionMs) return false
        if (newPositionMs <= RESTART_START_WINDOW_MS) return true
        return durationMs > 0L && (previousPositionMs - newPositionMs) * 4 >= durationMs
    }

    fun sanitized(): ListenCountRule = ListenCountRule(
        minSeconds = minSeconds.coerceIn(SECONDS_RANGE),
        minPercentage = minPercentage.coerceIn(PERCENTAGE_RANGE)
    )

    companion object {
        const val DEFAULT_MIN_SECONDS = 40
        const val DEFAULT_MIN_PERCENTAGE = 40
        val SECONDS_RANGE = 10..180
        val PERCENTAGE_RANGE = 10..100

        /** A seek or loop that lands this close to the start counts as starting over. */
        const val RESTART_START_WINDOW_MS = 3_000L
    }
}
