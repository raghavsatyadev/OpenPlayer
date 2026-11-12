package io.github.raghavsatyadev.openplayer.data.history

/** One counted listen: a play of [songId] that met the [ListenCountRule]. */
data class CountedListen(
    val songId: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val listenedMs: Long,
    val trackDurationMs: Long
)

/**
 * Follows one playthrough of a track and decides whether it is a listen.
 *
 * Feed it listening time with [advance]; it closes the current pass when the track starts over
 * (so a repeat or a seek back to the start can count again) and when [close] is called at the end
 * of the session. A closed pass is returned only if it met the rule.
 */
class ListenPass(
    private val songId: String,
    startedAtEpochMs: Long,
    startPositionMs: Long
) {
    private var passStartedAtEpochMs = startedAtEpochMs
    private var passListenedMs = 0L
    private var lastPositionMs = startPositionMs.coerceAtLeast(0L)

    val listenedInPassMs: Long get() = passListenedMs

    /**
     * Adds [deltaMs] of listening that ended at [positionMs]. If the position went back to the
     * start after this pass already counted, the pass closes and is returned; a new one begins.
     */
    fun advance(
        deltaMs: Long,
        positionMs: Long,
        durationMs: Long,
        nowEpochMs: Long,
        rule: ListenCountRule
    ): CountedListen? {
        passListenedMs += deltaMs.coerceAtLeast(0L)
        val position = positionMs.coerceAtLeast(0L)
        var closed: CountedListen? = null
        if (rule.isRestart(lastPositionMs, position, durationMs) &&
            rule.isCounted(passListenedMs, durationMs)
        ) {
            closed = build(endedAtEpochMs = nowEpochMs, durationMs = durationMs)
            passStartedAtEpochMs = nowEpochMs
            passListenedMs = 0L
        }
        lastPositionMs = position
        return closed
    }

    /** Ends the session; returns the last pass if it counted. */
    fun close(endedAtEpochMs: Long, durationMs: Long, rule: ListenCountRule): CountedListen? {
        if (!rule.isCounted(passListenedMs, durationMs)) return null
        return build(endedAtEpochMs = maxOf(endedAtEpochMs, passStartedAtEpochMs), durationMs = durationMs)
    }

    private fun build(endedAtEpochMs: Long, durationMs: Long) = CountedListen(
        songId = songId,
        startedAtEpochMs = passStartedAtEpochMs,
        endedAtEpochMs = endedAtEpochMs,
        listenedMs = passListenedMs,
        trackDurationMs = durationMs.coerceAtLeast(0L)
    )
}
