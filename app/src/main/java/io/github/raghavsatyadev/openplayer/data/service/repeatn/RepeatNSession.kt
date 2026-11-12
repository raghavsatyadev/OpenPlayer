package io.github.raghavsatyadev.openplayer.data.service.repeatn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide repeat-N state. [RepeatNController] (inside MusicService) is the only writer;
 * the UI mirrors [remaining] for the badge on the repeat button and sends commands back.
 */
@Singleton
class RepeatNSession @Inject constructor() {
    private val _remaining = MutableStateFlow(0)

    /** Extra plays still to come for the current song; 0 when repeat-N is off. */
    val remaining: StateFlow<Int> = _remaining.asStateFlow()

    val isActive: Boolean get() = _remaining.value > 0

    internal fun setRemaining(value: Int) {
        _remaining.value = value
    }
}
