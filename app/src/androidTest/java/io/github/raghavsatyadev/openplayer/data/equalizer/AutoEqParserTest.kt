package io.github.raghavsatyadev.openplayer.data.equalizer

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.AutoEqParser
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricBandType
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricEqProfile
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricProfileCodec
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutoEqParserTest {

    private val sample = """
        Preamp: -6.2 dB
        Filter 1: ON LSC Fc 105 Hz Gain 5.5 dB Q 0.70
        Filter 2: ON PK Fc 2210 Hz Gain -3.1 dB Q 1.41
        Filter 10: ON HSC Fc 10000 Hz Gain -2.0 dB Q 0.70
    """.trimIndent()

    @Test
    fun parsesSample() {
        val p = AutoEqParser.parse(sample)
        assertThat(p.preampDb).isWithin(0.001f).of(-6.2f)
        assertThat(p.bands).hasSize(3)
        assertThat(p.bands[0].type).isEqualTo(ParametricBandType.LOW_SHELF)
        assertThat(p.bands[0].frequencyHz).isEqualTo(105f)
        assertThat(p.bands[0].gainDb).isEqualTo(5.5f)
        assertThat(p.bands[0].q).isEqualTo(0.70f)
        assertThat(p.bands[1].type).isEqualTo(ParametricBandType.PEAK)
        assertThat(p.bands[1].gainDb).isEqualTo(-3.1f)
        assertThat(p.bands[2].type).isEqualTo(ParametricBandType.HIGH_SHELF)
        assertThat(p.bands.all { it.enabled }).isTrue()
    }

    @Test
    fun offFiltersAreKeptButDisabled() {
        val p = AutoEqParser.parse("Filter 1: OFF PK Fc 1000 Hz Gain 3.0 dB Q 1.00\nFilter 2: ON PK Fc 2000 Hz Gain 1.0 dB Q 1.00")
        assertThat(p.bands).hasSize(2)
        assertThat(p.bands[0].enabled).isFalse()
        assertThat(p.bands[1].enabled).isTrue()
    }

    @Test
    fun aliasesMapToTypes() {
        val text = listOf("PK", "PEQ", "LSC", "LS", "LSQ", "HSC", "HS", "HSQ")
            .mapIndexed { i, c -> "Filter ${i + 1}: ON $c Fc 1000 Hz Gain 1.0 dB Q 1.00" }
            .joinToString("\n")
        val types = AutoEqParser.parse(text).bands.map { it.type }
        assertThat(types).containsExactly(
            ParametricBandType.PEAK, ParametricBandType.PEAK,
            ParametricBandType.LOW_SHELF, ParametricBandType.LOW_SHELF, ParametricBandType.LOW_SHELF,
            ParametricBandType.HIGH_SHELF, ParametricBandType.HIGH_SHELF, ParametricBandType.HIGH_SHELF,
        ).inOrder()
    }

    @Test
    fun garbageLinesAreIgnored() {
        val p = AutoEqParser.parse("hello\n\nFilter: nonsense\nFilter 1: ON XYZ Fc 100 Hz Gain 1 dB Q 1\n$sample\n# comment")
        assertThat(p.bands).hasSize(3)
        assertThat(AutoEqParser.parse("nothing useful here").bands).isEmpty()
        assertThat(AutoEqParser.parse("").bands).isEmpty()
    }

    @Test
    fun valuesAreClamped() {
        val p = AutoEqParser.parse("Preamp: -90 dB\nFilter 1: ON PK Fc 5 Hz Gain 40.0 dB Q 0.01\nFilter 2: ON PK Fc 30000 Hz Gain -40.0 dB Q 50")
        assertThat(p.preampDb).isEqualTo(-24f)
        assertThat(p.bands[0].frequencyHz).isEqualTo(20f)
        assertThat(p.bands[0].gainDb).isEqualTo(24f)
        assertThat(p.bands[0].q).isEqualTo(0.1f)
        assertThat(p.bands[1].frequencyHz).isEqualTo(20000f)
        assertThat(p.bands[1].gainDb).isEqualTo(-24f)
        assertThat(p.bands[1].q).isEqualTo(10f)
    }

    @Test
    fun exportParsesBackToTheSameProfile() {
        val original = AutoEqParser.parse(sample + "\nFilter 4: OFF PK Fc 500.5 Hz Gain 1.5 dB Q 2.00")
        val again = AutoEqParser.parse(AutoEqParser.export(original))
        assertThat(again.preampDb).isWithin(0.05f).of(original.preampDb)
        assertThat(again.bands).hasSize(original.bands.size)
        original.bands.zip(again.bands).forEach { (a, b) ->
            assertThat(b.type).isEqualTo(a.type)
            assertThat(b.enabled).isEqualTo(a.enabled)
            assertThat(b.frequencyHz).isWithin(0.1f).of(a.frequencyHz)
            assertThat(b.gainDb).isWithin(0.05f).of(a.gainDb)
            assertThat(b.q).isWithin(0.005f).of(a.q)
        }
    }

    @Test
    fun jsonCodecRoundTripsAndRejectsGarbage() {
        val p = AutoEqParser.parse(sample)
        assertThat(ParametricProfileCodec.decode(ParametricProfileCodec.encode(p))).isEqualTo(p)
        assertThat(ParametricProfileCodec.decode("not json")).isNull()
        assertThat(ParametricProfileCodec.decode(null)).isNull()
        assertThat(ParametricEqProfile.EMPTY.bands).isEmpty()
    }
}
