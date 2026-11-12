package io.github.raghavsatyadev.openplayer.data.image.online

import android.content.Context
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import io.github.raghavsatyadev.openplayer.data.database.MusicDao
import io.github.raghavsatyadev.openplayer.data.network.deezer.DeezerApiService
import io.github.raghavsatyadev.openplayer.data.network.itunes.ITunesSearchClient
import io.github.raghavsatyadev.openplayer.data.preferences.OnlineCoverPreferences
import io.github.raghavsatyadev.openplayer.utils.LocalArtworkUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves missing album covers from online sources (Deezer, iTunes) and caches them locally.
 */
class OnlineCoverRepository(
    private val context: Context,
    private val deezer: DeezerApiService,
    private val itunes: ITunesSearchClient,
    private val okHttpClient: OkHttpClient,
    private val preferences: OnlineCoverPreferences,
    private val musicDao: MusicDao
) {
    private val dir = File(context.filesDir, DIR_NAME).apply {
        mkdirs()
        // Misses saved before MISS_FORMAT may have been rate limits, not real misses: retry them once.
        val stamp = File(this, MISS_FORMAT)
        if (!stamp.exists()) {
            listFiles { _, name -> name.endsWith(".none") }?.forEach { it.delete() }
            runCatching { stamp.createNewFile() }
        }
    }
    private val perSongMutexes = ConcurrentHashMap<Long, Mutex>()
    private val networkSemaphore = Semaphore(MAX_CONCURRENT_LOOKUPS)

    companion object {
        private const val TAG = "OnlineCovers"
        private const val DIR_NAME = "online_covers"
        private const val MAX_CONCURRENT_LOOKUPS = 4
        private const val MAX_DOWNLOAD_BYTES = 5L * 1024 * 1024 // 5 MB
        private const val MISS_CACHE_DURATION_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
        /** After a rate limit or network failure, wait this long before asking again. */
        private const val BACKOFF_MS = 2L * 60 * 1000
        private const val MISS_FORMAT = "misses-v2.stamp"
    }

    @Volatile
    private var backoffUntil = 0L

    /**
     * Returns the cached cover file for [songId] if it exists and is non-empty.
     * Safe to call from any thread without network access.
     */
    fun cachedCover(songId: Long): File? {
        val file = File(dir, "$songId.jpg")
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * Clears all "nothing found" marker files.
     */
    fun clearMisses() {
        dir.listFiles { _, name -> name.endsWith(".none") }?.forEach { it.delete() }
    }

    /**
     * Library scans store no artwork URI for songs without local art, so the image loader never
     * asks for them. Give those songs their local artwork URI so online lookups can run.
     */
    suspend fun linkSongsWithoutArtwork(): Int = withContext(Dispatchers.IO) {
        musicDao.linkSongsWithoutArtwork(LocalArtworkUri.SCHEME)
    }

    /**
     * Attempts to resolve and cache an online cover for [songId].
     */
    suspend fun coverFor(songId: Long): File? {
        // Fast paths before acquiring any locks
        cachedCover(songId)?.let { return it }
        if (!canUseNetwork()) return null
        if (isRecentMiss(songId)) return null

        val song = musicDao.getSongByIdOnce(songId)
        if (song == null) {
            writeNoneMarker(songId)
            return null
        }

        val query = CoverQuery.from(
            title = song.title,
            artist = song.artistName,
            album = song.albumName,
            durationMs = song.duration
        )
        if (query == null) {
            writeNoneMarker(songId)
            return null
        }

        val mutex = perSongMutexes.computeIfAbsent(songId) { Mutex() }
        return mutex.withLock {
            // Re-check cache and conditions after acquiring the lock
            cachedCover(songId)?.let { return@withLock it }
            if (!canUseNetwork()) return@withLock null
            if (isRecentMiss(songId)) return@withLock null

            networkSemaphore.withPermit {
                lookupAndDownloadCover(songId, query)
            }
        }
    }

    private suspend fun lookupAndDownloadCover(songId: Long, query: CoverQuery): File? = withContext(Dispatchers.IO) {
        val cleanArtist = query.artist.replace("\"", "").trim()
        val cleanTitle = query.title.replace("\"", "").trim()
        val deezerQuery = "artist:\"$cleanArtist\" track:\"$cleanTitle\""

        var imageUrl: String? = null
        var networkErrorOccurred = false

        // 1. Deezer search
        val deezerResult = runCatching {
            deezer.searchTrack(query = deezerQuery, limit = 5)
        }
        val deezerException = deezerResult.exceptionOrNull()
        val deezerError = deezerResult.getOrNull()?.error
        if (deezerException is IOException) {
            Timber.tag(TAG).d(deezerException, "Deezer search failed with network error")
            networkErrorOccurred = true
        } else if (deezerError != null && (deezerError.code == 4 || deezerError.code == 700)) {
            // Quota or busy: try again later rather than record a miss.
            Timber.tag(TAG).d("Deezer throttled: ${deezerError.code}")
            networkErrorOccurred = true
        } else {
            val tracks = deezerResult.getOrNull()?.data.orEmpty()
            val matchedTrack = CoverMatch.best(query, tracks) { track ->
                Triple(track.title, track.artist?.name.orEmpty(), track.duration * 1000L)
            }
            imageUrl = matchedTrack?.album?.let { it.coverXl ?: it.coverBig ?: it.coverMedium }
        }

        if (networkErrorOccurred) {
            backoffUntil = System.currentTimeMillis() + BACKOFF_MS
            return@withContext null
        }

        // 2. iTunes search if Deezer found no matching cover
        if (imageUrl == null) {
            val itunesTerm = "${query.artist} ${query.title}"
            val itunesResult = runCatching {
                itunes.searchSongs(term = itunesTerm, limit = 5, throwWhenThrottled = true)
            }
            val itunesException = itunesResult.exceptionOrNull()
            if (itunesException is IOException) {
                Timber.tag(TAG).d(itunesException, "iTunes search failed or was throttled")
                networkErrorOccurred = true
            } else {
                val songs = itunesResult.getOrNull().orEmpty()
                val matchedSong = CoverMatch.best(query, songs) { song ->
                    Triple(song.trackName.orEmpty(), song.artistName.orEmpty(), song.trackTimeMillis)
                }
                imageUrl = matchedSong?.bigArtworkUrl()
            }
        }

        if (networkErrorOccurred) {
            backoffUntil = System.currentTimeMillis() + BACKOFF_MS
            return@withContext null
        }

        // When both searches ran fine and found nothing, write .none
        if (imageUrl == null) {
            Timber.tag(TAG).d("No online cover found for song id $songId")
            writeNoneMarker(songId)
            return@withContext null
        }

        // 3. Download the cover image
        downloadImage(songId, imageUrl)
    }

    private fun downloadImage(songId: Long, url: String): File? {
        val tmpFile = File(dir, "$songId.jpg.tmp")
        val targetFile = File(dir, "$songId.jpg")
        val noneFile = File(dir, "$songId.none")

        return try {
            val request = Request.Builder().url(url).get().build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return null
                }
                val body = response.body ?: return null
                if (body.contentLength() > MAX_DOWNLOAD_BYTES) {
                    Timber.tag(TAG).d("Online cover size exceeds 5 MB limit")
                    return null
                }

                var totalRead = 0L
                body.byteStream().use { input ->
                    FileOutputStream(tmpFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            totalRead += read
                            if (totalRead > MAX_DOWNLOAD_BYTES) {
                                Timber.tag(TAG).d("Online cover stream exceeded 5 MB limit")
                                return null
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(tmpFile.absolutePath, options)
                if (options.outWidth <= 0 || options.outHeight <= 0) {
                    Timber.tag(TAG).d("Downloaded file could not be decoded as bitmap")
                    return null
                }

                if (targetFile.exists()) {
                    targetFile.delete()
                }
                if (tmpFile.renameTo(targetFile)) {
                    noneFile.delete()
                    Timber.tag(TAG).d("Successfully cached online cover for song id $songId")
                    targetFile
                } else {
                    null
                }
            }
        } catch (e: IOException) {
            Timber.tag(TAG).d(e, "Network error downloading cover for song id $songId")
            null
        } catch (e: Exception) {
            Timber.tag(TAG).d(e, "Error downloading cover for song id $songId")
            null
        } finally {
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
        }
    }

    private fun canUseNetwork(): Boolean {
        if (!preferences.enabled) return false
        if (System.currentTimeMillis() < backoffUntil) return false
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasInternet) return false
        if (preferences.wifiOnly && cm.isActiveNetworkMetered) {
            return false
        }
        return true
    }

    private fun isRecentMiss(songId: Long): Boolean {
        val noneFile = File(dir, "$songId.none")
        if (!noneFile.exists()) return false
        return (System.currentTimeMillis() - noneFile.lastModified()) < MISS_CACHE_DURATION_MS
    }

    private fun writeNoneMarker(songId: Long) {
        runCatching {
            val noneFile = File(dir, "$songId.none")
            if (!noneFile.exists()) {
                noneFile.createNewFile()
            } else {
                noneFile.setLastModified(System.currentTimeMillis())
            }
        }
    }
}
