package io.github.raghavsatyadev.openplayer.data.history

import io.github.raghavsatyadev.openplayer.data.database.EngagementDao
import io.github.raghavsatyadev.openplayer.data.database.ListenHistoryDao
import io.github.raghavsatyadev.openplayer.data.database.ListenHistoryEntity
import io.github.raghavsatyadev.openplayer.data.database.ListenSource
import io.github.raghavsatyadev.openplayer.data.database.SongListenCount
import io.github.raghavsatyadev.openplayer.data.preferences.ListenHistoryPreferences
import io.github.raghavsatyadev.openplayer.data.stats.PlaybackStatsRepository
import io.github.raghavsatyadev.openplayer.di.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import co.touchlab.kermit.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-listen history: one row per play that met the [ListenCountRule].
 * Listening time keeps going to [PlaybackStatsRepository]; this records what counted as a listen.
 */
@Singleton
class ListenHistoryRepository @Inject constructor(
    private val listenHistoryDao: ListenHistoryDao,
    private val engagementDao: EngagementDao,
    private val playbackStatsRepository: PlaybackStatsRepository,
    private val preferences: ListenHistoryPreferences,
    @AppScope private val appScope: CoroutineScope
) {
    private val importMutex = Mutex()

    @Volatile
    private var imported = false

    /** Plays after this are recorded live, so the import leaves them out. */
    private val createdAtMs = System.currentTimeMillis()

    @Volatile
    var rule: ListenCountRule = ListenCountRule()
        private set

    init {
        appScope.launch { preferences.ruleFlow.collect { rule = it } }
        appScope.launch {
            runCatching { importIfNeeded() }
                .onFailure { Logger.withTag(TAG).e(it) { "Listen history import failed" } }
        }
    }

    suspend fun record(listen: CountedListen) {
        if (listen.songId.isBlank()) return
        listenHistoryDao.insert(
            ListenHistoryEntity(
                songId = listen.songId,
                startedAt = listen.startedAtEpochMs.coerceAtLeast(0L),
                endedAt = listen.endedAtEpochMs.coerceAtLeast(0L),
                listenedMs = listen.listenedMs.coerceAtLeast(0L),
                trackDurationMs = listen.trackDurationMs.coerceAtLeast(0L),
                source = ListenSource.LIVE
            )
        )
    }

    fun recentListens(limit: Int): Flow<List<ListenHistoryEntity>> =
        listenHistoryDao.recentListensFlow(limit)

    suspend fun listensBetween(fromInclusive: Long, toExclusive: Long): List<ListenHistoryEntity> =
        listenHistoryDao.listensBetween(fromInclusive, toExclusive)

    suspend fun listenCounts(sinceInclusive: Long = 0L): List<SongListenCount> =
        listenHistoryDao.listenCounts(sinceInclusive)

    suspend fun listenCount(songId: String): Int = listenHistoryDao.countForSong(songId)

    /** Seeds the table once from the older stats (see [ListenHistoryImport]). */
    suspend fun importIfNeeded() {
        if (imported) return
        importMutex.withLock {
            if (imported) return
            if (preferences.isImported()) {
                imported = true
                return
            }
            val rule = preferences.currentRule()
            val events = playbackStatsRepository.exportEventsForBackup().map { event ->
                val end = event.endTimestamp ?: event.timestamp
                ListenHistoryImport.PlayEvent(
                    songId = event.songId,
                    startMs = event.startTimestamp ?: (end - event.durationMs),
                    endMs = end,
                    listenedMs = event.durationMs
                )
            }
            val playCounts = engagementDao.getAllEngagements().map { engagement ->
                ListenHistoryImport.PlayCount(
                    songId = engagement.songId,
                    playCount = engagement.playCount,
                    totalListenedMs = engagement.totalPlayDurationMs,
                    lastPlayedMs = engagement.lastPlayedTimestamp
                )
            }
            val durations = listenHistoryDao.getSongDurations().associate { it.id.toString() to it.duration }
            val rows = ListenHistoryImport.plan(events, playCounts, durations, rule, liveFromMs = createdAtMs)
            rows.chunked(INSERT_CHUNK).forEach { listenHistoryDao.insertAll(it) }
            preferences.markImported()
            imported = true
            Logger.withTag(TAG).i { String.format("Imported %d listens (%d play events, %d play counts)", rows.size, events.size, playCounts.size) }
        }
    }

    private companion object {
        const val TAG = "ListenHistory"
        const val INSERT_CHUNK = 500
    }
}
