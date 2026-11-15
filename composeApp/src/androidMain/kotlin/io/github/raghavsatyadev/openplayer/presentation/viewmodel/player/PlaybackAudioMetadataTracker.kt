package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import co.touchlab.kermit.Logger
import io.github.raghavsatyadev.openplayer.di.DispatcherProvider
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlaybackAudioMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Format info (mime type, bitrate, sample rate, channels, bit depth) for the playing track,
 * extracted from PlayerViewModel. Reads the selected audio track first and fills gaps by
 * probing the file with [MediaMetadataRetriever].
 */
class PlaybackAudioMetadataTracker(
    private val context: Context,
    private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
) {
    private var probeJob: Job? = null
    private var probeMediaId: String? = null

    private val _metadata = MutableStateFlow(PlaybackAudioMetadata())
    val metadata: StateFlow<PlaybackAudioMetadata> = _metadata.asStateFlow()

    fun reset() {
        cancelProbe()
        _metadata.value = PlaybackAudioMetadata()
    }

    fun prepareForMedia(mediaId: String?) {
        // Queue-only changes (shuffle/unshuffle, reordering, repeat toggles) re-announce
        // the same media id while the same track keeps playing. No tracks-changed event
        // follows, so wiping here would blank the player file info until the next real
        // transition. Keep the already-resolved format info instead.
        if (mediaId != null && _metadata.value.mediaId == mediaId) return
        cancelProbe()
        _metadata.value = PlaybackAudioMetadata(mediaId = mediaId)
    }

    fun refresh(player: Player, tracks: Tracks = player.currentTracks) {
        runCatching {
            val mediaId = player.currentMediaItem?.mediaId
            if (mediaId == null) {
                reset()
                return@runCatching
            }

            val selectedAudioFormat = tracks.groups
                .asSequence()
                .filter { it.type == C.TRACK_TYPE_AUDIO }
                .flatMap { group ->
                    (0 until group.length)
                        .asSequence()
                        .filter { index -> group.isTrackSelected(index) }
                        .map { index -> group.getTrackFormat(index) }
                }
                .firstOrNull()

            val current = _metadata.value.takeIf { it.mediaId == mediaId }
            val metadata = PlaybackAudioMetadata(
                mediaId = mediaId,
                mimeType = selectedAudioFormat?.sampleMimeType
                    ?: selectedAudioFormat?.containerMimeType
                    ?: current?.mimeType,
                bitrate = selectedAudioFormat?.bitrate?.takeIf { it > 0 }
                    ?: current?.bitrate,
                sampleRate = selectedAudioFormat?.sampleRate?.takeIf { it > 0 }
                    ?: current?.sampleRate,
                channelCount = selectedAudioFormat?.channelCount?.takeIf { it > 0 } ?: current?.channelCount,
                bitDepth = selectedAudioFormat?.pcmEncoding?.let(::bitDepthOf) ?: current?.bitDepth
            )

            _metadata.value = metadata
            probeMissing(player, metadata)
        }.onFailure { throwable ->
            Logger.w(throwable) { "Failed to refresh playback audio metadata" }
        }
    }

    private fun cancelProbe() {
        probeJob?.cancel()
        probeJob = null
        probeMediaId = null
    }

    private fun bitDepthOf(pcmEncoding: Int): Int? = when (pcmEncoding) {
        C.ENCODING_PCM_8BIT -> 8
        C.ENCODING_PCM_16BIT -> 16
        C.ENCODING_PCM_24BIT -> 24
        C.ENCODING_PCM_32BIT -> 32
        C.ENCODING_PCM_FLOAT -> 32
        else -> null
    }

    private fun probeMissing(player: Player, metadata: PlaybackAudioMetadata) {
        val shouldProbe = metadata.mimeType.isNullOrBlank() || metadata.bitrate == null || metadata.sampleRate == null
        if (!shouldProbe) return

        val mediaItem = player.currentMediaItem ?: return
        val mediaId = mediaItem.mediaId
        val uri = mediaItem.localConfiguration?.uri ?: return

        if (probeMediaId == mediaId && probeJob?.isActive == true) return

        probeJob?.cancel()
        probeMediaId = mediaId
        probeJob = scope.launch(dispatchers.io) {
            val probed = runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    val mimeType = retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                        ?.takeIf { it.isNotBlank() }
                        ?: context.contentResolver.getType(uri)
                    val bitrate = retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                        ?.toIntOrNull()
                        ?.takeIf { it > 0 }
                    val sampleRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                            ?.toIntOrNull()
                            ?.takeIf { it > 0 }
                    } else null
                    PlaybackAudioMetadata(
                        mediaId = mediaId,
                        mimeType = mimeType,
                        bitrate = bitrate,
                        sampleRate = sampleRate
                    )
                } finally {
                    retriever.release()
                }
            }.getOrNull() ?: return@launch

            _metadata.update { current ->
                if (current.mediaId != mediaId) return@update current
                current.copy(
                    mimeType = current.mimeType ?: probed.mimeType,
                    bitrate = current.bitrate ?: probed.bitrate,
                    sampleRate = current.sampleRate ?: probed.sampleRate
                )
            }
        }
    }
}
