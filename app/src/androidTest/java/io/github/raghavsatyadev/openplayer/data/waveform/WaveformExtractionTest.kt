package io.github.raghavsatyadev.openplayer.data.waveform

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.model.Song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/** Extraction on a generated sine WAV whose amplitude ramps up, and the file cache. */
@RunWith(AndroidJUnit4::class)
class WaveformExtractionTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val wav = File(context.cacheDir, "waveform_test_ramp.wav")
    private val cacheDir = File(context.cacheDir, "waveforms")

    @After
    fun cleanUp() {
        wav.delete()
        cacheDir.deleteRecursively()
    }

    private fun writeRampWav(seconds: Int = 3, rate: Int = 44100) {
        val frames = seconds * rate
        val data = ByteBuffer.allocate(frames * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) {
            val amp = 0.05 + 0.85 * i / frames
            data.putShort((sin(2 * PI * 440 * i / rate) * amp * Short.MAX_VALUE).toInt().toShort())
        }
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray()).putInt(36 + frames * 2).put("WAVE".toByteArray())
        header.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate)
            .putInt(rate * 2).putShort(2).putShort(16)
        header.put("data".toByteArray()).putInt(frames * 2)
        wav.outputStream().use {
            it.write(header.array())
            it.write(data.array())
        }
    }

    private fun song() = Song.emptySong().copy(
        id = "waveform-test",
        path = wav.absolutePath,
        duration = 3000L,
        dateModified = 1L,
    )

    @Test
    fun extractedBarsRiseAndStayInRange() = runBlocking {
        writeRampWav()
        val bars = WaveformExtractor().extract(
            configure = { it.setDataSource(wav.absolutePath) },
            durationHintMs = 3000L,
            bars = 30,
        )
        assertThat(bars).isNotNull()
        bars!!
        assertThat(bars.size).isEqualTo(30)
        bars.forEach { assertThat(it).isAtLeast(0f) }
        bars.forEach { assertThat(it).isAtMost(1f) }
        for (i in 1 until bars.size) assertThat(bars[i]).isAtLeast(bars[i - 1] - 0.01f)
        assertThat(bars.last()).isWithin(1e-4f).of(1f)
        assertThat(bars.last() - bars.first()).isGreaterThan(0.5f)
    }

    @Test
    fun repositoryCachesAndReturnsTheSameArray() = runBlocking {
        writeRampWav()
        val repository = WaveformRepository(context)
        val first = repository.waveformFor(song()).first()
        assertThat(first).isNotNull()
        assertThat(cacheDir.listFiles { f -> f.name.endsWith(".wf") }).isNotEmpty()

        // A second call (a fresh repository too) is answered from the cache file.
        assertThat(repository.waveformFor(song()).first()).isEqualTo(first)
        assertThat(WaveformRepository(context).waveformFor(song()).first()).isEqualTo(first)
    }

    @Test
    fun cacheTrimsLeastRecentlyUsedFirst() {
        val cache = WaveformCache(cacheDir, maxBytes = 3 * (8 + 4 * 100L))
        val bars = FloatArray(100) { it / 100f }
        cache.write("a", bars)
        Thread.sleep(20)
        cache.write("b", bars)
        Thread.sleep(20)
        cache.write("c", bars)
        Thread.sleep(20)
        cache.read("a")
        Thread.sleep(20)
        cache.write("d", bars)
        assertThat(cache.read("b")).isNull()
        assertThat(cache.read("a")).isEqualTo(bars)
        assertThat(cache.read("d")).isEqualTo(bars)
    }
}
