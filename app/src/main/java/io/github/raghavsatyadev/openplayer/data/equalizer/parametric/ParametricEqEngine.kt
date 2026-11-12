package io.github.raghavsatyadev.openplayer.data.equalizer.parametric

import android.media.audiofx.DynamicsProcessing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import co.touchlab.kermit.Logger

/**
 * Applies a [ParametricEqProfile] with [DynamicsProcessing].
 *
 * DynamicsProcessing has no peak/shelf filters, only a gain per band with the band's upper cutoff
 * frequency. We therefore configure [BAND_COUNT] log-spaced pre-EQ bands from 20 Hz to 20 kHz (cutoff =
 * band upper edge) and set each band's gain to the profile's response sampled at the band's geometric
 * centre. The profile's preamp goes to the input gain. MBC, post-EQ and the limiter are not in use:
 * AutoEQ preamps already leave headroom and a limiter would colour the sound.
 *
 * Created lazily: nothing exists until the engine is enabled and has an audio session. If construction
 * fails (some devices/sessions throw) the engine is marked unavailable for this process.
 */
class ParametricEqEngine {

    /** What the underlying effect reports, for tests and diagnostics. */
    data class AppliedState(val cutoffsHz: FloatArray, val gainsDb: FloatArray, val inputGainDb: Float)

    private var processor: DynamicsProcessing? = null
    private var sessionId = 0
    private var cutoffs = ParametricResponse.bandCutoffs(BAND_COUNT)

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _profile = MutableStateFlow(ParametricEqProfile.EMPTY)
    val profile: StateFlow<ParametricEqProfile> = _profile.asStateFlow()

    private val _available = MutableStateFlow(true)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    val isAttached: Boolean get() = processor != null

    /** Remembers [audioSessionId] and builds the effect if the engine is enabled. */
    fun attach(audioSessionId: Int) {
        if (audioSessionId != sessionId) releaseEffect()
        sessionId = audioSessionId
        ensureEffect()
    }

    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        if (enabled) ensureEffect() else processor?.runCatching { this.enabled = false }
        if (enabled) apply()
    }

    fun setProfile(profile: ParametricEqProfile) {
        _profile.value = profile
        apply()
    }

    fun readBack(): AppliedState? {
        val dp = processor ?: return null
        return try {
            val eq = dp.getPreEqByChannelIndex(0)
            val n = eq.bandCount
            AppliedState(
                FloatArray(n) { eq.getBand(it).cutoffFrequency },
                FloatArray(n) { eq.getBand(it).gain },
                dp.getInputGainByChannelIndex(0),
            )
        } catch (e: Exception) {
            Logger.withTag(TAG).w(e) { "Could not read back DynamicsProcessing state" }
            null
        }
    }

    fun release() {
        releaseEffect()
        sessionId = 0
    }

    private fun releaseEffect() {
        processor?.runCatching { release() }
        processor = null
    }

    private fun ensureEffect() {
        if (processor != null || !_enabled.value || sessionId == 0 || !_available.value) return
        for (count in BAND_COUNT_FALLBACKS) {
            try {
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    CHANNELS, true, count, false, 0, false, 0, false,
                ).build()
                processor = DynamicsProcessing(0, sessionId, config)
                cutoffs = ParametricResponse.bandCutoffs(count)
                break
            } catch (e: Exception) {
                Logger.withTag(TAG).w(e) { String.format("DynamicsProcessing with %d bands failed", count) }
                releaseEffect()
            }
        }
        if (processor == null) {
            _available.value = false
            _enabled.value = false
            Logger.withTag(TAG).w { "Parametric EQ unavailable for this process" }
            return
        }
        apply()
    }

    private fun apply() {
        val dp = processor ?: return
        try {
            val profile = _profile.value
            val gains = ParametricResponse.sampleBandGains(profile, cutoffs)
            val eq = DynamicsProcessing.Eq(true, true, cutoffs.size)
            for (i in cutoffs.indices) {
                eq.setBand(i, DynamicsProcessing.EqBand(true, cutoffs[i].toFloat(), gains[i].coerceIn(-MAX_BAND_DB, MAX_BAND_DB)))
            }
            dp.setPreEqAllChannelsTo(eq)
            dp.setInputGainAllChannelsTo(profile.preampDb)
            dp.enabled = _enabled.value
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "Failed applying parametric EQ" }
        }
    }

    companion object {
        private const val TAG = "ParametricEqEngine"
        const val BAND_COUNT = 48
        private val BAND_COUNT_FALLBACKS = listOf(BAND_COUNT, 32, 16)
        private const val CHANNELS = 2
        private const val MAX_BAND_DB = 30f
    }
}
