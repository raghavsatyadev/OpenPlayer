package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import co.touchlab.kermit.Logger
import io.github.raghavsatyadev.openplayer.data.media.CoverArtUpdate
import io.github.raghavsatyadev.openplayer.data.media.CustomMetadataChanges
import io.github.raghavsatyadev.openplayer.data.database.AlbumArtThemeDao
import io.github.raghavsatyadev.openplayer.data.media.ImageCacheManager
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.di.DispatcherProvider
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.LibraryStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.MetadataEditStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.MultiSelectionStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlaybackStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerUiState
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ThemeStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.replaceSong
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.utils.MediaItemBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Single and batch metadata edits (with MediaStore write permission) and album-art cache upkeep, extracted from PlayerViewModel. */
@OptIn(UnstableApi::class)
class PlayerMetadataEditor(
    private val scope: CoroutineScope,
    private val context: android.content.Context,
    private val dispatchers: DispatcherProvider,
    private val metadataEditStateHolder: MetadataEditStateHolder,
    private val libraryStateHolder: LibraryStateHolder,
    private val playbackStateHolder: PlaybackStateHolder,
    private val multiSelectionStateHolder: MultiSelectionStateHolder,
    private val themeStateHolder: ThemeStateHolder,
    private val playerUiState: MutableStateFlow<PlayerUiState>,
    private val selectedSongForInfo: MutableStateFlow<Song?>,
    private val imageCacheManager: ImageCacheManager,
    private val albumArtThemeDao: AlbumArtThemeDao,
    private val writeRequests: PlayerWriteRequests,
    private val toast: suspend (String) -> Unit,
) {

    fun saveBatchMetadata(
        songs: List<Song>,
        title: String?,
        artist: String?,
        album: String?,
        albumArtist: String?,
        composer: String?,
        genre: String?,
        lyrics: String?,
        trackNumber: Int?,
        discNumber: Int?,
        replayGainTrackGainDb: String?,
        replayGainAlbumGainDb: String?,
        coverArtUpdate: CoverArtUpdate?
    ) {
        scope.launch {
            val localSongsNeedingPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                songs.mapNotNull { song ->
                    song.id.toLongOrNull()?.takeIf { it > 0 }?.let { song to it }
                }
            } else {
                emptyList()
            }

            if (localSongsNeedingPermission.isNotEmpty()) {
                val uris = localSongsNeedingPermission.mapNotNull { (_, songId) ->
                    android.provider.MediaStore.Audio.Media.getContentUri(
                        android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY,
                        songId
                    )
                }

                if (uris.isNotEmpty()) {
                    val intentSender = io.github.raghavsatyadev.openplayer.utils.MediaStorePermissionHelper
                        .createWriteRequestIntentSender(context, uris)

                    if (intentSender != null) {
                        writeRequests.pendingBatchMetadataEdit = PendingBatchMetadataEdit(
                            songs = songs,
                            title = title,
                            artist = artist,
                            album = album,
                            albumArtist = albumArtist,
                            composer = composer,
                            genre = genre,
                            lyrics = lyrics,
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            replayGainTrackGainDb = replayGainTrackGainDb,
                            replayGainAlbumGainDb = replayGainAlbumGainDb,
                            coverArtUpdate = coverArtUpdate
                        )
                        writeRequests.emit(intentSender)
                        return@launch
                    }
                }
            }

            performBatchMetadataEdit(
                songs, title, artist, album, albumArtist, composer, genre, lyrics,
                trackNumber, discNumber, replayGainTrackGainDb, replayGainAlbumGainDb, coverArtUpdate
            )
        }
    }

    internal suspend fun performBatchMetadataEdit(
        songs: List<Song>,
        title: String?,
        artist: String?,
        album: String?,
        albumArtist: String?,
        composer: String?,
        genre: String?,
        lyrics: String?,
        trackNumber: Int?,
        discNumber: Int?,
        replayGainTrackGainDb: String?,
        replayGainAlbumGainDb: String?,
        coverArtUpdate: CoverArtUpdate?
    ) {
        var successCount = 0
        var failureCount = 0
        val previousAlbumArts = mutableSetOf<String?>()

        songs.forEach { song ->
            previousAlbumArts.add(song.albumArtUriString)

            val result = metadataEditStateHolder.saveMetadata(
                song = song,
                newTitle = title ?: song.title,
                newArtist = artist ?: song.displayArtist,
                newAlbum = album ?: song.album,
                newAlbumArtist = albumArtist ?: (song.albumArtist ?: ""),
                newComposer = composer ?: "",
                newGenre = genre ?: (song.genre ?: ""),
                newLyrics = lyrics ?: (song.lyrics ?: ""),
                newTrackNumber = trackNumber ?: song.trackNumber,
                newDiscNumber = discNumber ?: song.discNumber,
                newReplayGainTrackGainDb = replayGainTrackGainDb,
                newReplayGainAlbumGainDb = replayGainAlbumGainDb,
                coverArtUpdate = coverArtUpdate
            )

            if (result.success && result.updatedSong != null) {
                successCount++
                val updatedSong = result.updatedSong
                val refreshedAlbumArtUri = result.updatedAlbumArtUri

                invalidateCoverArtCaches(song.albumArtUriString, refreshedAlbumArtUri)

                playerUiState.update { state ->
                    val updatedQueue = state.currentPlaybackQueue.replaceSong(updatedSong)
                    if (updatedQueue === state.currentPlaybackQueue) {
                        state
                    } else {
                        state.copy(currentPlaybackQueue = updatedQueue)
                    }
                }

                libraryStateHolder.updateSong(updatedSong)

                if (playbackStateHolder.stablePlayerState.value.currentSong?.id == song.id) {
                    playbackStateHolder.updateStablePlayerState {
                        it.copy(
                            currentSong = updatedSong,
                            lyrics = result.parsedLyrics
                        )
                    }

                    val controller = playbackStateHolder.mediaController
                    if (controller != null) {
                        val currentIndex = controller.currentMediaItemIndex
                        if (currentIndex >= 0 && currentIndex < controller.mediaItemCount) {
                            val currentPosition = controller.currentPosition
                            val newMediaItem = MediaItemBuilder.build(updatedSong)
                            controller.replaceMediaItem(currentIndex, newMediaItem)
                            controller.seekTo(currentIndex, currentPosition)
                        }
                    }
                }

                if (selectedSongForInfo.value?.id == song.id) {
                    selectedSongForInfo.value = updatedSong
                }
            } else {
                failureCount++
            }
        }

        if (coverArtUpdate != null) {
            previousAlbumArts.forEach { previousArt ->
                purgeAlbumArtThemes(previousArt, null)
            }

            val currentSongId = playbackStateHolder.stablePlayerState.value.currentSong?.id
            if (currentSongId != null && songs.any { it.id == currentSongId }) {
                val currentSong = playbackStateHolder.stablePlayerState.value.currentSong
                val paletteTargetUri = currentSong?.albumArtUriString
                if (paletteTargetUri != null) {
                    themeStateHolder.getAlbumColorSchemeFlow(paletteTargetUri)
                    themeStateHolder.extractAndGenerateColorScheme(
                        paletteTargetUri.toUri(),
                        paletteTargetUri,
                        isPreload = false
                    )
                } else {
                    themeStateHolder.extractAndGenerateColorScheme(null, null, isPreload = false)
                }
            }
        }

        multiSelectionStateHolder.clearSelection()

        val message = when {
            failureCount == 0 -> getString(Res.string.batch_edit_success, successCount)
            successCount == 0 -> getString(Res.string.batch_edit_failed)
            else -> getString(Res.string.batch_edit_partial_success, successCount, songs.size)
        }
        toast(message)
    }

    fun editSongMetadata(
        song: Song,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newAlbumArtist: String,
        newComposer: String,
        newGenre: String,
        newLyrics: String,
        newTrackNumber: Int,
        newDiscNumber: Int?,
        newReplayGainTrackGainDb: String? = null,
        newReplayGainAlbumGainDb: String? = null,
        coverArtUpdate: CoverArtUpdate?,
        customMetadataChanges: CustomMetadataChanges = CustomMetadataChanges(),
    ) {
        scope.launch {
            Logger.withTag("PlayerViewModel").e { "METADATA_EDIT_VM: Starting editSongMetadata via Holder" }

            val songId = song.id.toLongOrNull()
            if (songId != null && songId > 0 && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val intentSender = io.github.raghavsatyadev.openplayer.utils.MediaStorePermissionHelper
                    .createWriteRequestForSong(context, songId)
                if (intentSender != null) {
                    writeRequests.pendingMetadataEdit = PendingMetadataEdit(
                        song = song,
                        title = newTitle,
                        artist = newArtist,
                        album = newAlbum,
                        albumArtist = newAlbumArtist,
                        composer = newComposer,
                        genre = newGenre,
                        lyrics = newLyrics,
                        trackNumber = newTrackNumber,
                        discNumber = newDiscNumber,
                        replayGainTrackGainDb = newReplayGainTrackGainDb,
                        replayGainAlbumGainDb = newReplayGainAlbumGainDb,
                        coverArtUpdate = coverArtUpdate,
                        customMetadataChanges = customMetadataChanges
                    )
                    writeRequests.emit(intentSender)
                    return@launch
                }
            }

            performMetadataEdit(song, newTitle, newArtist, newAlbum, newAlbumArtist, newComposer, newGenre, newLyrics,
                newTrackNumber, newDiscNumber, newReplayGainTrackGainDb, newReplayGainAlbumGainDb, coverArtUpdate,
                customMetadataChanges)
        }
    }

    internal suspend fun performMetadataEdit(
        song: Song,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newAlbumArtist: String,
        newComposer: String,
        newGenre: String,
        newLyrics: String,
        newTrackNumber: Int,
        newDiscNumber: Int?,
        newReplayGainTrackGainDb: String?,
        newReplayGainAlbumGainDb: String?,
        coverArtUpdate: CoverArtUpdate?,
        customMetadataChanges: CustomMetadataChanges = CustomMetadataChanges(),
    ) {
        val previousAlbumArt = song.albumArtUriString

        val result = metadataEditStateHolder.saveMetadata(
            song = song,
            newTitle = newTitle,
            newArtist = newArtist,
            newAlbum = newAlbum,
            newAlbumArtist = newAlbumArtist,
            newComposer = newComposer,
            newGenre = newGenre,
            newLyrics = newLyrics,
            newTrackNumber = newTrackNumber,
            newDiscNumber = newDiscNumber,
            newReplayGainTrackGainDb = newReplayGainTrackGainDb,
            newReplayGainAlbumGainDb = newReplayGainAlbumGainDb,
            coverArtUpdate = coverArtUpdate,
            customMetadataChanges = customMetadataChanges
        )

        Logger.withTag("PlayerViewModel").e { "METADATA_EDIT_VM: Result success=${result.success}" }

        if (result.success && result.updatedSong != null) {
            val updatedSong = result.updatedSong
            val refreshedAlbumArtUri = result.updatedAlbumArtUri

            invalidateCoverArtCaches(previousAlbumArt, refreshedAlbumArtUri)

            playerUiState.update { state ->
                val updatedQueue = state.currentPlaybackQueue.replaceSong(updatedSong)
                if (updatedQueue === state.currentPlaybackQueue) {
                    state
                } else {
                    state.copy(currentPlaybackQueue = updatedQueue)
                }
            }

            libraryStateHolder.updateSong(updatedSong)

            if (playbackStateHolder.stablePlayerState.value.currentSong?.id == song.id) {
                playbackStateHolder.updateStablePlayerState {
                    it.copy(
                        currentSong = updatedSong,
                        lyrics = result.parsedLyrics
                    )
                }

                val controller = playbackStateHolder.mediaController
                if (controller != null) {
                    val currentIndex = controller.currentMediaItemIndex
                    if (currentIndex >= 0 && currentIndex < controller.mediaItemCount) {
                        val currentPosition = controller.currentPosition
                        val newMediaItem = MediaItemBuilder.build(updatedSong)
                        controller.replaceMediaItem(currentIndex, newMediaItem)
                        controller.seekTo(currentIndex, currentPosition)
                    }
                }
            }

            if (selectedSongForInfo.value?.id == song.id) {
                selectedSongForInfo.value = updatedSong
            }

            if (coverArtUpdate != null) {
                purgeAlbumArtThemes(previousAlbumArt, updatedSong.albumArtUriString)
                val paletteTargetUri = updatedSong.albumArtUriString
                if (paletteTargetUri != null) {
                    themeStateHolder.getAlbumColorSchemeFlow(paletteTargetUri)
                    val currentUri = playbackStateHolder.stablePlayerState.value.currentSong?.albumArtUriString
                    themeStateHolder.extractAndGenerateColorScheme(paletteTargetUri.toUri(), currentUri, isPreload = false)
                } else {
                    val currentUri = playbackStateHolder.stablePlayerState.value.currentSong?.albumArtUriString
                    themeStateHolder.extractAndGenerateColorScheme(null, currentUri, isPreload = false)
                }
            }

            toast(getString(Res.string.metadata_updated_successfully))
        } else {
            val errorMessage = result.getUserFriendlyErrorMessage()
            Logger.withTag("PlayerViewModel").e { "METADATA_EDIT_VM: Failed - ${result.error}: $errorMessage" }
            toast(errorMessage)
        }
    }

    private fun invalidateCoverArtCaches(vararg uriStrings: String?) {
        imageCacheManager.invalidateCoverArtCaches(*uriStrings)
    }

    private suspend fun purgeAlbumArtThemes(vararg uriStrings: String?) {
        val uris = uriStrings.mapNotNull { it?.takeIf(String::isNotBlank) }.distinct()
        if (uris.isEmpty()) return

        withContext(dispatchers.io) {
            albumArtThemeDao.deleteThemesByUris(uris)
        }
    }

    suspend fun forceRegenerateAlbumPaletteForSong(song: Song): Boolean {
        val albumArtUri = song.albumArtUriString?.takeIf { it.isNotBlank() } ?: return false
        return runCatching {
            themeStateHolder.forceRegenerateColorScheme(
                uriString = albumArtUri,
                regenerateAllStyles = true
            )
            true
        }.getOrDefault(false)
    }


}
