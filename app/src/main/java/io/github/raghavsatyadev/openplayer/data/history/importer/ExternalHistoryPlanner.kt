package io.github.raghavsatyadev.openplayer.data.history.importer

import io.github.raghavsatyadev.openplayer.data.database.ListenHistoryEntity
import io.github.raghavsatyadev.openplayer.data.history.ListenCountRule
import java.util.TreeSet
import kotlin.math.abs

/** Rows to insert and the summary of how the plays were sorted. */
class HistoryImportPlan(val rows: List<ListenHistoryEntity>, val summary: HistoryImportSummary)

/**
 * Turns imported plays into history rows: matches songs, works out start/end times, applies the
 * listen rule to Spotify and drops plays the history already has.
 *
 * Pure logic. [existingStarts] holds the start times of listens already stored (per song id); the
 * planner also remembers what it accepts, so overlapping exports (Last.fm and ListenBrainz) count
 * once. Never touches the database; the caller loads [existingStarts] once for the import's span.
 */
class ExternalHistoryPlanner(
    private val matcher: SongMatcher,
    private val durations: Map<String, Long>,
    private val rule: ListenCountRule,
    existingStarts: Map<String, List<Long>>
) {
    private val starts = HashMap<String, TreeSet<Long>>().also { map ->
        for ((song, times) in existingStarts) map[song] = TreeSet(times)
    }

    fun plan(
        listens: Iterable<ImportedListen>,
        source: HistorySource,
        unparseable: Int = 0,
        failedFiles: Int = 0
    ): HistoryImportPlan {
        val rows = ArrayList<ListenHistoryEntity>()
        var duplicates = 0
        var unmatched = 0
        var belowRule = 0
        val unmatchedTracks = HashMap<String, Int>()

        for (listen in listens) {
            val songId = matcher.match(listen)
            if (songId == null) {
                unmatched++
                unmatchedTracks.merge("${listen.artist} - ${listen.title}", 1, Int::plus)
                continue
            }
            val duration = durations[songId] ?: 0L
            val played = listen.msPlayed
            if (source == HistorySource.SPOTIFY && played != null && !rule.isCounted(played, duration)) {
                belowRule++
                continue
            }
            val startedAt: Long
            val endedAt: Long
            if (listen.timestampIsEnd) {
                endedAt = listen.timestampMs
                startedAt = endedAt - (played ?: duration)
            } else {
                startedAt = listen.timestampMs
                endedAt = startedAt + duration
            }
            if (startedAt < 0L) continue // before 1970: not a real play
            val set = starts.getOrPut(songId) { TreeSet() }
            if (isDuplicate(set, startedAt, duration)) {
                duplicates++
                continue
            }
            set += startedAt
            rows += ListenHistoryEntity(
                songId = songId,
                startedAt = startedAt,
                endedAt = endedAt.coerceAtLeast(startedAt),
                listenedMs = (if (source == HistorySource.SPOTIFY) played else null) ?: duration,
                trackDurationMs = duration,
                source = source.listenSource
            )
        }

        val sample = unmatchedTracks.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(SAMPLE_SIZE)
            .map { UnmatchedTrack(it.key, it.value) }
        return HistoryImportPlan(
            rows,
            HistoryImportSummary(
                imported = rows.size,
                duplicates = duplicates,
                unmatched = unmatched,
                belowRule = belowRule,
                unparseable = unparseable,
                unmatchedSample = sample,
                failedFiles = failedFiles
            )
        )
    }

    private fun isDuplicate(set: TreeSet<Long>, startedAt: Long, durationMs: Long): Boolean {
        val window = dedupeWindowMs(durationMs)
        val near = set.ceiling(startedAt - window) ?: return false
        return abs(near - startedAt) <= window
    }

    companion object {
        const val SAMPLE_SIZE = 20
        private const val MIN_WINDOW_MS = 30_000L
        private const val MAX_WINDOW_MS = 180_000L

        /** How close two starts of one song must be to be the same play. */
        fun dedupeWindowMs(durationMs: Long): Long =
            maxOf(MIN_WINDOW_MS, minOf(durationMs / 2, MAX_WINDOW_MS))

        /** The widest a start can sit from a listen's own time, for loading what already exists. */
        const val SPAN_PADDING_MS = 30 * 60 * 1000L
    }
}
