package io.github.raghavsatyadev.openplayer.data.service.repeatn

import androidx.media3.common.Player

/**
 * Pure state machine for "repeat the current song N more times, then continue normally".
 *
 * While active the player runs in REPEAT_MODE_ONE; every loop of the song arrives as a
 * MEDIA_ITEM_TRANSITION_REASON_REPEAT transition. When the transition that takes [remaining]
 * to zero arrives, that loop is the last one, so the previous repeat mode is restored right
 * away and the queue moves on after it.
 */
class RepeatNCounter {

    sealed interface Action {
        /** Nothing to do. */
        data object None : Action

        /** [remaining] changed; the count is still running. */
        data class Counting(val remaining: Int) : Action

        /** The count is over (finished or reset by a skip); put [mode] back on the player. */
        data class Restore(val mode: Int, val finished: Boolean) : Action
    }

    var songId: String? = null
        private set
    var remaining: Int = 0
        private set
    private var previousMode: Int = Player.REPEAT_MODE_OFF

    val isActive: Boolean get() = remaining > 0

    /** Starts a count of [times] extra plays; returns the repeat mode the player must use. */
    fun start(songId: String, times: Int, currentMode: Int): Int {
        this.songId = songId
        remaining = times.coerceIn(MIN_TIMES, MAX_TIMES)
        // Repeat-one already loops forever, so "N more times" restores to off.
        previousMode = if (currentMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_OFF else currentMode
        return Player.REPEAT_MODE_ONE
    }

    fun onTransition(newSongId: String?, reason: Int): Action {
        if (!isActive) return Action.None
        if (newSongId != songId) return reset(finished = false)
        return when (reason) {
            Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> {
                remaining--
                if (remaining <= 0) reset(finished = true) else Action.Counting(remaining)
            }
            // Same song, e.g. the queue was edited around it: keep counting.
            else -> Action.None
        }
    }

    /** The user picked another repeat mode mid-count: accept it. Returns true if that ended the count. */
    fun onRepeatModeChanged(mode: Int): Boolean {
        if (!isActive || mode == Player.REPEAT_MODE_ONE) return false
        clear()
        return true
    }

    /** Ends the count; returns the mode to restore, or null if it was not active. */
    fun cancel(): Int? {
        if (!isActive) return null
        val mode = previousMode
        clear()
        return mode
    }

    private fun reset(finished: Boolean): Action.Restore {
        val mode = previousMode
        clear()
        return Action.Restore(mode, finished)
    }

    private fun clear() {
        remaining = 0
        songId = null
    }

    companion object {
        const val MIN_TIMES = 2
        const val MAX_TIMES = 10
    }
}
