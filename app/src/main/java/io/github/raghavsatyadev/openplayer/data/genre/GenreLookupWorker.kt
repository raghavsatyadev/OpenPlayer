package io.github.raghavsatyadev.openplayer.data.genre

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.raghavsatyadev.openplayer.data.database.GenreFillDao
import io.github.raghavsatyadev.openplayer.data.database.GenreLookupCacheEntity
import io.github.raghavsatyadev.openplayer.data.preferences.OnlineGenrePreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import co.touchlab.kermit.Logger
import java.util.concurrent.TimeUnit

/**
 * Looks up genres online for songs the offline fill could not place, one album (or loose song)
 * at a time, slowly enough to stay inside the free APIs' limits. Each result, found or not, is
 * cached, so the network is asked once per album; a miss is asked again after [MISS_RETRY_MS].
 * A run stops after [MAX_RUN_MS] and queues the next one, keeping under WorkManager's time limit.
 */
@HiltWorker
class GenreLookupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val filler: GenreFiller,
    private val source: OnlineGenreSource,
    private val dao: GenreFillDao,
    private val preferences: OnlineGenrePreferences,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!preferences.enabled) return Result.success()
        val start = System.currentTimeMillis()
        // Offline first: the artist table may place songs no sync has filled yet.
        filler.fillOffline()
        val groups = filler.missingGenreSongs().groupBy { it.lookupKey }
        val cached = filler.cachedLookups(groups.keys.toList())

        // Albums another song already found online (e.g. a song added since): no request needed.
        val known = HashMap<Long, String>()
        val todo = groups.filter { (key, songs) ->
            val entry = cached[key]
            when {
                entry?.genre != null -> { songs.forEach { known[it.songId] = entry.genre }; false }
                entry != null -> start - entry.checkedAt >= MISS_RETRY_MS
                else -> true
            }
        }
        dao.setInferredGenres(known)

        var looked = 0
        for ((key, songs) in todo) {
            if (looked >= MAX_GROUPS_PER_RUN || System.currentTimeMillis() - start > MAX_RUN_MS) {
                enqueue(WorkManager.getInstance(applicationContext), preferences.wifiOnly, ExistingWorkPolicy.APPEND_OR_REPLACE)
                return Result.success()
            }
            if (!preferences.enabled || isStopped) return Result.success()
            when (val result = lookupGroup(songs)) {
                is GenreLookupResult.Found -> {
                    dao.putCachedLookup(GenreLookupCacheEntity(key, result.genre, System.currentTimeMillis()))
                    dao.setInferredGenres(songs.associate { it.songId to result.genre })
                }
                GenreLookupResult.NotFound ->
                    dao.putCachedLookup(GenreLookupCacheEntity(key, null, System.currentTimeMillis()))
                GenreLookupResult.TryLater -> {
                    Logger.withTag(TAG).d { "Genre lookup paused after $looked albums; retrying later" }
                    return Result.retry()
                }
            }
            looked++
        }
        Logger.withTag(TAG).d { "Genre lookup done: $looked albums looked up, ${known.size} songs from cache" }
        return Result.success()
    }

    /** Tries up to [TRIES_PER_GROUP] different songs of the album; the first match wins. */
    private suspend fun lookupGroup(songs: List<GenreSongKey>): GenreLookupResult {
        var result: GenreLookupResult = GenreLookupResult.NotFound
        for (song in songs.distinctBy { it.query.title.lowercase() }.take(TRIES_PER_GROUP)) {
            result = source.lookup(song.query)
            delay(REQUEST_GAP_MS * source.lastRequestCount.coerceAtLeast(1))
            if (result != GenreLookupResult.NotFound) break
        }
        return result
    }

    companion object {
        private const val TAG = "OnlineGenres"
        const val WORK_NAME = "online_genre_lookup"
        private const val TRIES_PER_GROUP = 2
        private const val MAX_GROUPS_PER_RUN = 100
        private const val MAX_RUN_MS = 8L * 60 * 1000
        /** About 17 requests a minute, under iTunes' roughly 20. */
        private const val REQUEST_GAP_MS = 3_500L
        private const val MISS_RETRY_MS = 30L * 24 * 60 * 60 * 1000

        fun enqueue(workManager: WorkManager, wifiOnly: Boolean, policy: ExistingWorkPolicy) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<GenreLookupWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()
            workManager.enqueueUniqueWork(WORK_NAME, policy, request)
        }

        fun cancel(workManager: WorkManager) = workManager.cancelUniqueWork(WORK_NAME)
    }
}
