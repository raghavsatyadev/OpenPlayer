package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import co.touchlab.kermit.Logger
import io.github.raghavsatyadev.openplayer.data.model.Genre
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.model.StorageFilter
import io.github.raghavsatyadev.openplayer.data.repository.MusicRepository
import io.github.raghavsatyadev.openplayer.data.worker.SyncManager
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.LibraryStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.UNKNOWN_GENRE_ID
import io.github.raghavsatyadev.openplayer.resources.Res
import io.github.raghavsatyadev.openplayer.resources.player_no_songs_in_library_toast
import io.github.raghavsatyadev.openplayer.resources.queue_name_all_songs_shuffled
import io.github.raghavsatyadev.openplayer.resources.queue_name_liked_songs_shuffled
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString

private const val SHUFFLE_ALL_SONG_LIMIT = 10_000

/** What [PlayerShuffleActions] needs back from PlayerViewModel. */
data class PlayerShuffleCallbacks(
    val playSongsShuffled: (songs: List<Song>, queueName: String) -> Unit,
    val currentStorageFilter: () -> StorageFilter,
    val isControllerReady: () -> Boolean,
    val setPendingPlaybackAction: (() -> Unit) -> Unit,
    val toast: (String) -> Unit,
)

/**
 * The shuffle sources (all songs, the Quick Settings tile, favorites, a random genre or artist),
 * extracted from PlayerViewModel. Each one picks the songs and hands them to playSongsShuffled.
 */
class PlayerShuffleActions(
    private val scope: CoroutineScope,
    private val musicRepository: MusicRepository,
    private val syncManager: SyncManager,
    private val libraryStateHolder: LibraryStateHolder,
    private val loadedGenres: StateFlow<ImmutableList<Genre>?>,
    private val callbacks: PlayerShuffleCallbacks,
) {
    fun shuffleAllSongs(queueName: String? = null) {
        Logger.withTag("ShuffleDebug").d { "shuffleAllSongs called." }
        scope.launch {
            val randomSongs = musicRepository.getRandomSongs(limit = SHUFFLE_ALL_SONG_LIMIT)
            if (randomSongs.isNotEmpty()) {
                callbacks.playSongsShuffled(
                    randomSongs,
                    queueName ?: getString(Res.string.queue_name_all_songs_shuffled)
                )
            }
        }
    }

    /**
     * Called from the Quick Settings tile. Unlike [shuffleAllSongs], this always starts fresh
     * playback and handles a MediaController that isn't ready yet (cold start from the tile).
     * It samples the repository directly so it does not depend on the in-memory song cache.
     */
    fun triggerShuffleAllFromTile() {
        Logger.d { "[TileDebug] triggerShuffleAllFromTile called. controllerReady=${callbacks.isControllerReady()}" }
        val action: () -> Unit = {
            scope.launch {
                var songs = musicRepository.getRandomSongs(limit = SHUFFLE_ALL_SONG_LIMIT)
                Logger.d { "[TileDebug] Repository returned ${songs.size} random songs immediately" }

                if (songs.isEmpty()) {
                    Logger.d { "[TileDebug] No songs available yet, triggering sync and retrying repository sample" }
                    syncManager.sync()
                    songs = withTimeoutOrNull(30_000L) {
                        var refreshedSongs = emptyList<Song>()
                        while (refreshedSongs.isEmpty()) {
                            refreshedSongs = musicRepository.getRandomSongs(limit = SHUFFLE_ALL_SONG_LIMIT)
                            if (refreshedSongs.isEmpty()) delay(500L)
                        }
                        refreshedSongs
                    } ?: emptyList()
                    Logger.d { "[TileDebug] After retry, repository returned ${songs.size} songs" }
                }

                if (songs.isNotEmpty()) {
                    callbacks.playSongsShuffled(songs, getString(Res.string.queue_name_all_songs_shuffled))
                } else {
                    Logger.w { "[TileDebug] No songs found even after sync - library may be empty" }
                    callbacks.toast(getString(Res.string.player_no_songs_in_library_toast))
                }
            }
        }

        if (callbacks.isControllerReady()) {
            action()
        } else {
            Logger.d { "[TileDebug] mediaController null, queuing as pendingPlaybackAction" }
            callbacks.setPendingPlaybackAction(action)
        }
    }

    fun playRandomSong() = shuffleAllSongs()

    fun shuffleFavoriteSongs() {
        Logger.withTag("ShuffleDebug").d { "shuffleFavoriteSongs called." }
        scope.launch {
            val favSongs = musicRepository.getFavoriteSongsOnce(callbacks.currentStorageFilter())
            if (favSongs.isNotEmpty()) {
                callbacks.playSongsShuffled(favSongs, getString(Res.string.queue_name_liked_songs_shuffled))
            }
        }
    }

    fun shuffleRandomGenre() {
        scope.launch {
            val allGenres = loadedGenres.value ?: libraryStateHolder.genres.first()
            // Unknown is the bucket of untagged songs, so it is only picked when nothing else is.
            val randomGenre = allGenres
                .filterNot { it.id == UNKNOWN_GENRE_ID }
                .ifEmpty { allGenres }
                .randomOrNull() ?: return@launch
            val genreSongs = musicRepository.getMusicByGenre(randomGenre.name).first()
            if (genreSongs.isNotEmpty()) {
                callbacks.playSongsShuffled(genreSongs, randomGenre.name)
            }
        }
    }

    fun shuffleRandomArtist() {
        scope.launch {
            val randomArtist = libraryStateHolder.artists.value.randomOrNull() ?: return@launch
            val artistSongs = musicRepository.getSongsForArtist(randomArtist.id).first()
            if (artistSongs.isNotEmpty()) {
                callbacks.playSongsShuffled(artistSongs, randomArtist.name)
            }
        }
    }
}
