package io.github.raghavsatyadev.openplayer.data.equalizer

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricBand
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricBandType
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricEqProfile
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricResponse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParametricResponseTest {

    private fun profile(preamp: Float = 0f, vararg bands: ParametricBand) = ParametricEqProfile("t", preamp, bands.toList())

    @Test
    fun peakGivesItsGainAtFcAndNothingFarAway() {
        val p = profile(0f, ParametricBand(ParametricBandType.PEAK, 1000f, 6f, 2f))
        assertThat(ParametricResponse.combinedGainDb(p, 1000.0)).isWithin(0.05).of(6.0)
        assertThat(ParametricResponse.combinedGainDb(p, 30.0)).isWithin(0.1).of(0.0)
        assertThat(ParametricResponse.combinedGainDb(p, 18000.0)).isWithin(0.3).of(0.0)
    }

    @Test
    fun shelvesReachTheirGainOnTheirSide() {
        val low = profile(0f, ParametricBand(ParametricBandType.LOW_SHELF, 1000f, 5f, 0.7f))
        assertThat(ParametricResponse.combinedGainDb(low, 20.0)).isWithin(0.2).of(5.0)
        assertThat(ParametricResponse.combinedGainDb(low, 18000.0)).isWithin(0.2).of(0.0)
        val high = profile(0f, ParametricBand(ParametricBandType.HIGH_SHELF, 4000f, -4f, 0.7f))
        assertThat(ParametricResponse.combinedGainDb(high, 18000.0)).isWithin(0.5).of(-4.0)
        assertThat(ParametricResponse.combinedGainDb(high, 50.0)).isWithin(0.1).of(0.0)
    }

    @Test
    fun preampOffsetsEverythingAndDisabledBandsAreSkipped() {
        val band = ParametricBand(ParametricBandType.PEAK, 1000f, 6f, 2f, enabled = false)
        val p = profile(-3f, band)
        assertThat(ParametricResponse.combinedGainDb(p, 1000.0)).isWithin(0.001).of(-3.0)
        val on = p.copy(bands = listOf(band.copy(enabled = true)))
        assertThat(ParametricResponse.combinedGainDb(on, 1000.0)).isWithin(0.05).of(3.0)
    }

    @Test
    fun bandsSumInDb() {
        val b = ParametricBand(ParametricBandType.PEAK, 1000f, 3f, 1f)
        val one = ParametricResponse.combinedGainDb(profile(0f, b), 1000.0)
        val two = ParametricResponse.combinedGainDb(profile(0f, b, b), 1000.0)
        assertThat(two).isWithin(0.001).of(2 * one)
    }

    @Test
    fun samplingGivesIncreasingCutoffsAndPerBandGains() {
        val cutoffs = ParametricResponse.bandCutoffs(48)
        assertThat(cutoffs).hasLength(48)
        for (i in 1 until cutoffs.size) assertThat(cutoffs[i]).isGreaterThan(cutoffs[i - 1])
        assertThat(cutoffs.last()).isWithin(0.01).of(20000.0)
        assertThat(cutoffs.first()).isGreaterThan(20.0)

        val p = profile(-2f, ParametricBand(ParametricBandType.PEAK, 1000f, 6f, 2f))
        val gains = ParametricResponse.sampleBandGains(p, cutoffs)
        assertThat(gains).hasLength(48)
        val peakIndex = cutoffs.indexOfFirst { it >= 1000.0 }
        assertThat(gains[peakIndex]).isGreaterThan(3f)
        assertThat(gains[0]).isWithin(0.2f).of(0f) // preamp is not part of the band gains
    }
}
