package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlaylistViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource

/**
 * Song options for a Home "Recently played" pill. The pills have no more-options button, so a
 * long press opens this sheet instead. Playing from here keeps the recently played queue, the
 * same as tapping the pill.
 */
@Composable
fun RecentlyPlayedSongOptionsSheet(
    song: Song,
    queueSongs: ImmutableList<Song>,
    queueName: String,
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onSongDeleted: (Song) -> Unit,
    onNavigateToAlbum: (Song) -> Unit,
    onNavigateToArtist: (artistId: Long) -> Unit,
    onNavigateToGenre: (Song) -> Unit
) {
    val addedToQueueMessage = stringResource(Res.string.toast_added_to_queue)
    val playingNextMessage = stringResource(Res.string.toast_playing_next)
    val selectedSongForInfo by playerViewModel.selectedSongForInfo.collectAsStateWithLifecycle()
    val favoriteSongIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    var showPlaylistBottomSheet by remember(song.id) { mutableStateOf(false) }

    // The pill holds Home's snapshot of the song. Selecting it loads the library copy, which
    // also follows metadata edits made from this sheet; show the pill's copy until it arrives.
    LaunchedEffect(song.id) {
        playerViewModel.selectSongForInfo(song)
    }
    val sheetSong = selectedSongForInfo?.takeIf { it.id == song.id } ?: song

    SongInfoBottomSheet(
        song = sheetSong,
        isFavorite = favoriteSongIds.contains(sheetSong.id),
        onToggleFavorite = { playerViewModel.toggleFavoriteSpecificSong(sheetSong) },
        onDismiss = {
            showPlaylistBottomSheet = false
            onDismiss()
        },
        onPlaySong = {
            if (queueSongs.any { it.id == sheetSong.id }) {
                playerViewModel.playSongs(
                    songsToPlay = queueSongs,
                    startSong = sheetSong,
                    queueName = queueName
                )
            } else {
                playerViewModel.showAndPlaySong(sheetSong)
            }
            onDismiss()
        },
        onAddToQueue = {
            playerViewModel.addSongToQueue(sheetSong)
            playerViewModel.sendToast(addedToQueueMessage)
            onDismiss()
        },
        onAddNextToQueue = {
            playerViewModel.addSongNextToQueue(sheetSong)
            playerViewModel.sendToast(playingNextMessage)
            onDismiss()
        },
        onAddToPlayList = {
            showPlaylistBottomSheet = true
        },
        onDeleteFromDevice = playerViewModel::deleteFromDevice,
        onNavigateToAlbum = {
            onNavigateToAlbum(sheetSong)
            onDismiss()
        },
        onNavigateToArtist = {
            onNavigateToArtist(sheetSong.artistId)
            onDismiss()
        },
        onNavigateToArtistById = { artistId ->
            onNavigateToArtist(artistId)
            onDismiss()
        },
        onNavigateToGenre = {
            onNavigateToGenre(sheetSong)
            onDismiss()
        },
        onEditSong = { newTitle, newArtist, newAlbum, newAlbumArtist, newComposer, newGenre, newLyrics, newTrackNumber, newDiscNumber, replayGainTrackGainDb, replayGainAlbumGainDb, coverArtUpdate, customMetadataChanges ->
            playerViewModel.editSongMetadata(
                sheetSong,
                newTitle,
                newArtist,
                newAlbum,
                newAlbumArtist,
                newComposer,
                newGenre,
                newLyrics,
                newTrackNumber,
                newDiscNumber,
                replayGainTrackGainDb,
                replayGainAlbumGainDb,
                coverArtUpdate,
                customMetadataChanges
            )
        },
        removeFromListTrigger = { onSongDeleted(sheetSong) }
    )

    if (showPlaylistBottomSheet) {
        val playlistViewModel: PlaylistViewModel = koinViewModel()
        val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
        val navBarCompactMode by playerViewModel.navBarCompactMode.collectAsStateWithLifecycle()
        val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        PlaylistBottomSheet(
            playlistUiState = playlistUiState,
            songs = persistentListOf(sheetSong),
            onDismiss = { showPlaylistBottomSheet = false },
            bottomBarHeight = resolveNavBarOccupiedHeight(systemNavBarInset, navBarCompactMode),
            playerViewModel = playerViewModel,
            playlistViewModel = playlistViewModel
        )
    }
}
