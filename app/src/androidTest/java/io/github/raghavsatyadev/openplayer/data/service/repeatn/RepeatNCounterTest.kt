package io.github.raghavsatyadev.openplayer.data.service.repeatn

import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepeatNCounterTest {

    private val repeat = Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
    private val auto = Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
    private val seek = Player.MEDIA_ITEM_TRANSITION_REASON_SEEK

    @Test
    fun startForcesRepeatOneAndClampsTheCount() {
        val counter = RepeatNCounter()
        assertThat(counter.start("a", 50, Player.REPEAT_MODE_ALL)).isEqualTo(Player.REPEAT_MODE_ONE)
        assertThat(counter.remaining).isEqualTo(RepeatNCounter.MAX_TIMES)
        counter.start("a", 0, Player.REPEAT_MODE_ALL)
        assertThat(counter.remaining).isEqualTo(RepeatNCounter.MIN_TIMES)
    }

    @Test
    fun countsDownThenRestoresThePreviousModeOnTheLastLoop() {
        val counter = RepeatNCounter()
        counter.start("a", 3, Player.REPEAT_MODE_ALL)

        assertThat(counter.onTransition("a", repeat)).isEqualTo(RepeatNCounter.Action.Counting(2))
        assertThat(counter.onTransition("a", repeat)).isEqualTo(RepeatNCounter.Action.Counting(1))
        assertThat(counter.onTransition("a", repeat))
            .isEqualTo(RepeatNCounter.Action.Restore(Player.REPEAT_MODE_ALL, finished = true))
        assertThat(counter.isActive).isFalse()
        // Three REPEAT transitions = three extra plays; nothing happens afterwards.
        assertThat(counter.onTransition("a", repeat)).isEqualTo(RepeatNCounter.Action.None)
    }

    @Test
    fun aSkipToAnotherSongResetsAndRestores() {
        val counter = RepeatNCounter()
        counter.start("a", 5, Player.REPEAT_MODE_OFF)
        counter.onTransition("a", repeat)

        assertThat(counter.onTransition("b", seek))
            .isEqualTo(RepeatNCounter.Action.Restore(Player.REPEAT_MODE_OFF, finished = false))
        assertThat(counter.isActive).isFalse()
        assertThat(counter.songId).isNull()
    }

    @Test
    fun sameSongTransitionsOtherThanRepeatKeepCounting() {
        val counter = RepeatNCounter()
        counter.start("a", 2, Player.REPEAT_MODE_OFF)
        assertThat(counter.onTransition("a", Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED))
            .isEqualTo(RepeatNCounter.Action.None)
        assertThat(counter.onTransition("a", auto)).isEqualTo(RepeatNCounter.Action.None)
        assertThat(counter.remaining).isEqualTo(2)
    }

    @Test
    fun repeatOneBeforehandRestoresToOff() {
        val counter = RepeatNCounter()
        counter.start("a", 2, Player.REPEAT_MODE_ONE)
        counter.onTransition("a", repeat)
        assertThat(counter.onTransition("a", repeat))
            .isEqualTo(RepeatNCounter.Action.Restore(Player.REPEAT_MODE_OFF, finished = true))
    }

    @Test
    fun choosingAnotherRepeatModeEndsTheCountWithoutRestoring() {
        val counter = RepeatNCounter()
        counter.start("a", 4, Player.REPEAT_MODE_ALL)
        assertThat(counter.onRepeatModeChanged(Player.REPEAT_MODE_ONE)).isFalse()
        assertThat(counter.isActive).isTrue()
        assertThat(counter.onRepeatModeChanged(Player.REPEAT_MODE_OFF)).isTrue()
        assertThat(counter.isActive).isFalse()
    }

    @Test
    fun cancelReturnsThePreviousModeOnce() {
        val counter = RepeatNCounter()
        counter.start("a", 4, Player.REPEAT_MODE_ALL)
        assertThat(counter.cancel()).isEqualTo(Player.REPEAT_MODE_ALL)
        assertThat(counter.cancel()).isNull()
    }
}
