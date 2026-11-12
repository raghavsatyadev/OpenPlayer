package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import io.github.raghavsatyadev.openplayer.data.service.MusicNotificationProvider
import io.github.raghavsatyadev.openplayer.data.service.repeatn.RepeatNCounter
import io.github.raghavsatyadev.openplayer.data.service.repeatn.RepeatNSession
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Mirrors repeat-N for the UI and forwards the user's choices to MusicService. */
@Singleton
class RepeatNStateHolder @Inject constructor(
    private val session: RepeatNSession,
) {
    /** Extra plays still to come for the current song; 0 when off. */
    val remaining: StateFlow<Int> = session.remaining

    /** True while repeat-N holds the player in repeat-one (so that mode must not be saved). */
    val isActive: Boolean get() = session.isActive

    fun start(controller: MediaController?, times: Int) {
        controller?.sendCustomCommand(
            SessionCommand(MusicNotificationProvider.CUSTOM_COMMAND_REPEAT_N, Bundle.EMPTY),
            Bundle().apply {
                putInt(
                    MusicNotificationProvider.EXTRA_REPEAT_N_TIMES,
                    times.coerceIn(RepeatNCounter.MIN_TIMES, RepeatNCounter.MAX_TIMES),
                )
            },
        )
    }

    fun cancel(controller: MediaController?) {
        controller?.sendCustomCommand(
            SessionCommand(MusicNotificationProvider.CUSTOM_COMMAND_CANCEL_REPEAT_N, Bundle.EMPTY),
            Bundle.EMPTY,
        )
    }
}
