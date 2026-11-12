package io.github.raghavsatyadev.openplayer.data.history

import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.database.ListenSource
import org.junit.jupiter.api.Test

class ListenHistoryImportTest {

    private val rule = ListenCountRule(minSeconds = 40, minPercentage = 40)
    private val durations = mapOf("a" to 200_000L, "b" to 50_000L)

    private fun event(song: String, end: Long, listened: Long) =
        ListenHistoryImport.PlayEvent(song, startMs = end - listened, endMs = end, listenedMs = listened)

    @Test
    fun `play events become listens only when they meet the rule`() {
        val rows = ListenHistoryImport.plan(
            events = listOf(
                event("a", 100_000L, 60_000L),
                event("a", 200_000L, 10_000L),
                // 40% of 50 s is 20 s.
                event("b", 300_000L, 21_000L)
            ),
            playCounts = emptyList(),
            songDurationsMs = durations,
            rule = rule
        )
        assertThat(rows.map { it.songId to it.endedAt }).containsExactly("a" to 100_000L, "b" to 300_000L).inOrder()
        assertThat(rows.all { it.source == ListenSource.IMPORTED_PLAY_EVENT }).isTrue()
        assertThat(rows.first().trackDurationMs).isEqualTo(200_000L)
    }

    @Test
    fun `play counts beyond the events file carry over as estimated rows`() {
        val rows = ListenHistoryImport.plan(
            events = listOf(event("a", 100_000L, 60_000L), event("a", 200_000L, 10_000L)),
            playCounts = listOf(
                ListenHistoryImport.PlayCount("a", playCount = 5, totalListenedMs = 370_000L, lastPlayedMs = 200_000L),
                ListenHistoryImport.PlayCount("c", playCount = 2, totalListenedMs = 0L, lastPlayedMs = 0L)
            ),
            songDurationsMs = durations,
            rule = rule
        )
        val estimated = rows.filter { it.source == ListenSource.ESTIMATED_FROM_PLAY_COUNT }
        // "a": 5 plays, 2 in the events file -> 3 older plays of (370 - 70) / 3 s each.
        assertThat(estimated.count { it.songId == "a" }).isEqualTo(3)
        assertThat(estimated.first { it.songId == "a" }.listenedMs).isEqualTo(100_000L)
        assertThat(estimated.count { it.songId == "c" }).isEqualTo(2)
    }

    @Test
    fun `plays recorded live are neither imported nor estimated`() {
        val rows = ListenHistoryImport.plan(
            events = listOf(event("a", 100_000L, 60_000L), event("a", 900_000L, 60_000L)),
            playCounts = listOf(ListenHistoryImport.PlayCount("a", 2, 120_000L, 900_000L)),
            songDurationsMs = durations,
            rule = rule,
            liveFromMs = 500_000L
        )
        assertThat(rows.map { it.endedAt }).containsExactly(100_000L)
    }

    @Test
    fun `a corrupt play count is capped`() {
        val rows = ListenHistoryImport.plan(
            events = emptyList(),
            playCounts = listOf(ListenHistoryImport.PlayCount("a", Int.MAX_VALUE, 0L, 0L)),
            songDurationsMs = durations,
            rule = rule
        )
        assertThat(rows).hasSize(ListenHistoryImport.MAX_ESTIMATED_PER_SONG)
    }
}
