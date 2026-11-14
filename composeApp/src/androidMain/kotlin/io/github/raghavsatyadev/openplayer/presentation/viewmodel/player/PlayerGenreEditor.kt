package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import co.touchlab.kermit.Logger
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.repository.MusicRepository
import io.github.raghavsatyadev.openplayer.di.DispatcherProvider
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.LibraryStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.MetadataEditStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlaybackStateHolder
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.utils.MediaItemBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Batch genre edit (with MediaStore write permission), extracted from PlayerViewModel. */
@OptIn(UnstableApi::class)
class PlayerGenreEditor(
    private val scope: CoroutineScope,
    private val context: android.content.Context,
    private val dispatchers: DispatcherProvider,
    private val musicRepository: MusicRepository,
    private val metadataEditStateHolder: MetadataEditStateHolder,
    private val libraryStateHolder: LibraryStateHolder,
    private val playbackStateHolder: PlaybackStateHolder,
    private val writeRequests: PlayerWriteRequests,
    private val toast: suspend (String) -> Unit,
) {

    fun batchEditGenre(songs: List<Song>, newGenre: String) {
        if (songs.isEmpty()) return

        scope.launch {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val uris = songs.mapNotNull { song ->
                    song.id.toLongOrNull()?.takeIf { it > 0 }?.let { id ->
                        io.github.raghavsatyadev.openplayer.utils.MediaStorePermissionHelper.getMediaStoreUri(context, id)
                    }
                }
                if (uris.isNotEmpty()) {
                    val intentSender = io.github.raghavsatyadev.openplayer.utils.MediaStorePermissionHelper
                        .createWriteRequestIntentSender(context, uris)
                    if (intentSender != null) {
                        writeRequests.pendingBatchGenreEdit = songs to newGenre
                        writeRequests.emit(intentSender)
                        return@launch
                    }
                }
            }

            performBatchEditGenre(songs, newGenre)
        }
    }

    internal suspend fun performBatchEditGenre(songs: List<Song>, newGenre: String) {
            Logger.withTag("PlayerViewModel").d { "Starting batch genre update for ${songs.size} songs to '$newGenre'" }
            toast(getString(Res.string.player_updating_n_songs, songs.size))

            var successCount = 0
            var failCount = 0

            songs.forEach { song ->
                val sourceSong = if (song.lyrics != null) {
                    song
                } else {
                    withContext(dispatchers.io) {
                        musicRepository.getSong(song.id).first()
                    } ?: song
                }

                val result = metadataEditStateHolder.saveMetadata(
                    song = sourceSong,
                    newTitle = sourceSong.title,
                    newArtist = sourceSong.artist,
                    newAlbum = sourceSong.album,
                    newAlbumArtist = sourceSong.albumArtist ?: "",
                    newComposer = "",
                    newGenre = newGenre,
                    newLyrics = sourceSong.lyrics ?: "",
                    newTrackNumber = sourceSong.trackNumber,
                    newDiscNumber =  sourceSong.discNumber,
                    coverArtUpdate = null
                )

                if (result.success && result.updatedSong != null) {
                    successCount++
                    val updatedSong = result.updatedSong

                    libraryStateHolder.updateSong(updatedSong)

                    if (playbackStateHolder.stablePlayerState.value.currentSong?.id == song.id) {
                        playbackStateHolder.updateStablePlayerState { it.copy(currentSong = updatedSong) }
                        val controller = playbackStateHolder.mediaController
                        if (controller != null) {
                            val idx = controller.currentMediaItemIndex
                            if (idx != C.INDEX_UNSET) {
                                controller.replaceMediaItem(idx, MediaItemBuilder.build(updatedSong))
                            }
                        }
                    }
                } else {
                    failCount++
                }
            }

            if (failCount == 0) {
                toast(getString(Res.string.player_batch_genre_updated_all, successCount))
            } else {
                toast(
                    getString(Res.string.player_batch_genre_updated_partial, successCount, failCount),
                )
            }
    }
}
