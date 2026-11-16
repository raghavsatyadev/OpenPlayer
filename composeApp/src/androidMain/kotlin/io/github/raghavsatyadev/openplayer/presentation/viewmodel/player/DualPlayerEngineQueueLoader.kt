package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.service.player.DualPlayerEngine
import io.github.raghavsatyadev.openplayer.di.DispatcherProvider
import io.github.raghavsatyadev.openplayer.utils.MediaItemBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

private data class PreparedPlaybackQueueSegments(
    val beforeCurrent: List<MediaItem>,
    val afterCurrent: List<MediaItem>,
    val currentIndex: Int
)

/** [PlaybackQueueLoader] that works directly on [DualPlayerEngine.masterPlayer] (the controller mirror lags behind it). */
@androidx.annotation.OptIn(UnstableApi::class)
class DualPlayerEngineQueueLoader(
    private val dualPlayerEngine: DualPlayerEngine,
    private val dispatchers: DispatcherProvider,
) : PlaybackQueueLoader {

    override suspend fun prepareStart(song: Song, playlistId: String?, startPositionMs: Long): () -> Unit {
        val startMediaItem = dualPlayerEngine.resolveMediaItem(buildItem(song, playlistId))
        return {
            dualPlayerEngine.cancelNext()
            val enginePlayer = dualPlayerEngine.masterPlayer
            enginePlayer.setMediaItem(startMediaItem, startPositionMs)
            enginePlayer.prepare()
            enginePlayer.play()
        }
    }

    override suspend fun attachRest(songs: List<Song>, startSongId: String, playlistId: String?): Int? {
        val segments = prepareSegments(songs, startSongId, playlistId)
        return withContext(Dispatchers.Main.immediate) {
            attachIfCurrent(startSongId, segments)
        }
    }

    private suspend fun prepareSegments(
        songsToPlay: List<Song>,
        startSongId: String,
        playlistId: String?
    ): PreparedPlaybackQueueSegments = withContext(dispatchers.default) {
        val currentIndex = songsToPlay
            .indexOfFirst { it.id == startSongId }
            .takeIf { it >= 0 }
            ?: 0

        val beforeCurrent = List(currentIndex) { index ->
            buildItem(songsToPlay[index], playlistId)
        }
        val afterStartIndex = currentIndex + 1
        val afterCurrent = List((songsToPlay.size - afterStartIndex).coerceAtLeast(0)) { offset ->
            buildItem(songsToPlay[afterStartIndex + offset], playlistId)
        }

        PreparedPlaybackQueueSegments(
            beforeCurrent = beforeCurrent,
            afterCurrent = afterCurrent,
            currentIndex = currentIndex
        )
    }

    private suspend fun attachIfCurrent(
        startSongId: String,
        preparedSegments: PreparedPlaybackQueueSegments
    ): Int? {
        val player = dualPlayerEngine.masterPlayer
        if (player.currentMediaItem?.mediaId != startSongId) return null
        if (player.mediaItemCount != 1) return null
        if (player.getMediaItemAt(0).mediaId != startSongId) return null

        val batchSize = 200

        if (preparedSegments.beforeCurrent.isNotEmpty()) {
            var insertedCount = 0
            while (insertedCount < preparedSegments.beforeCurrent.size) {
                val end = (insertedCount + batchSize).coerceAtMost(preparedSegments.beforeCurrent.size)
                val batch = preparedSegments.beforeCurrent.subList(insertedCount, end)
                player.addMediaItems(insertedCount, batch)
                insertedCount = end
                yield()
            }
        }

        if (preparedSegments.afterCurrent.isNotEmpty()) {
            var insertedCount = 0
            while (insertedCount < preparedSegments.afterCurrent.size) {
                val end = (insertedCount + batchSize).coerceAtMost(preparedSegments.afterCurrent.size)
                val batch = preparedSegments.afterCurrent.subList(insertedCount, end)
                player.addMediaItems(preparedSegments.beforeCurrent.size + 1 + insertedCount, batch)
                insertedCount = end
                yield()
            }
        }

        return preparedSegments.currentIndex
    }

    private fun buildItem(song: Song, playlistId: String?): MediaItem =
        MediaItemBuilder.withPlaylistId(MediaItemBuilder.build(song), playlistId)
}
