package io.github.raghavsatyadev.openplayer.data.history.recommend

import androidx.room.ColumnInfo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One listen with a real time (estimated rows are left out). */
data class TimedListen(
    @ColumnInfo(name = "song_id") val songId: String,
    @ColumnInfo(name = "ended_at") val endedAt: Long,
)

/**
 * The listen history arranged the ways the picks read it: in play order, per song, and per day.
 * Built once from rows sorted oldest first.
 */
class HistoryIndex(listensOldestFirst: List<TimedListen>, val zone: ZoneId = ZoneId.systemDefault()) {

    /** Every listen, oldest first. */
    val listens: List<TimedListen> = listensOldestFirst

    /** Each song's listen times, oldest first. */
    val timesBySong: Map<String, LongArray>

    /** Listens on each local day. */
    val listensByDay: Map<LocalDate, List<TimedListen>>

    /** Songs ordered by how often they were heard, most first. */
    val songsByCount: List<String>

    init {
        val times = HashMap<String, MutableList<Long>>()
        val days = HashMap<LocalDate, MutableList<TimedListen>>()
        for (l in listensOldestFirst) {
            times.getOrPut(l.songId) { ArrayList() } += l.endedAt
            days.getOrPut(dayOf(l.endedAt)) { ArrayList() } += l
        }
        timesBySong = times.mapValues { (_, v) -> v.toLongArray() }
        listensByDay = days
        songsByCount = timesBySong.entries
            .sortedWith(compareByDescending<Map.Entry<String, LongArray>> { it.value.size }.thenByDescending { it.value.last() })
            .map { it.key }
    }

    fun dayOf(epochMs: Long): LocalDate = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

    fun listenCount(songId: String): Int = timesBySong[songId]?.size ?: 0

    /** Years that have listens, newest first. */
    fun years(): List<Int> = listensByDay.keys.map { it.year }.distinct().sortedDescending()

    val isEmpty: Boolean get() = listens.isEmpty()
}

/** Counts how many times each song turns up, then ranks them. */
internal class ListenTally {
    private val counts = LinkedHashMap<String, Int>()

    fun add(songId: String) {
        counts[songId] = (counts[songId] ?: 0) + 1
    }

    val isEmpty: Boolean get() = counts.isEmpty()

    /** Most counted first, [seed] removed; [sortKey] replaces the count as the ranking value. */
    fun ranked(seed: String?, sortKey: ((String, Int) -> Int)? = null): List<String> {
        seed?.let(counts::remove)
        val key: (Map.Entry<String, Int>) -> Int = if (sortKey == null) {
            { it.value }
        } else {
            { sortKey(it.key, it.value) }
        }
        return counts.entries.sortedByDescending(key).map { it.key }
    }
}
