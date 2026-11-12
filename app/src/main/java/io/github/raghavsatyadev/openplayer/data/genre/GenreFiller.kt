package io.github.raghavsatyadev.openplayer.data.genre

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import io.github.raghavsatyadev.openplayer.data.database.GenreFillDao
import io.github.raghavsatyadev.openplayer.data.database.GenreLookupCacheEntity
import io.github.raghavsatyadev.openplayer.data.playlist.ArtistGenreLoader
import io.github.raghavsatyadev.openplayer.data.preferences.OnlineGenrePreferences
import io.github.raghavsatyadev.openplayer.utils.GenreParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import co.touchlab.kermit.Logger

/**
 * Gives local songs whose files carry no genre one, without touching the files:
 * 1. a genre found online earlier for the same album (kept in the lookup cache, so rescans
 *    restore it without the network);
 * 2. the bundled artist table (offline);
 * 3. if the user turned it on, an online lookup in the background ([GenreLookupWorker]).
 * Songs with a genre tag and songs whose genre the user edited are never changed.
 */
class GenreFiller(
    private val dao: GenreFillDao,
    private val artistGenres: ArtistGenreLoader,
    private val preferences: OnlineGenrePreferences,
    private val workManager: WorkManager,
) {
    /** Runs after each library sync. Returns how many songs got a genre. */
    suspend fun fillAfterSync(): Int = withContext(Dispatchers.IO) {
        val (filled, missing) = fillOffline()
        if (preferences.enabled && filled < missing) {
            GenreLookupWorker.enqueue(workManager, preferences.wifiOnly, ExistingWorkPolicy.KEEP)
        }
        filled
    }

    /**
     * Fills what needs no network: cached online results, then the artist table. Returns how
     * many songs got a genre and how many were missing one.
     */
    suspend fun fillOffline(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val songs = missingGenreSongs()
        if (songs.isEmpty()) return@withContext 0 to 0

        val cached = cachedLookups(songs.map { it.lookupKey }.distinct())
        val table = artistGenres.load(songs.flatMapTo(HashSet()) { it.artistKeys })
        val genres = HashMap<Long, String>()
        for (song in songs) {
            val genre = cached[song.lookupKey]?.genre
                ?: song.artistKeys.firstNotNullOfOrNull { table.genreFor(it) }?.let(BroadGenre::of)
            if (genre != null) genres[song.songId] = genre
        }
        dao.setInferredGenres(genres)
        Logger.withTag(TAG).d { "Filled ${genres.size} of ${songs.size} songs with no genre" }
        genres.size to songs.size
    }

    /** Local, never-edited songs that still have no genre and have an artist and title to look up. */
    suspend fun missingGenreSongs(): List<GenreSongKey> =
        dao.getGenreFillCandidates(GenreParser.noGenreCharacters)
            .filter { GenreParser.split(it.genre).isEmpty() }
            .mapNotNull { GenreSongKey.of(it) }

    suspend fun cachedLookups(keys: List<String>): Map<String, GenreLookupCacheEntity> =
        keys.chunked(SQL_CHUNK).flatMap { dao.getCachedLookups(it) }.associateBy { it.lookupKey }

    private companion object {
        const val TAG = "GenreFiller"
        const val SQL_CHUNK = 900
    }
}
