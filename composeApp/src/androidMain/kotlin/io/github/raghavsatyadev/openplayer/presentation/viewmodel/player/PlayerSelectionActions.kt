package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import android.content.Context
import co.touchlab.kermit.Logger
import io.github.raghavsatyadev.openplayer.data.model.Album
import io.github.raghavsatyadev.openplayer.data.model.Artist
import io.github.raghavsatyadev.openplayer.data.model.Genre
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.model.StorageFilter
import io.github.raghavsatyadev.openplayer.data.repository.MusicRepository
import io.github.raghavsatyadev.openplayer.di.DispatcherProvider
import io.github.raghavsatyadev.openplayer.presentation.selection.flattenDistinctGroups
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.MultiSelectionStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerUiState
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.utils.ZipShareHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private const val MAX_ALBUM_BATCH_SELECTION = 6

private data class ResolvedAlbumSelection(
    val albums: List<Album>,
    val songs: List<Song>,
    val wasTrimmed: Boolean
)

/** Actions that stay in PlayerViewModel (queue and playback live there) and are invoked by [PlayerSelectionActions]. */
class PlayerSelectionCallbacks(
    val playSongs: (songsToPlay: List<Song>, startSong: Song, queueName: String, playlistId: String?) -> Unit,
    val addSongToQueue: (Song) -> Unit,
    val addSongsAsNext: (List<Song>) -> Unit,
    val setFavoriteStatus: suspend (songId: String, isFavorite: Boolean) -> Unit,
    val effectiveStorageFilter: (PlayerUiState) -> StorageFilter,
    val showPlayer: () -> Unit,
)

