package io.github.raghavsatyadev.openplayer.data.history.recommend

import io.github.raghavsatyadev.openplayer.data.database.ListenHistoryDao
import io.github.raghavsatyadev.openplayer.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** Loads the listen history once per change and hands out the [HistoryPicks] built on it. */
@Singleton
class HistoryPicksRepository @Inject constructor(
    private val listenHistoryDao: ListenHistoryDao,
) {
    private val mutex = Mutex()
    private var cached: HistoryIndex? = null
    private var cachedCount = -1

    /** Emits when the history changes, so screens can rebuild their picks. */
    val changes: Flow<Int> = listenHistoryDao.countFlow()

    suspend fun index(): HistoryIndex = withContext(Dispatchers.IO) {
        mutex.withLock {
            val count = listenHistoryDao.count()
            cached?.takeIf { count == cachedCount }
                ?: HistoryIndex(listenHistoryDao.timedListens()).also {
                    cached = it
                    cachedCount = count
                }
        }
    }

    /**
     * Songs for the Daily Mix from history: what you play at this hour, what you played with your
     * latest tracks, and what you played around today a year ago. Interleaved, so each source gets
     * a share. Only songs still in [library] are returned.
     */
    suspend fun dailyMixPicks(library: List<Song>, limit: Int, now: Long, random: Random): List<Song> {
        val index = index()
        if (index.isEmpty || limit <= 0) return emptyList()
        val byId = library.associateBy { it.id }
        val recentSeeds = index.listens.asReversed().asSequence().map { it.songId }.distinct().take(3).toList()
        val sources = buildList {
            add(HistoryPicks.aroundTimeOfDay(index, now, limit, random))
            recentSeeds.forEach { add(HistoryPicks.playedAlongside(index, it, now).take(limit)) }
            HistoryPicks.lostMemoryYears(index, now).firstOrNull()?.let {
                add(HistoryPicks.lostMemories(index, it, now).take(limit))
            }
        }
        return interleave(sources, recentSeeds.toSet())
            .mapNotNull(byId::get)
            .take(limit)
    }

    private fun interleave(sources: List<List<String>>, skip: Set<String>): List<String> {
        val seen = LinkedHashSet<String>()
        val longest = sources.maxOfOrNull { it.size } ?: 0
        for (i in 0 until longest) {
            for (source in sources) source.getOrNull(i)?.takeIf { it !in skip }?.let(seen::add)
        }
        return seen.toList()
    }
}
