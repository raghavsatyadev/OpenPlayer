package io.github.raghavsatyadev.openplayer.data.history.recommend

import java.time.Instant
import java.time.LocalDate
import kotlin.math.pow
import kotlin.random.Random

/**
 * Song picks drawn from your own listen history, after Namida's generators. Every function returns
 * song ids, best first, and never the seed itself.
 */
object HistoryPicks {

    /** Listens taken on each side of the seed when looking for what was played with it. */
    const val ALONGSIDE_SAMPLE = 10

    /** A seed listen younger than this is the session going on now, so it is skipped if possible. */
    private const val CURRENT_SESSION_MS = 2 * 60 * 60 * 1000L

    const val TIME_RANGE_DAYS = 7
    const val DISCOVER_DAYS = 18
    private const val DISCOVER_FIRST_LISTENS = 2

    const val LOST_MEMORIES_DAYS = 5

    private const val TIME_OF_DAY_WINDOW_MIN = 120
    private const val TIME_OF_DAY_MIN_LISTENS = 3
    private const val TIME_OF_DAY_MIN_IN_WINDOW = 2
    private const val TIME_OF_DAY_MIN_SHARE = 1.0 / 3
    private const val MINUTES_PER_DAY = 24 * 60

    /**
     * Songs heard just before and after the seed, counted over every time the seed was played and
     * ranked by how often they turn up.
     */
    fun playedAlongside(
        index: HistoryIndex,
        seedId: String,
        now: Long,
        sample: Int = ALONGSIDE_SAMPLE,
    ): List<String> {
        val newestFirst = index.listens.asReversed()
        if (newestFirst.isEmpty()) return emptyList()
        val tally = ListenTally()
        var skipped: List<TimedListen>? = null
        var i = 0
        while (i < newestFirst.size) {
            val listen = newestFirst[i]
            if (listen.songId != seedId) {
                i++
                continue
            }
            val window = newestFirst.subList((i - sample).coerceAtLeast(0), (i + sample).coerceAtMost(newestFirst.size))
            if (tally.isEmpty && skipped == null && now - listen.endedAt < CURRENT_SESSION_MS) {
                skipped = window
                i++
                continue
            }
            window.forEach { tally.add(it.songId) }
            i += sample
        }
        if (tally.isEmpty) skipped?.forEach { tally.add(it.songId) }
        return tally.ranked(seedId)
    }

    /** Songs heard within [days] of any of the seed's listens. */
    fun sameTimeRange(index: HistoryIndex, seedId: String, days: Int = TIME_RANGE_DAYS): List<String> {
        val times = index.timesBySong[seedId] ?: return emptyList()
        val tally = ListenTally()
        daysAround(index, times.asIterable(), days).forEach { day ->
            index.listensByDay[day]?.forEach { tally.add(it.songId) }
        }
        return tally.ranked(seedId)
    }

    /**
     * Songs you found around when you found the seed: songs whose own first listens fall within
     * [days] of the seed's first listens. Ranked by total listens.
     */
    fun discoveredTogether(index: HistoryIndex, seedId: String, days: Int = DISCOVER_DAYS): List<String> {
        val times = index.timesBySong[seedId] ?: return emptyList()
        val tally = ListenTally()
        daysAround(index, times.take(DISCOVER_FIRST_LISTENS), days).forEach { day ->
            index.listensByDay[day]?.forEach { l ->
                val first = index.timesBySong[l.songId] ?: return@forEach
                if (isAmongFirst(first, l.endedAt)) tally.add(l.songId)
            }
        }
        return tally.ranked(seedId) { id, local -> index.listenCount(id).takeIf { it > 0 } ?: local }
    }

    /** Songs from the seed's year (a `yyyyMMdd` year counts as its `yyyy`), most heard first. */
    fun sameEra(seedYear: Int, seedId: String, songYears: Map<String, Int>, index: HistoryIndex): List<String> {
        val year = yearOf(seedYear)
        if (year <= 0) return emptyList()
        return songYears.entries
            .filter { it.key != seedId && yearOf(it.value) == year }
            .sortedByDescending { index.listenCount(it.key) }
            .map { it.key }
    }

    /** Years you have listens in, newest first, without the current one. */
    fun lostMemoryYears(index: HistoryIndex, now: Long): List<Int> {
        val thisYear = index.dayOf(now).year
        return index.years().filter { it < thisYear }
    }

    /** What you played most within [days] of today's date in [year]. */
    fun lostMemories(index: HistoryIndex, year: Int, now: Long, days: Int = LOST_MEMORIES_DAYS): List<String> {
        val center = index.dayOf(now).withYear(year)
        val tally = ListenTally()
        for (offset in -days..days) {
            index.listensByDay[center.plusDays(offset.toLong())]?.forEach { tally.add(it.songId) }
        }
        return tally.ranked(null)
    }

    /**
     * Songs you mostly play around this time of day. A song needs a few listens, some of them
     * within two hours of the clock time now, and a fair share of all of them. The order is a
     * weighted random draw, so heavier songs tend to come first but the list changes each time.
     */
    fun aroundTimeOfDay(index: HistoryIndex, now: Long, count: Int, random: Random = Random.Default): List<String> {
        val nowLocal = Instant.ofEpochMilli(now).atZone(index.zone)
        val nowMinute = nowLocal.hour * 60 + nowLocal.minute
        val picks = ArrayList<Pair<String, Double>>()
        for (songId in index.songsByCount) {
            val times = index.timesBySong[songId] ?: continue
            if (times.size < TIME_OF_DAY_MIN_LISTENS) break
            val inWindow = times.count { t ->
                val local = Instant.ofEpochMilli(t).atZone(index.zone)
                minuteDistance(local.hour * 60 + local.minute, nowMinute) <= TIME_OF_DAY_WINDOW_MIN
            }
            if (inWindow < TIME_OF_DAY_MIN_IN_WINDOW) continue
            val share = inWindow.toDouble() / times.size
            if (share < TIME_OF_DAY_MIN_SHARE) continue
            val weight = inWindow * share
            picks += songId to random.nextDouble().pow(1 / weight)
        }
        return picks.sortedByDescending { it.second }.take(count).map { it.first }
    }

    private fun daysAround(index: HistoryIndex, times: Iterable<Long>, days: Int): Set<LocalDate> {
        val result = HashSet<LocalDate>()
        for (t in times) {
            val day = index.dayOf(t)
            for (offset in -days..days) result += day.plusDays(offset.toLong())
        }
        return result
    }

    private fun isAmongFirst(times: LongArray, time: Long): Boolean {
        for (k in 0 until minOf(DISCOVER_FIRST_LISTENS, times.size)) if (times[k] == time) return true
        return false
    }

    private fun minuteDistance(a: Int, b: Int): Int {
        val d = kotlin.math.abs(a - b)
        return minOf(d, MINUTES_PER_DAY - d)
    }

    /** `yyyy` stays as it is; `yyyyMMdd` becomes `yyyy`. */
    fun yearOf(year: Int): Int = if (year < 10_000) year else year / 10_000
}