/** Multi-selection batch actions (play, queue, favorite, share, resolve) extracted from PlayerViewModel. */
class PlayerSelectionActions(
    private val scope: CoroutineScope,
    private val context: Context,
    private val musicRepository: MusicRepository,
    private val dispatchers: DispatcherProvider,
    private val multiSelectionStateHolder: MultiSelectionStateHolder,
    private val favoriteSongIds: StateFlow<Set<String>>,
    private val playerUiState: StateFlow<PlayerUiState>,
    private val toast: suspend (String) -> Unit,
    private val callbacks: PlayerSelectionCallbacks,
) {

    /**
     * Plays all selected songs, preserving their selection order.
     * Clears selection after starting playback.
     */
    fun playSelectedSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        scope.launch {
            callbacks.playSongs(songs, songs.first(), getString(Res.string.queue_name_selected_songs), null)
        }
        multiSelectionStateHolder.clearSelection()
    }

    /**
     * Adds all selected songs to the end of the queue.
     * Clears selection after adding.
     */
    fun addSelectedToQueue(songs: List<Song>) {
        songs.forEach { callbacks.addSongToQueue(it) }
        scope.launch {
            val n = songs.size
            toast(
                getPluralString(Res.plurals.n_songs_added_to_queue, n, n),
            )
        }
        multiSelectionStateHolder.clearSelection()
    }

    /**
     * Adds all selected songs to play next, preserving selection order.
     * Songs are inserted in reverse order so they play in the correct sequence.
     * Clears selection after adding.
     */
    fun addSelectedAsNext(songs: List<Song>) {
        callbacks.addSongsAsNext(songs)
        scope.launch {
            val n = songs.size
            toast(
                getPluralString(Res.plurals.n_songs_will_play_next, n, n),
            )
        }
        multiSelectionStateHolder.clearSelection()
    }

    fun playSelectedAlbums(albums: List<Album>) {
        if (albums.isEmpty()) return
        scope.launch {
            try {
                val resolvedSelection = resolveSelectedAlbumSongs(albums)
                if (resolvedSelection.songs.isEmpty()) {
                    toast(getString(Res.string.player_no_playable_songs_in_albums))
                    return@launch
                }

                val queueName = if (resolvedSelection.albums.size == 1) {
                    resolvedSelection.albums.first().title
                } else {
                    getString(Res.string.player_queue_name_selected_albums)
                }

                callbacks.playSongs(resolvedSelection.songs, resolvedSelection.songs.first(), queueName, null)
                callbacks.showPlayer()

                if (resolvedSelection.wasTrimmed) {
                    toast(
                        getString(Res.string.player_only_first_n_albums_queued, MAX_ALBUM_BATCH_SELECTION),
                    )
                } else {
                    toast(
                        getString(Res.string.player_albums_queued_format,
                            resolvedSelection.albums.size,
                            resolvedSelection.songs.size,
                        ),
                    )
                }
            } catch (e: Exception) {
                Logger.withTag("PlayerViewModel").e(e) { "Error playing selected albums" }
                toast(getString(Res.string.player_could_not_queue_albums))
            }
        }
    }

    fun addSelectedAlbumsAsNext(albums: List<Album>) {
        if (albums.isEmpty()) return

        scope.launch {
            try {
                val resolvedSelection = resolveSelectedAlbumSongs(albums)
                if (resolvedSelection.songs.isEmpty()) {
                    toast(getString(Res.string.player_no_playable_songs_in_albums))
                    return@launch
                }

                callbacks.addSongsAsNext(resolvedSelection.songs)

                if (resolvedSelection.wasTrimmed) {
                    toast(getString(Res.string.player_only_first_n_albums_next, MAX_ALBUM_BATCH_SELECTION))
                } else {
                    toast(getString(Res.string.player_albums_will_play_next_format, resolvedSelection.albums.size))
                }
            } catch (e: Exception) {
                Logger.withTag("PlayerViewModel").e(e) { "Error adding selected albums as next" }
                toast(getString(Res.string.player_could_not_add_albums_next))
            }
        }
    }

    fun addSelectedAlbumsToQueue(albums: List<Album>) {
        if (albums.isEmpty()) return

        scope.launch {
            try {
                val resolvedSelection = resolveSelectedAlbumSongs(albums)
                if (resolvedSelection.songs.isEmpty()) {
                    toast(getString(Res.string.player_no_playable_songs_in_albums))
                    return@launch
                }

                resolvedSelection.songs.forEach(callbacks.addSongToQueue)

                if (resolvedSelection.wasTrimmed) {
                    toast(getString(Res.string.player_only_first_n_albums_added_queue, MAX_ALBUM_BATCH_SELECTION))
                } else {
                    toast(getString(Res.string.player_albums_added_to_queue_format, resolvedSelection.albums.size))
                }
            } catch (e: Exception) {
                Logger.withTag("PlayerViewModel").e(e) { "Error adding selected albums to queue" }
                toast(getString(Res.string.player_could_not_add_albums_queue))
            }
        }
    }

    suspend fun getSongsForAlbums(albums: List<Album>): List<Song> {
        return resolveSelectedAlbumSongs(albums).songs
    }

    /** Resolves the selected genres to their songs, in selection order and without duplicates. */
    suspend fun getSongsForGenres(genres: List<Genre>): List<Song> {
        return withContext(dispatchers.io) {
            genres.flatMap { genre ->
                musicRepository.getMusicByGenre(genre.name).first()
            }.distinctBy { it.id }
        }
    }

    fun playSelectedGenres(genres: List<Genre>) {
        if (genres.isEmpty()) return
        scope.launch {
            try {
                val songs = getSongsForGenres(genres)
                if (songs.isNotEmpty()) {
                    callbacks.playSongs(songs, songs.first(), getString(Res.string.player_queue_name_selected_genres), null)
                    callbacks.showPlayer()
                } else {
                    toast(getString(Res.string.player_no_playable_songs_in_genres))
                }
            } catch (e: Exception) {
                Logger.withTag("PlayerViewModel").e(e) { "Error playing selected genres" }
                toast(getString(Res.string.player_could_not_queue_genres))
            }
        }
    }

    fun addSelectedGenresToQueue(genres: List<Genre>) {
        if (genres.isEmpty()) return
        scope.launch {
            try {
                val songs = getSongsForGenres(genres)
                if (songs.isNotEmpty()) {
                    songs.forEach { callbacks.addSongToQueue(it) }
                    val n = songs.size
                    toast(
                        getPluralString(Res.plurals.n_songs_added_to_queue, n, n),
                    )
                } else {
                    toast(getString(Res.string.player_no_playable_songs_in_genres))
                }
            } catch (e: Exception) {
                Logger.withTag("PlayerViewModel").e(e) { "Error adding selected genres to queue" }
                toast(getString(Res.string.player_could_not_queue_genres))
            }
        }
    }

    fun addSelectedGenresAsNext(genres: List<Genre>) {
        if (genres.isEmpty()) return
        scope.launch {
            try {
                val songs = getSongsForGenres(genres)
                if (songs.isNotEmpty()) {
                    callbacks.addSongsAsNext(songs)
                    val n = songs.size
                    toast(
                        getPluralString(Res.plurals.n_songs_will_play_next, n, n),
                    )
                } else {
                    toast(getString(Res.string.player_no_playable_songs_in_genres))
                }
            } catch (e: Exception) {
                Logger.withTag("PlayerViewModel").e(e) { "Error adding selected genres as next" }
                toast(getString(Res.string.player_could_not_queue_genres))
            }
        }
    }

    /**
     * Adds all selected songs to favorites.
     * Clears selection after liking.
     */
    fun likeSelectedSongs(songs: List<Song>) {
        scope.launch {
            val favIds = favoriteSongIds.value.toMutableSet()
            var likedCount = 0
            songs.forEach { song ->
                if (!favIds.contains(song.id)) {
                    callbacks.setFavoriteStatus(song.id, true)
                    favIds.add(song.id)
                    likedCount++
                }
            }
            if (likedCount > 0) {
                toast(
                    getPluralString(Res.plurals.n_songs_added_to_favorites, likedCount, likedCount),
                )
            } else {
                toast(getString(Res.string.player_all_songs_already_in_favorites))
            }
            multiSelectionStateHolder.clearSelection()
        }
    }

    /**
     * Removes all selected songs from favorites.
     * Clears selection after unliking.
     */
    fun unlikeSelectedSongs(songs: List<Song>) {
        scope.launch {
            val favIds = favoriteSongIds.value.toMutableSet()
            var unlikedCount = 0
            songs.forEach { song ->
                if (favIds.contains(song.id)) {
                    callbacks.setFavoriteStatus(song.id, false)
                    favIds.remove(song.id)
                    unlikedCount++
                }
            }
            if (unlikedCount > 0) {
                toast(
                    getPluralString(Res.plurals.n_songs_removed_from_favorites,
                        unlikedCount,
                        unlikedCount,
                    ),
                )
            } else {
                toast(getString(Res.string.player_no_songs_were_in_favorites))
            }
            multiSelectionStateHolder.clearSelection()
        }
    }

    /**
     * Shares all selected songs as a ZIP file.
     * Clears selection after initiating share.
     */
    fun shareSelectedAsZip(songs: List<Song>) {
        scope.launch {
            toast(getString(Res.string.player_creating_zip))

            val result = ZipShareHelper.createAndShareZip(context, songs)

            result.onSuccess {
                multiSelectionStateHolder.clearSelection()
            }.onFailure { error ->
                toast(
                    getString(Res.string.player_share_zip_failed_format, error.localizedMessage ?: ""),
                )
                Logger.withTag("PlayerViewModel").e(error) { "Failed to share selection as zip" }
            }
        }
    }

    private suspend fun resolveSelectedAlbumSongs(albums: List<Album>): ResolvedAlbumSelection {
        val albumsToProcess = albums.take(MAX_ALBUM_BATCH_SELECTION)
        val wasTrimmed = albums.size > albumsToProcess.size

        val songs = withContext(dispatchers.io) {
            buildList {
                albumsToProcess.forEach { album ->
                    val albumSongs = musicRepository.getSongsForAlbum(album.id).first()
                    if (albumSongs.isNotEmpty()) {
                        addAll(sortSongsForAlbumSelection(albumSongs))
                    }
                }
            }
        }

        return ResolvedAlbumSelection(
            albums = albumsToProcess,
            songs = songs,
            wasTrimmed = wasTrimmed
        )
    }

    /** Resolves whole albums to the currently visible songs consumed by the batch-action sheet. */
    suspend fun resolveAlbumSongsForBatchActions(albums: List<Album>): List<Song> {
        if (albums.isEmpty()) return emptyList()
        val visibleSongIds = currentLibrarySelectionSongIds()
        return withContext(dispatchers.io) {
            flattenDistinctGroups(
                groups = albums.map { album ->
                    sortSongsForAlbumSelection(musicRepository.getSongsForAlbum(album.id).first())
                        .filter { it.id in visibleSongIds }
                },
                keyOf = Song::id
            )
        }
    }

    /**
     * Resolves whole artists to songs and removes overlaps from multi-artist tracks so delete,
     * edit, queue and playlist actions are each applied once per file.
     */
    suspend fun resolveArtistSongsForBatchActions(artists: List<Artist>): List<Song> {
        if (artists.isEmpty()) return emptyList()
        val visibleSongIds = currentLibrarySelectionSongIds()
        return withContext(dispatchers.io) {
            flattenDistinctGroups(
                groups = artists.map { artist ->
                    musicRepository.getSongsForArtist(artist.id).first()
                        .filter { it.id in visibleSongIds }
                },
                keyOf = Song::id
            )
        }
    }

    private suspend fun currentLibrarySelectionSongIds(): Set<String> {
        val state = playerUiState.value
        return musicRepository.getSongIdsSorted(
            sortOption = state.currentSongSortOption,
            storageFilter = callbacks.effectiveStorageFilter(state),
        ).mapTo(hashSetOf()) { it.toString() }
    }

    private fun sortSongsForAlbumSelection(songs: List<Song>): List<Song> {
        return songs.sortedWith(
            compareBy<Song> { it.discNumber ?: 1 }
                .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                .thenBy { it.title.lowercase(Locale.getDefault()) }
        )
    }
}
