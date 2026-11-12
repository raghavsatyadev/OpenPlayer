package io.github.raghavsatyadev.openplayer.data.repository

import io.github.raghavsatyadev.openplayer.data.model.Song

/**
 * What playing a [SavedQueue] again would do: its songs that still exist, in order, where to
 * start in them and how far into that song. [missingCount] songs were gone and are skipped.
 */
data class SavedQueueRestorePlan(
    val songs: List<Song>,
    val startIndex: Int,
    val positionMs: Long,
    val missingCount: Int
) {
    val isEmpty: Boolean get() = songs.isEmpty()

    companion object {
        /**
         * Looks each saved id up in [available] (repeats in the queue are kept). If the song that
         * was playing is gone, playback starts at the song now in its place and from the beginning,
         * since the saved position belonged to the missing song.
         */
        fun build(queue: SavedQueue, available: Map<String, Song>): SavedQueueRestorePlan {
            val songs = ArrayList<Song>(queue.songIds.size)
            var keptBeforeCurrent = 0
            var currentKept = false
            queue.songIds.forEachIndexed { index, id ->
                val song = available[id]
                if (song != null) {
                    if (index < queue.currentIndex) keptBeforeCurrent++
                    if (index == queue.currentIndex) currentKept = true
                    songs += song
                }
            }
            if (songs.isEmpty()) return SavedQueueRestorePlan(emptyList(), 0, 0L, queue.songIds.size)
            return SavedQueueRestorePlan(
                songs = songs,
                startIndex = keptBeforeCurrent.coerceIn(0, songs.lastIndex),
                positionMs = if (currentKept) queue.positionMs else 0L,
                missingCount = queue.songIds.size - songs.size
            )
        }
    }
}
