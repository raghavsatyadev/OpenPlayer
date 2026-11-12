package io.github.raghavsatyadev.openplayer.data.equalizer.parametric

import java.util.Locale

/**
 * Reads and writes the AutoEQ / Equalizer APO `ParametricEQ.txt` format:
 *
 *     Preamp: -6.2 dB
 *     Filter 1: ON LSC Fc 105 Hz Gain 5.5 dB Q 0.70
 *
 * Unknown lines are ignored. OFF filters are kept but disabled. Values are clamped.
 */
object AutoEqParser {

    private const val DEFAULT_Q = 0.707f

    private val preampRegex = Regex("""^\s*Preamp\s*:\s*(-?\d+(?:\.\d+)?)\s*dB\s*$""", RegexOption.IGNORE_CASE)

    private val filterRegex = Regex(
        """^\s*Filter\s*\d*\s*:\s*(ON|OFF)\s+([A-Za-z]+)\s+Fc\s+(-?\d+(?:\.\d+)?)\s*(?:Hz)?""" +
            """(?:\s+Gain\s+(-?\d+(?:\.\d+)?)\s*(?:dB)?)?(?:\s+Q\s+(-?\d+(?:\.\d+)?))?""",
        RegexOption.IGNORE_CASE,
    )

    private fun typeOf(code: String): ParametricBandType? = when (code.uppercase(Locale.ROOT)) {
        "PK", "PEQ" -> ParametricBandType.PEAK
        "LSC", "LS", "LSQ" -> ParametricBandType.LOW_SHELF
        "HSC", "HS", "HSQ" -> ParametricBandType.HIGH_SHELF
        else -> null
    }

    /** Parses [text]; the result has no bands when nothing valid was found. */
    fun parse(text: String, name: String = "AutoEQ"): ParametricEqProfile {
        var preamp = 0f
        val bands = ArrayList<ParametricBand>()
        for (line in text.lineSequence()) {
            val pm = preampRegex.matchEntire(line)
            if (pm != null) {
                pm.groupValues[1].toFloatOrNull()?.let { preamp = it }
                continue
            }
            val m = filterRegex.find(line) ?: continue
            val type = typeOf(m.groupValues[2]) ?: continue
            val fc = m.groupValues[3].toFloatOrNull() ?: continue
            val gain = m.groupValues[4].toFloatOrNull() ?: 0f
            val q = m.groupValues[5].toFloatOrNull() ?: DEFAULT_Q
            bands += ParametricBand(
                type = type,
                frequencyHz = fc,
                gainDb = gain,
                q = q,
                enabled = m.groupValues[1].equals("ON", ignoreCase = true),
            ).clamped()
        }
        return ParametricEqProfile(
            name = name,
            preampDb = preamp.coerceIn(-ParametricEqProfile.MAX_PREAMP_DB, ParametricEqProfile.MAX_PREAMP_DB),
            bands = bands,
        )
    }

    fun export(profile: ParametricEqProfile): String = buildString {
        append(String.format(Locale.US, "Preamp: %.1f dB\n", profile.preampDb))
        profile.bands.forEachIndexed { i, b ->
            val code = when (b.type) {
                ParametricBandType.PEAK -> "PK"
                ParametricBandType.LOW_SHELF -> "LSC"
                ParametricBandType.HIGH_SHELF -> "HSC"
            }
            append(
                String.format(
                    Locale.US, "Filter %d: %s %s Fc %s Hz Gain %.1f dB Q %.2f\n",
                    i + 1, if (b.enabled) "ON" else "OFF", code, formatFreq(b.frequencyHz), b.gainDb, b.q,
                ),
            )
        }
    }

    private fun formatFreq(f: Float): String =
        if (f == Math.rint(f.toDouble()).toFloat()) f.toInt().toString() else String.format(Locale.US, "%.1f", f)
}
