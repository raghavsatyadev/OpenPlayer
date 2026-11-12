package io.github.raghavsatyadev.openplayer.data.service.repeatn

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import co.touchlab.kermit.Logger

/**
 * Applies [RepeatNCounter] to the session player inside MusicService. Hooks are the player
 * listener's media-item-transition and repeat-mode callbacks; [onActiveChanged] lets the
 * sleep-timer hold keep crossfade off, so each loop is a plain REPEAT transition.
 */
internal class RepeatNController(
    private val session: RepeatNSession,
    private val sessionPlayer: () -> Player,
    private val onActiveChanged: () -> Unit,
) {
    private val counter = RepeatNCounter()

    val isActive: Boolean get() = counter.isActive

    fun start(times: Int) {
        val player = sessionPlayer()
        val songId = player.currentMediaItem?.mediaId ?: return
        cancel(restoreRepeatMode = true)
        val mode = counter.start(songId, times, player.repeatMode)
        session.setRemaining(counter.remaining)
        onActiveChanged()
        player.repeatMode = mode
        Logger.withTag(TAG).d { String.format("Repeat-N started: %d more plays of %s", counter.remaining, songId) }
    }

    fun cancel(restoreRepeatMode: Boolean = true) {
        val mode = counter.cancel() ?: return
        session.setRemaining(0)
        onActiveChanged()
        if (restoreRepeatMode) sessionPlayer().repeatMode = mode
    }

    fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        when (val action = counter.onTransition(mediaItem?.mediaId, reason)) {
            RepeatNCounter.Action.None -> Unit
            is RepeatNCounter.Action.Counting -> session.setRemaining(action.remaining)
            is RepeatNCounter.Action.Restore -> {
                session.setRemaining(0)
                onActiveChanged()
                sessionPlayer().repeatMode = action.mode
                Logger.withTag(TAG).d { String.format("Repeat-N ended (finished=%s)", action.finished) }
            }
        }
    }

    fun onRepeatModeChanged(mode: Int) {
        if (counter.onRepeatModeChanged(mode)) {
            session.setRemaining(0)
            onActiveChanged()
        }
    }

    private companion object {
        const val TAG = "RepeatNController"
    }
}
