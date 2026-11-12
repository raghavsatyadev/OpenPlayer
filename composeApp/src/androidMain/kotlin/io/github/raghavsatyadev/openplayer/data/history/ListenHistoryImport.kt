package io.github.raghavsatyadev.openplayer.data.history

import io.github.raghavsatyadev.openplayer.data.database.ListenHistoryEntity
import io.github.raghavsatyadev.openplayer.data.database.ListenSource

/**
 * Builds the first listen history from the stats OpenPlayer kept before it:
 * the per-play events file (real times, ~2 years) and the per-song play counts (all time).
 *
 * - A play event becomes a listen if it meets [rule] for the song's length.
 * - Plays from [liveFromMs] on were already recorded live, so they make no rows here.
 * - A song whose play count is higher than its events (plays older than the events file) gets
 *   the difference as [ListenSource.ESTIMATED_FROM_PLAY_COUNT] rows, so all-time counts carry over.
 */
object ListenHistoryImport {

    data class PlayEvent(
        val songId: String,
        val startMs: Long,
        val endMs: Long,
        val listenedMs: Long
    )

    data class PlayCount(
        val songId: String,
        val playCount: Int,
        val totalListenedMs: Long,
        val lastPlayedMs: Long
    )

    /** At most this many estimated rows per song, so a corrupt count can't flood the table. */
    const val MAX_ESTIMATED_PER_SONG = 2_000

    fun plan(
        events: List<PlayEvent>,
        playCounts: List<PlayCount>,
        songDurationsMs: Map<String, Long>,
        rule: ListenCountRule,
        liveFromMs: Long = Long.MAX_VALUE
    ): List<ListenHistoryEntity> {
        val rows = ArrayList<ListenHistoryEntity>(events.size)
        val eventsPerSong = HashMap<String, Int>()
        val eventMsPerSong = HashMap<String, Long>()

        for (event in events) {
            if (event.songId.isBlank()) continue
            eventsPerSong[event.songId] = (eventsPerSong[event.songId] ?: 0) + 1
            eventMsPerSong[event.songId] = (eventMsPerSong[event.songId] ?: 0L) + event.listenedMs.coerceAtLeast(0L)
            if (event.endMs >= liveFromMs) continue
            val durationMs = songDurationsMs[event.songId] ?: 0L
            if (!rule.isCounted(event.listenedMs, durationMs)) continue
            val end = event.endMs.coerceAtLeast(0L)
            rows += ListenHistoryEntity(
                songId = event.songId,
                startedAt = event.startMs.coerceIn(0L, end),
                endedAt = end,
                listenedMs = event.listenedMs,
                trackDurationMs = durationMs.coerceAtLeast(0L),
                source = ListenSource.IMPORTED_PLAY_EVENT
            )
        }

        for (count in playCounts) {
            if (count.songId.isBlank()) continue
            val missing = (count.playCount - (eventsPerSong[count.songId] ?: 0))
                .coerceIn(0, MAX_ESTIMATED_PER_SONG)
            if (missing == 0) continue
            val leftoverMs = (count.totalListenedMs - (eventMsPerSong[count.songId] ?: 0L)).coerceAtLeast(0L)
            val averageMs = leftoverMs / missing
            val placeholderTime = count.lastPlayedMs.coerceAtLeast(0L)
            repeat(missing) {
                rows += ListenHistoryEntity(
                    songId = count.songId,
                    startedAt = placeholderTime,
                    endedAt = placeholderTime,
                    listenedMs = averageMs,
                    trackDurationMs = (songDurationsMs[count.songId] ?: 0L).coerceAtLeast(0L),
                    source = ListenSource.ESTIMATED_FROM_PLAY_COUNT
                )
            }
        }

        rows.sortBy { it.endedAt }
        return rows
    }
}
