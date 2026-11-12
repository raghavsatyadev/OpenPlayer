package io.github.raghavsatyadev.openplayer.data.genre

import io.github.raghavsatyadev.openplayer.data.image.online.CoverMatch
import io.github.raghavsatyadev.openplayer.data.image.online.CoverQuery
import io.github.raghavsatyadev.openplayer.data.network.deezer.DeezerApiService
import io.github.raghavsatyadev.openplayer.data.network.deezer.DeezerError
import io.github.raghavsatyadev.openplayer.data.network.itunes.ITunesSearchClient
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import co.touchlab.kermit.Logger
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of one online genre lookup. */
sealed interface GenreLookupResult {
    data class Found(val genre: String) : GenreLookupResult
    data object NotFound : GenreLookupResult
    /** The network failed or the service asked us to slow down: try again later, cache nothing. */
    data object TryLater : GenreLookupResult
}

/**
 * Finds a song's genre on iTunes (the track's own genre, one request), then Deezer (the matched
 * track's album genres, two requests). A result only counts when title, artist and duration
 * match, the same check the online cover lookup uses.
 */
@Singleton
class OnlineGenreSource @Inject constructor(
    private val itunes: ITunesSearchClient,
    private val deezer: DeezerApiService,
) {
    /** Number of requests the last [lookup] made, for throttling. */
    @Volatile
    var lastRequestCount: Int = 0
        private set

    suspend fun lookup(query: CoverQuery): GenreLookupResult {
        lastRequestCount = 0
        return try {
            fromITunes(query)?.let { return GenreLookupResult.Found(it) }
            fromDeezer(query)?.let { return GenreLookupResult.Found(it) }
            GenreLookupResult.NotFound
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Logger.withTag(TAG).d(e) { "Genre lookup network error" }
            GenreLookupResult.TryLater
        } catch (e: HttpException) {
            Logger.withTag(TAG).d(e) { "Genre lookup HTTP ${e.code()}" }
            if (e.code() == 429 || e.code() >= 500) GenreLookupResult.TryLater else GenreLookupResult.NotFound
        }
    }

    private suspend fun fromITunes(query: CoverQuery): String? {
        lastRequestCount++
        val songs = itunes.searchSongs(term = "${query.artist} ${query.title}", limit = 5, throwWhenThrottled = true)
        val match = CoverMatch.best(query, songs) { Triple(it.trackName.orEmpty(), it.artistName.orEmpty(), it.trackTimeMillis) }
        return match?.primaryGenreName?.takeUnless { it.isGeneric() }?.let(BroadGenre::of)
    }

    private suspend fun fromDeezer(query: CoverQuery): String? {
        val artist = query.artist.replace("\"", "").trim()
        val title = query.title.replace("\"", "").trim()
        lastRequestCount++
        val search = deezer.searchTrack(query = "artist:\"$artist\" track:\"$title\"", limit = 5)
        if (search.error != null) return search.error.orThrowIfThrottled()
        val track = CoverMatch.best(query, search.data) {
            Triple(it.title, it.artist?.name.orEmpty(), it.duration * 1000L)
        } ?: return null
        val albumId = track.album?.id?.takeIf { it > 0 } ?: return null
        lastRequestCount++
        val album = deezer.getAlbum(albumId)
        if (album.error != null) return album.error.orThrowIfThrottled()
        val genre = album.genres?.data?.firstOrNull { !it.name.isGeneric() }?.name
        return BroadGenre.of(genre)
    }

    /** Quota (4) and busy (700) mean "later"; anything else, such as not found (800), is a miss. */
    private fun DeezerError.orThrowIfThrottled(): String? {
        if (code == 4 || code == 700) throw IOException("Deezer error $code: $message")
        return null
    }

    /** Store-wide catch-alls that say nothing about the song. */
    private fun String.isGeneric(): Boolean =
        isBlank() || trim().lowercase() in setOf("music", "all", "other", "unknown", "various")

    private companion object {
        const val TAG = "OnlineGenres"
    }
}
