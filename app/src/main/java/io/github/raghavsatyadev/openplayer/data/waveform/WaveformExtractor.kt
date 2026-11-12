package io.github.raghavsatyadev.openplayer.data.waveform

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Decodes a song with MediaExtractor + MediaCodec, downmixes it to mono and reduces it to
 * `bars` amplitude values in 0..1 (a blend of peak and RMS per bar, normalised to the loudest bar).
 *
 * Approach (as in open-source Android players and editors that avoid FFmpeg): pick the first
 * audio track, run it through a decoder, read the PCM from the output buffers (16-bit or float,
 * as the output format says) and fold it into time buckets. Files longer than
 * [FULL_DECODE_LIMIT_MS] are not decoded end to end: the extractor seeks to the start of every bar
 * and only a short window is decoded there, which keeps long podcasts and mixes fast.
 */
class WaveformExtractor {

    /** [configure] points the extractor at the song (path or content Uri). Null when undecodable. */
    suspend fun extract(
        configure: (MediaExtractor) -> Unit,
        durationHintMs: Long,
        bars: Int = DEFAULT_BARS,
    ): FloatArray? {
        require(bars > 0)
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            configure(extractor)
            val trackIndex = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION)
            } else {
                durationHintMs * 1000L
            }
            if (durationUs <= 0L) return null
            extractor.selectTrack(trackIndex)
            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }
            val acc = BarAccumulator(bars, durationUs)
            if (durationUs / 1000L > FULL_DECODE_LIMIT_MS) {
                for (bar in 0 until bars) {
                    currentCoroutineContext().ensureActive()
                    extractor.seekTo(durationUs * bar / bars, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    codec.flush()
                    acc.fixedBar = bar
                    decode(extractor, codec, acc, SPARSE_WINDOW_US)
                }
            } else {
                acc.fixedBar = -1
                decode(extractor, codec, acc, Long.MAX_VALUE)
            }
            return acc.result()
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    /** Decodes until [maxDecodedUs] of audio has been read, or the stream ends. */
    private suspend fun decode(
        extractor: MediaExtractor,
        codec: MediaCodec,
        acc: BarAccumulator,
        maxDecodedUs: Long,
    ) {
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var channels = 1
        var float = false
        var sampleRate = 44100
        var decodedFrames = 0L
        var stalls = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            if (!inputDone) {
                val inIdx = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inIdx >= 0) {
                    val buf = codec.getInputBuffer(inIdx)!!
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIdx = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            when {
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val f = codec.outputFormat
                    channels = f.intOr(MediaFormat.KEY_CHANNEL_COUNT, channels).coerceAtLeast(1)
                    sampleRate = f.intOr(MediaFormat.KEY_SAMPLE_RATE, sampleRate).coerceAtLeast(1)
                    float = f.intOr(MediaFormat.KEY_PCM_ENCODING, PCM_16BIT) == PCM_FLOAT
                }
                outIdx >= 0 -> {
                    stalls = 0
                    val out = codec.getOutputBuffer(outIdx)
                    if (out != null && info.size > 0) {
                        out.position(info.offset)
                        out.limit(info.offset + info.size)
                        decodedFrames += fold(out, channels, float, sampleRate, info.presentationTimeUs, acc)
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                    if (decodedFrames * 1_000_000L / sampleRate >= maxDecodedUs) return
                }
                outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    // Decoder stalled with all input delivered: stop instead of spinning forever.
                    if (inputDone && ++stalls > 200) return
                }
            }
        }
    }

    /** Downmixes one output buffer to mono and adds it to [acc]; returns the frame count. */
    private fun fold(
        out: ByteBuffer,
        channels: Int,
        float: Boolean,
        sampleRate: Int,
        ptsUs: Long,
        acc: BarAccumulator,
    ): Int {
        out.order(ByteOrder.nativeOrder())
        val bytesPer = if (float) 4 else 2
        val frames = out.remaining() / (bytesPer * channels)
        val usPerFrame = 1_000_000.0 / sampleRate
        for (i in 0 until frames) {
            var sum = 0f
            for (c in 0 until channels) {
                sum += if (float) out.getFloat() else out.getShort() / 32768f
            }
            acc.add(ptsUs + (i * usPerFrame).toLong(), sum / channels)
        }
        return frames
    }

    private fun MediaFormat.intOr(key: String, default: Int) =
        if (containsKey(key)) getInteger(key) else default

    /** Collects per-bar peak and energy. A non-negative [fixedBar] forces every sample into it. */
    private class BarAccumulator(private val bars: Int, private val durationUs: Long) {
        private val peak = FloatArray(bars)
        private val sumSq = DoubleArray(bars)
        private val count = IntArray(bars)
        var fixedBar = -1

        fun add(ptsUs: Long, v: Float) {
            val bar = if (fixedBar >= 0) fixedBar
            else ((ptsUs.coerceAtLeast(0L) * bars) / durationUs).toInt().coerceIn(0, bars - 1)
            val a = abs(v)
            if (a > peak[bar]) peak[bar] = a
            sumSq[bar] += (v * v).toDouble()
            count[bar]++
        }

        fun result(): FloatArray {
            val raw = FloatArray(bars) { i ->
                if (count[i] == 0) 0f else {
                    val rms = sqrt(sumSq[i] / count[i]).toFloat()
                    0.4f * peak[i] + 0.6f * rms
                }
            }
            val max = raw.max()
            if (max <= 1e-6f) return FloatArray(bars)
            return FloatArray(bars) { (raw[it] / max).coerceIn(0f, 1f) }
        }
    }

    companion object {
        const val DEFAULT_BARS = 160
        const val FULL_DECODE_LIMIT_MS = 10 * 60 * 1000L
        private const val SPARSE_WINDOW_US = 80_000L
        private const val TIMEOUT_US = 5_000L
        private const val PCM_16BIT = 2
        private const val PCM_FLOAT = 4
    }
}
