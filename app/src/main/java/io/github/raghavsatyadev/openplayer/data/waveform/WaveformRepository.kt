package io.github.raghavsatyadev.openplayer.data.waveform

import android.content.Context
import android.net.Uri
import io.github.raghavsatyadev.openplayer.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Waveform bars for a song: from the cache under cacheDir/waveforms when present, otherwise
 * decoded on a background dispatcher and cached. [waveformFor] emits once: the bars, or null when
 * the song cannot be decoded (callers fall back to the plain slider). Collecting it cancels the
 * decode when the collector goes away, for example on a skip.
 */
@Singleton
class WaveformRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val cache = WaveformCache(File(context.cacheDir, "waveforms"))
    private val extractor = WaveformExtractor()
    private val decodeGate = Semaphore(1)

    fun waveformFor(song: Song, bars: Int = WaveformExtractor.DEFAULT_BARS): Flow<FloatArray?> = flow {
        val file = File(song.path)
        val size = if (file.isFile) file.length() else 0L
        val modified = if (file.isFile) file.lastModified() else song.dateModified
        val key = WaveformCache.keyFor(song.id, size, modified, bars)
        cache.read(key)?.let {
            emit(it)
            return@flow
        }
        val result = decodeGate.withPermit {
            // A concurrent collector may have finished this song while we waited.
            cache.read(key) ?: runCatching {
                extractor.extract(
                    configure = { ex ->
                        if (file.isFile) ex.setDataSource(song.path)
                        else ex.setDataSource(context, Uri.parse(song.contentUriString), null)
                    },
                    durationHintMs = song.duration,
                    bars = bars,
                )
            }.onFailure {
                if (it is kotlinx.coroutines.CancellationException) throw it
                Timber.w(it, "Waveform extraction failed for %s", song.id)
            }.getOrNull()?.also { cache.write(key, it) }
        }
        emit(result)
    }.flowOn(Dispatchers.Default)
}
