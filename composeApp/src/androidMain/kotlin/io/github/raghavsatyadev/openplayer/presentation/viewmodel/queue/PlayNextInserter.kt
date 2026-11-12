package io.github.raghavsatyadev.openplayer.presentation.viewmodel.queue

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player

/**
 * "Play next" placement. With [enabled] (the "insert after latest" setting) each song goes after
 * the previously inserted one; otherwise every song goes right after the current one, as before.
 */
class PlayNextInserter(private val tracker: InsertAfterTracker = InsertAfterTracker()) {

    var enabled: Boolean = true
        set(value) {
            if (field != value) tracker.reset()
            field = value
        }

    /** Adds [item] as the next song of [player] and remembers where it went. */
    fun insert(player: Player, item: MediaItem) {
        val count = player.mediaItemCount
        val current = player.currentMediaItemIndex
        val index = if (enabled) {
            tracker.resolveInsertionIndex(
                currentIndex = if (current == C.INDEX_UNSET) -1 else current,
                size = count,
                idAt = { player.getMediaItemAt(it).mediaId },
            )
        } else if (current != C.INDEX_UNSET) {
            (current + 1).coerceAtMost(count)
        } else {
            count
        }
        player.addMediaItem(index, item)
        if (enabled) tracker.recordInserted(index, item.mediaId)
    }

    /** Songs to feed to [insert] one by one so that they end up in their given order. */
    fun <T> inInsertOrder(songs: List<T>): List<T> = if (enabled) songs else songs.asReversed()

    fun onCurrentItemChanged(newIndex: Int, reason: Int) {
        val natural = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
            reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
        tracker.onCurrentItemChanged(newIndex, userInitiated = !natural)
    }

    fun onItemRemoved(index: Int) = tracker.onItemRemoved(index)

    fun onItemMoved(from: Int, to: Int) = tracker.onItemMoved(from, to)

    fun onQueueReplaced() = tracker.onQueueReplaced()
}
