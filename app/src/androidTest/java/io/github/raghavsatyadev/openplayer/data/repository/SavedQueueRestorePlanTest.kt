package io.github.raghavsatyadev.openplayer.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.model.Song
import org.junit.Test
import org.junit.runner.RunWith

/** How a saved queue maps back onto the songs that still exist. */
@RunWith(AndroidJUnit4::class)
class SavedQueueRestorePlanTest {

    private fun song(id: String) = Song.emptySong().copy(id = id, title = "Song $id")

    private fun queue(ids: List<String>, index: Int, positionMs: Long) =
        SavedQueue(1L, "q", 0L, ids, index, positionMs)

    @Test
    fun everySongPresentRestoresIndexAndPosition() {
        val plan = SavedQueueRestorePlan.build(
            queue(listOf("1", "2", "3"), index = 1, positionMs = 5_000L),
            listOf("1", "2", "3").associateWith(::song)
        )
        assertThat(plan.songs.map { it.id }).containsExactly("1", "2", "3").inOrder()
        assertThat(plan.startIndex).isEqualTo(1)
        assertThat(plan.positionMs).isEqualTo(5_000L)
        assertThat(plan.missingCount).isEqualTo(0)
    }

    @Test
    fun missingSongsBeforeTheCurrentOneShiftTheIndex() {
        val plan = SavedQueueRestorePlan.build(
            queue(listOf("1", "2", "3"), index = 2, positionMs = 7_000L),
            listOf("2", "3").associateWith(::song)
        )
        assertThat(plan.songs.map { it.id }).containsExactly("2", "3").inOrder()
        assertThat(plan.startIndex).isEqualTo(1)
        assertThat(plan.positionMs).isEqualTo(7_000L)
        assertThat(plan.missingCount).isEqualTo(1)
    }

    @Test
    fun missingCurrentSongStartsTheNextOneFromTheBeginning() {
        val plan = SavedQueueRestorePlan.build(
            queue(listOf("1", "2", "3"), index = 1, positionMs = 7_000L),
            listOf("1", "3").associateWith(::song)
        )
        assertThat(plan.songs[plan.startIndex].id).isEqualTo("3")
        assertThat(plan.positionMs).isEqualTo(0L)
    }

    @Test
    fun repeatedSongsAreKeptAndNothingLeftIsEmpty() {
        val repeated = SavedQueueRestorePlan.build(
            queue(listOf("1", "1"), index = 1, positionMs = 0L),
            mapOf("1" to song("1"))
        )
        assertThat(repeated.songs).hasSize(2)
        assertThat(repeated.startIndex).isEqualTo(1)

        val none = SavedQueueRestorePlan.build(queue(listOf("9"), 0, 0L), emptyMap())
        assertThat(none.isEmpty).isTrue()
        assertThat(none.missingCount).isEqualTo(1)
    }
}
