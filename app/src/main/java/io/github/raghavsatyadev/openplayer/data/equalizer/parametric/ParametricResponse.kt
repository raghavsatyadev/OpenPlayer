package io.github.raghavsatyadev.openplayer.data.equalizer.parametric

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Frequency response of a [ParametricEqProfile] from the RBJ Audio EQ Cookbook biquads at 48 kHz,
 * plus the band sampling DynamicsProcessing needs (it has no peak/shelf filters, only per-band gains).
 */
object ParametricResponse {
    const val SAMPLE_RATE = 48000.0
    const val MIN_HZ = 20.0
    const val MAX_HZ = 20000.0

    /** Magnitude of one band at [freqHz] in dB. */
    fun bandGainDb(band: ParametricBand, freqHz: Double): Double {
        if (band.gainDb == 0f) return 0.0
        val a = 10.0.pow(band.gainDb / 40.0)
        val w0 = 2.0 * PI * band.frequencyHz / SAMPLE_RATE
        val cw = cos(w0)
        val alpha = sin(w0) / (2.0 * band.q)
        val beta = 2.0 * sqrt(a) * alpha
        val b0: Double
        val b1: Double
        val b2: Double
        val a0: Double
        val a1: Double
        val a2: Double
        when (band.type) {
            ParametricBandType.PEAK -> {
                b0 = 1 + alpha * a; b1 = -2 * cw; b2 = 1 - alpha * a
                a0 = 1 + alpha / a; a1 = -2 * cw; a2 = 1 - alpha / a
            }
            ParametricBandType.LOW_SHELF -> {
                b0 = a * ((a + 1) - (a - 1) * cw + beta)
                b1 = 2 * a * ((a - 1) - (a + 1) * cw)
                b2 = a * ((a + 1) - (a - 1) * cw - beta)
                a0 = (a + 1) + (a - 1) * cw + beta
                a1 = -2 * ((a - 1) + (a + 1) * cw)
                a2 = (a + 1) + (a - 1) * cw - beta
            }
            ParametricBandType.HIGH_SHELF -> {
                b0 = a * ((a + 1) + (a - 1) * cw + beta)
                b1 = -2 * a * ((a - 1) + (a + 1) * cw)
                b2 = a * ((a + 1) + (a - 1) * cw - beta)
                a0 = (a + 1) - (a - 1) * cw + beta
                a1 = 2 * ((a - 1) - (a + 1) * cw)
                a2 = (a + 1) - (a - 1) * cw - beta
            }
        }
        val w = 2.0 * PI * freqHz / SAMPLE_RATE
        val c1 = cos(w)
        val c2 = cos(2 * w)
        val num = b0 * b0 + b1 * b1 + b2 * b2 + 2 * (b0 * b1 + b1 * b2) * c1 + 2 * b0 * b2 * c2
        val den = a0 * a0 + a1 * a1 + a2 * a2 + 2 * (a0 * a1 + a1 * a2) * c1 + 2 * a0 * a2 * c2
        return 10.0 * log10((num / den).coerceAtLeast(1e-12))
    }

    /** Sum of the enabled bands' gains at [freqHz] (without preamp). */
    fun bandsGainDb(profile: ParametricEqProfile, freqHz: Double): Double =
        profile.bands.sumOf { if (it.enabled) bandGainDb(it, freqHz) else 0.0 }

    /** preamp + sum of the enabled bands' gains at [freqHz]. */
    fun combinedGainDb(profile: ParametricEqProfile, freqHz: Double): Double =
        profile.preampDb + bandsGainDb(profile, freqHz)

    /**
     * Upper edges (Hz) of [count] log-spaced bands between 20 Hz and 20 kHz; strictly increasing,
     * the last one is 20 kHz.
     */
    fun bandCutoffs(count: Int): DoubleArray = DoubleArray(count) { i ->
        MIN_HZ * (MAX_HZ / MIN_HZ).pow((i + 1).toDouble() / count)
    }

    /**
     * Per-band gain for [cutoffs]: [bandsGainDb] (preamp excluded, it goes to input gain) sampled at
     * each band's geometric centre.
     */
    fun sampleBandGains(profile: ParametricEqProfile, cutoffs: DoubleArray): FloatArray =
        FloatArray(cutoffs.size) { i ->
            val lower = if (i == 0) MIN_HZ else cutoffs[i - 1]
            bandsGainDb(profile, sqrt(lower * cutoffs[i])).toFloat()
        }
}
