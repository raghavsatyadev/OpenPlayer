package io.github.raghavsatyadev.openplayer.data.equalizer

import android.content.Context
import android.media.AudioManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.AutoEqParser
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricEqEngine
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Builds a real DynamicsProcessing on a fresh audio session and reads back what was applied. */
@RunWith(AndroidJUnit4::class)
class ParametricEqEngineTest {

    @Test
    fun appliesProfileToDynamicsProcessing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val engine = ParametricEqEngine()
        try {
            engine.setProfile(
                AutoEqParser.parse("Preamp: -4.0 dB\nFilter 1: ON PK Fc 1000 Hz Gain 6.0 dB Q 2.00\nFilter 2: ON LSC Fc 100 Hz Gain 3.0 dB Q 0.70"),
            )
            engine.setEnabled(true)
            engine.attach(audio.generateAudioSessionId())
            assumeTrue("DynamicsProcessing could not be created on this device", engine.isAttached)

            val state = engine.readBack()
            assumeTrue("Could not read the effect back", state != null)
            state!!
            assertThat(state.cutoffsHz.size).isAtLeast(16)
            assertThat(state.gainsDb.size).isEqualTo(state.cutoffsHz.size)
            for (i in 1 until state.cutoffsHz.size) assertThat(state.cutoffsHz[i]).isGreaterThan(state.cutoffsHz[i - 1])
            assertThat(state.inputGainDb).isWithin(0.01f).of(-4f)
            val peak = state.cutoffsHz.indexOfFirst { it >= 1000f }
            assertThat(state.gainsDb[peak]).isGreaterThan(3f)
            assertThat(state.gainsDb.last()).isWithin(0.5f).of(0f)

            engine.setProfile(AutoEqParser.parse("Preamp: -1.0 dB"))
            assertThat(engine.readBack()!!.inputGainDb).isWithin(0.01f).of(-1f)
        } finally {
            engine.release()
        }
    }
}
