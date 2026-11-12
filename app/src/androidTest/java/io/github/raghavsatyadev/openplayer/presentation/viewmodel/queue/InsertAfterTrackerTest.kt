package io.github.raghavsatyadev.openplayer.presentation.viewmodel.queue

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** The queue is a plain mutable list here; the tracker only sees indices and media ids. */
@RunWith(AndroidJUnit4::class)
class InsertAfterTrackerTest {

    private val tracker = InsertAfterTracker()
    private val queue = mutableListOf("c0", "c1", "c2", "c3")
    private var current = 0

    /** What "Play next" does with the tracker. */
    private fun playNext(id: String) {
        val index = tracker.resolveInsertionIndex(current, queue.size) { queue.getOrNull(it) }
        queue.add(index, id)
        tracker.recordInserted(index, id)
    }

    @Test
    fun successiveInsertsKeepTheOrderTheyWereChosenIn() {
        playNext("a")
        playNext("b")
        playNext("c")
        assertThat(queue).containsExactly("c0", "a", "b", "c", "c1", "c2", "c3").inOrder()
    }

    @Test
    fun withNothingTrackedTheSongGoesRightAfterTheCurrentOne() {
        current = 2
        playNext("a")
        assertThat(queue).containsExactly("c0", "c1", "c2", "a", "c3").inOrder()
    }

    @Test
    fun removingAnItemBeforeTheLatestShiftsIt() {
        playNext("a")
        playNext("b") // c0 a b c1 c2 c3, latest = 2
        queue.removeAt(3)
        tracker.onItemRemoved(3) // c1 is after the latest: no shift
        assertThat(tracker.latest).isEqualTo(2)
        queue.removeAt(1)
        tracker.onItemRemoved(1) // "a" is before the latest
        assertThat(tracker.latest).isEqualTo(1)
        playNext("x")
        assertThat(queue).containsExactly("c0", "b", "x", "c2", "c3").inOrder()
    }

    @Test
    fun movingAnItemAcrossTheLatestShiftsIt() {
        playNext("a")
        playNext("b") // latest = 2
        // Move c0's neighbour "a" (index 1) behind the latest.
        queue.add(2, queue.removeAt(1))
        tracker.onItemMoved(1, 2)
        assertThat(tracker.latest).isEqualTo(1) // "b" slid back
        // The followed song itself moves.
        queue.add(4, queue.removeAt(1))
        tracker.onItemMoved(1, 4)
        assertThat(tracker.latest).isEqualTo(4)
    }

    @Test
    fun unreportedEditsAreCaughtByTheMediaIdCheck() {
        playNext("a")
        playNext("b") // c0 a b c1 c2 c3, latest = 2
        queue.add(1, "x") // slipped in before the latest, not reported
        playNext("y")
        assertThat(queue).containsExactly("c0", "x", "a", "b", "y", "c1", "c2", "c3").inOrder()
    }

    @Test
    fun aRemovedFollowedSongFallsBackToAfterTheCurrentOne() {
        playNext("a")
        queue.remove("a") // not reported
        playNext("b")
        assertThat(queue).containsExactly("c0", "b", "c1", "c2", "c3").inOrder()
    }

    @Test
    fun replacingTheQueueResets() {
        playNext("a")
        tracker.onQueueReplaced()
        assertThat(tracker.latest).isNull()
    }

    @Test
    fun aManualJumpResetsButSkippingToTheNextSongDoesNot() {
        playNext("a")
        playNext("b") // latest = 2
        current = 1
        tracker.onCurrentItemChanged(1, userInitiated = true) // next-step skip
        assertThat(tracker.latest).isEqualTo(2)
        tracker.onCurrentItemChanged(0, userInitiated = true) // back
        assertThat(tracker.latest).isNull()
    }

    @Test
    fun playbackPassingTheLatestResets() {
        playNext("a")
        playNext("b")
        tracker.onCurrentItemChanged(1, userInitiated = false)
        assertThat(tracker.latest).isEqualTo(2)
        tracker.onCurrentItemChanged(2, userInitiated = false)
        assertThat(tracker.latest).isNull()
    }
}
