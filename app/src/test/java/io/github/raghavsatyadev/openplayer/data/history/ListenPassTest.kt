package io.github.raghavsatyadev.openplayer.data.history

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ListenPassTest {

    private val rule = ListenCountRule(minSeconds = 40, minPercentage = 40)
    private val duration = 180_000L

    @Test
    fun `a skip before the threshold is not a listen`() {
        val pass = ListenPass("a", startedAtEpochMs = 1_000L, startPositionMs = 0L)
        pass.advance(30_000L, 30_000L, duration, 31_000L, rule)
        assertThat(pass.close(31_000L, duration, rule)).isNull()
    }

    @Test
    fun `enough listening is one listen with its times`() {
        val pass = ListenPass("a", startedAtEpochMs = 1_000L, startPositionMs = 0L)
        pass.advance(20_000L, 20_000L, duration, 21_000L, rule)
        pass.advance(25_000L, 45_000L, duration, 46_000L, rule)
        val listen = pass.close(46_000L, duration, rule)
        assertThat(listen).isEqualTo(
            CountedListen("a", startedAtEpochMs = 1_000L, endedAtEpochMs = 46_000L, listenedMs = 45_000L, trackDurationMs = duration)
        )
    }

    @Test
    fun `repeat one counts each loop`() {
        val pass = ListenPass("a", startedAtEpochMs = 0L, startPositionMs = 0L)
        assertThat(pass.advance(179_000L, 179_000L, duration, 179_000L, rule)).isNull()
        val first = pass.advance(1_000L, 0L, duration, 180_000L, rule)
        assertThat(first?.listenedMs).isEqualTo(180_000L)
        pass.advance(60_000L, 60_000L, duration, 240_000L, rule)
        val second = pass.close(240_000L, duration, rule)
        assertThat(second?.startedAtEpochMs).isEqualTo(180_000L)
        assertThat(second?.listenedMs).isEqualTo(60_000L)
    }

    @Test
    fun `seeking back before a listen counted keeps adding to the same pass`() {
        val pass = ListenPass("a", startedAtEpochMs = 0L, startPositionMs = 0L)
        pass.advance(20_000L, 20_000L, duration, 20_000L, rule)
        assertThat(pass.advance(0L, 0L, duration, 20_000L, rule)).isNull()
        pass.advance(20_000L, 20_000L, duration, 40_000L, rule)
        assertThat(pass.close(40_000L, duration, rule)?.listenedMs).isEqualTo(40_000L)
    }

    @Test
    fun `a seek back after a listen starts a new pass that must count on its own`() {
        val pass = ListenPass("a", startedAtEpochMs = 0L, startPositionMs = 0L)
        pass.advance(90_000L, 90_000L, duration, 90_000L, rule)
        assertThat(pass.advance(0L, 0L, duration, 90_000L, rule)).isNotNull()
        pass.advance(10_000L, 10_000L, duration, 100_000L, rule)
        assertThat(pass.close(100_000L, duration, rule)).isNull()
    }
}
