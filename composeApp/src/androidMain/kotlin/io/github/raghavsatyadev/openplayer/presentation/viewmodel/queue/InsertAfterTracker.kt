package io.github.raghavsatyadev.openplayer.presentation.viewmodel.queue

/**
 * Remembers where the last "Play next" song went, so the next one lands right after it and the
 * songs keep the order the user chose (instead of each jumping in front of the previous one).
 *
 * Pure bookkeeping on queue indices. Queue edits that are not reported here are caught at
 * insert time: the stored media id must still sit at the stored index, otherwise the tracker
 * looks for the id after the current song, and resets if it is gone.
 */
class InsertAfterTracker {

    private var latestIndex: Int = NONE
    private var latestMediaId: String? = null
    private var currentIndex: Int = NONE

    /** Queue index of the last inserted song, or null when there is none to follow. */
    val latest: Int? get() = latestIndex.takeIf { it != NONE }

    /**
     * Where the next "Play next" song should go. [idAt] reads the media id at a queue index.
     * With no current song the song goes to the end.
     */
    fun resolveInsertionIndex(currentIndex: Int, size: Int, idAt: (Int) -> String?): Int {
        if (currentIndex < 0) {
            reset()
            return size
        }
        this.currentIndex = currentIndex
        val afterCurrent = (currentIndex + 1).coerceAtMost(size)
        val index = latestIndex
        val id = latestMediaId
        if (index == NONE || id == null || index <= currentIndex) {
            reset()
            return afterCurrent
        }
        if (index < size && idAt(index) == id) return (index + 1).coerceAtMost(size)
        // The queue changed under us: find the same song again, nearest to where it was.
        val found = (currentIndex + 1 until size)
            .filter { idAt(it) == id }
            .minByOrNull { kotlin.math.abs(it - index) }
        if (found == null) {
            reset()
            return afterCurrent
        }
        latestIndex = found
        return (found + 1).coerceAtMost(size)
    }

    /** A "Play next" song was inserted at [index]. */
    fun recordInserted(index: Int, mediaId: String?) {
        latestIndex = index
        latestMediaId = mediaId
    }

    /** [count] songs were inserted at [at] by something other than "Play next". */
    fun onItemsInserted(at: Int, count: Int) {
        if (latestIndex != NONE && at <= latestIndex) latestIndex += count
        if (currentIndex != NONE && at <= currentIndex) currentIndex += count
    }

    fun onItemRemoved(index: Int) {
        when {
            latestIndex == NONE -> Unit
            index < latestIndex -> latestIndex -= 1
            // The followed song is gone; fall back to inserting after the current song.
            index == latestIndex -> reset()
        }
        if (currentIndex != NONE && index < currentIndex) currentIndex -= 1
    }

    fun onItemMoved(from: Int, to: Int) {
        latestIndex = shifted(latestIndex, from, to)
        if (currentIndex != NONE) currentIndex = shifted(currentIndex, from, to)
    }

    /** The queue was replaced (new list, shuffle rebuild). */
    fun onQueueReplaced() = reset()

    /**
     * The current song changed to [newIndex]. A jump that was not just the next song resets the
     * tracker (a manual skip back or into the queue), and so does reaching the followed song.
     */
    fun onCurrentItemChanged(newIndex: Int, userInitiated: Boolean) {
        val wasNextStep = currentIndex != NONE && newIndex == currentIndex + 1
        currentIndex = newIndex
        if (latestIndex == NONE) return
        if ((userInitiated && !wasNextStep) || newIndex >= latestIndex) reset()
    }

    fun reset() {
        latestIndex = NONE
        latestMediaId = null
    }

    private fun shifted(index: Int, from: Int, to: Int): Int = when {
        index == NONE -> NONE
        index == from -> to
        from < index && to >= index -> index - 1
        from > index && to <= index -> index + 1
        else -> index
    }

    private companion object {
        const val NONE = -1
    }
}
