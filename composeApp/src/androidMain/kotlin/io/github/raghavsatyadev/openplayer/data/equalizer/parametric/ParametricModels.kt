package io.github.raghavsatyadev.openplayer.data.equalizer.parametric

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class ParametricBandType { PEAK, LOW_SHELF, HIGH_SHELF }

@Serializable
data class ParametricBand(
    val type: ParametricBandType = ParametricBandType.PEAK,
    val frequencyHz: Float = 1000f,
    val gainDb: Float = 0f,
    val q: Float = 1f,
    val enabled: Boolean = true,
) {
    /** The same band with frequency, gain and Q forced into the supported ranges. */
    fun clamped(): ParametricBand = copy(
        frequencyHz = frequencyHz.coerceIn(MIN_FREQ_HZ, MAX_FREQ_HZ),
        gainDb = gainDb.coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB),
        q = q.coerceIn(MIN_Q, MAX_Q),
    )

    companion object {
        const val MIN_FREQ_HZ = 20f
        const val MAX_FREQ_HZ = 20000f
        const val MAX_GAIN_DB = 24f
        const val MIN_Q = 0.1f
        const val MAX_Q = 10f
    }
}

@Serializable
data class ParametricEqProfile(
    val name: String = "Custom",
    val preampDb: Float = 0f,
    val bands: List<ParametricBand> = emptyList(),
) {
    val hasEnabledBands: Boolean get() = bands.any { it.enabled }

    companion object {
        const val MAX_PREAMP_DB = 24f
        val EMPTY = ParametricEqProfile()
    }
}

/** JSON codec used for the preference value. Unknown keys are ignored and bad input decodes to null. */
object ParametricProfileCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(profile: ParametricEqProfile): String = json.encodeToString(profile)

    fun decode(text: String?): ParametricEqProfile? {
        if (text.isNullOrBlank()) return null
        return try {
            val p = json.decodeFromString<ParametricEqProfile>(text)
            p.copy(
                preampDb = p.preampDb.coerceIn(-ParametricEqProfile.MAX_PREAMP_DB, ParametricEqProfile.MAX_PREAMP_DB),
                bands = p.bands.map { it.clamped() },
            )
        } catch (_: Exception) {
            null
        }
    }
}
