package io.github.raghavsatyadev.openplayer.data.history

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ListenCountRuleTest {

    private val rule = ListenCountRule(minSeconds = 40, minPercentage = 40)

    @Test
    fun `whichever comes first counts for a long track`() {
        // 40% of 10 min is 4 min, so 40 s wins.
        assertThat(rule.requiredListenMs(600_000L)).isEqualTo(40_000L)
    }

    @Test
    fun `percentage wins for a short track`() {
        // 40% of 60 s is 24 s, less than 40 s.
        assertThat(rule.requiredListenMs(60_000L)).isEqualTo(24_000L)
    }

    @Test
    fun `unknown length falls back to seconds`() {
        assertThat(rule.requiredListenMs(0L)).isEqualTo(40_000L)
    }

    @Test
    fun `isCounted needs the full amount`() {
        assertThat(rule.isCounted(39_999L, 600_000L)).isFalse()
        assertThat(rule.isCounted(40_000L, 600_000L)).isTrue()
        assertThat(rule.isCounted(0L, 0L)).isFalse()
    }

    @Test
    fun `out of range settings are clamped`() {
        val wild = ListenCountRule(minSeconds = 1, minPercentage = 500)
        assertThat(wild.requiredListenMs(0L)).isEqualTo(ListenCountRule.SECONDS_RANGE.first * 1000L)
        assertThat(wild.sanitized().minPercentage).isEqualTo(100)
    }

    @Test
    fun `restart means going back to the start or back by a quarter`() {
        val duration = 200_000L
        assertThat(rule.isRestart(190_000L, 500L, duration)).isTrue()
        assertThat(rule.isRestart(150_000L, 100_000L, duration)).isTrue()
        assertThat(rule.isRestart(150_000L, 120_000L, duration)).isFalse()
        assertThat(rule.isRestart(10_000L, 20_000L, duration)).isFalse()
    }
}
