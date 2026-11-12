package io.github.raghavsatyadev.openplayer.data.history

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.history.recommend.HistoryIndex
import io.github.raghavsatyadev.openplayer.data.history.recommend.HistoryPicks
import io.github.raghavsatyadev.openplayer.data.history.recommend.TimedListen
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random

/** History picks on a made-up history held in memory; nothing touches the app's database. */
@RunWith(AndroidJUnit4::class)
class HistoryPicksTest {

    private val zone = ZoneOffset.UTC
    private val now = at(2026, 11, 11, 9, 0)

    private fun at(y: Int, m: Int, d: Int, h: Int = 12, min: Int = 0): Long =
        LocalDateTime.of(y, m, d, h, min).toInstant(zone).toEpochMilli()

    private fun index(vararg listens: Pair<String, Long>) =
        HistoryIndex(listens.map { TimedListen(it.first, it.second) }.sortedBy { it.endedAt }, zone)

    @Test
    fun playedAlongside_ranksNeighboursByHowOftenTheyTurnUp() {
        val index = index(
            "b" to at(2026, 1, 1, 10, 0), "seed" to at(2026, 1, 1, 10, 4), "c" to at(2026, 1, 1, 10, 8),
            "b" to at(2026, 3, 1, 10, 0), "seed" to at(2026, 3, 1, 10, 4),
        )
        val picks = HistoryPicks.playedAlongside(index, "seed", now)
        assertThat(picks).containsExactly("b", "c").inOrder()
    }

    @Test
    fun sameTimeRange_keepsOnlySongsWithinAWeek() {
        val index = index(
            "seed" to at(2026, 5, 10), "near" to at(2026, 5, 16), "far" to at(2026, 5, 30),
        )
        assertThat(HistoryPicks.sameTimeRange(index, "seed")).containsExactly("near")
    }

    @Test
    fun discoveredTogether_ignoresSongsFoundLongBefore() {
        val index = index(
            "old" to at(2025, 1, 1), "old" to at(2025, 1, 2),
            "seed" to at(2026, 5, 1),
            "new" to at(2026, 5, 10), "old" to at(2026, 5, 5),
        )
        assertThat(HistoryPicks.discoveredTogether(index, "seed")).containsExactly("new")
    }

    @Test
    fun sameEra_readsFullDatesAsTheirYear() {
        val index = index("a" to at(2026, 1, 1), "a" to at(2026, 1, 2), "b" to at(2026, 1, 3))
        val years = mapOf("seed" to 1999, "a" to 19990612, "b" to 1999, "c" to 2001)
        assertThat(HistoryPicks.sameEra(1999, "seed", years, index)).containsExactly("a", "b").inOrder()
    }

    @Test
    fun lostMemories_takesDaysAroundTodayInPastYears() {
        val index = index(
            "inside" to at(2024, 11, 14), "inside" to at(2024, 11, 8),
            "outside" to at(2024, 12, 1), "thisYear" to at(2026, 11, 10),
        )
        assertThat(HistoryPicks.lostMemoryYears(index, now)).containsExactly(2024)
        assertThat(HistoryPicks.lostMemories(index, 2024, now)).containsExactly("inside")
    }

    @Test
    fun aroundTimeOfDay_picksSongsUsuallyHeardNearThisHour() {
        val morning = (1..4).map { "morning" to at(2026, 10, it, 8, 30) }
        val evening = (1..4).map { "evening" to at(2026, 10, it, 21, 0) }
        val index = index(*(morning + evening).toTypedArray())
        assertThat(HistoryPicks.aroundTimeOfDay(index, now, 10, Random(1))).containsExactly("morning")
    }
}
