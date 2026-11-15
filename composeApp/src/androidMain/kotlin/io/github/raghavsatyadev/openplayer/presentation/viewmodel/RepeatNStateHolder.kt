package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import io.github.raghavsatyadev.openplayer.data.service.repeatn.RepeatNCounter
import io.github.raghavsatyadev.openplayer.data.service.repeatn.RepeatNSession
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.player.PlaybackController
import kotlinx.coroutines.flow.StateFlow

/** Mirrors repeat-N for the UI and forwards the user's choices to MusicService. */
class RepeatNStateHolder(
    private val session: RepeatNSession,
) {
    /** Extra plays still to come for the current song; 0 when off. */
    val remaining: StateFlow<Int> = session.remaining

    /** True while repeat-N holds the player in repeat-one (so that mode must not be saved). */
    val isActive: Boolean get() = session.isActive

    fun start(controller: PlaybackController, times: Int) {
        controller.startRepeatN(times.coerceIn(RepeatNCounter.MIN_TIMES, RepeatNCounter.MAX_TIMES))
    }

    fun cancel(controller: PlaybackController) {
        controller.cancelRepeatN()
    }
}
