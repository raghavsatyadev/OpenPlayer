package io.github.raghavsatyadev.openplayer.presentation.viewmodel.search

import androidx.annotation.StringRes
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.model.Song

/** What tapping a song in the search results does. [key] is what the preference stores. */
enum class SearchTapMode(val key: String, @StringRes val labelRes: Int) {
    PLAY_ONLY_THIS("play_only_this", R.string.search_tap_play_only_this),
    PLAY_RESULTS("play_results", R.string.search_tap_play_results),
    PLAY_NEXT("play_next", R.string.search_tap_play_next),
    ADD_TO_QUEUE("add_to_queue", R.string.search_tap_add_to_queue),
    PLAY_ALBUM("play_album", R.string.search_tap_play_album),
    PLAY_ARTIST("play_artist", R.string.search_tap_play_artist);

    companion object {
        val DEFAULT = PLAY_RESULTS

        fun fromKey(key: String?): SearchTapMode = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/** The action a tap turns into; [execute] carries it out through [SearchTapActions]. */
sealed interface SearchTapPlan {
    /** Replace the queue with [queue] and start at [start]. */
    data class Play(val queue: List<Song>, val start: Song, val queueName: String) : SearchTapPlan
    data class PlayNext(val song: Song) : SearchTapPlan
    data class AddToQueue(val song: Song) : SearchTapPlan
    data class PlayAlbumOf(val song: Song) : SearchTapPlan
    data class PlayArtistOf(val song: Song) : SearchTapPlan

    /** Whether the search screen should close afterwards: only when playback was started. */
    val startsPlayback: Boolean
        get() = this is Play || this is PlayAlbumOf || this is PlayArtistOf
}

/** Pure mapping from a tap to its plan. [results] are the song results currently listed. */
fun planSearchTap(
    mode: SearchTapMode,
    song: Song,
    results: List<Song>,
    queueName: String,
): SearchTapPlan = when (mode) {
    SearchTapMode.PLAY_ONLY_THIS -> SearchTapPlan.Play(listOf(song), song, queueName)
    SearchTapMode.PLAY_RESULTS -> SearchTapPlan.Play(
        // A song outside the list (e.g. from another section) plays alone.
        queue = if (results.any { it.id == song.id }) results else listOf(song),
        start = song,
        queueName = queueName,
    )
    SearchTapMode.PLAY_NEXT -> SearchTapPlan.PlayNext(song)
    SearchTapMode.ADD_TO_QUEUE -> SearchTapPlan.AddToQueue(song)
    SearchTapMode.PLAY_ALBUM -> SearchTapPlan.PlayAlbumOf(song)
    SearchTapMode.PLAY_ARTIST -> SearchTapPlan.PlayArtistOf(song)
}

/** The player operations a [SearchTapPlan] needs; the search screen wires them to PlayerViewModel. */
interface SearchTapActions {
    fun playQueue(queue: List<Song>, start: Song, queueName: String)
    fun playNext(song: Song)
    fun addToQueue(song: Song)
    fun playAlbumOf(song: Song)
    fun playArtistOf(song: Song)
    fun toast(@StringRes messageRes: Int)
}

fun SearchTapActions.execute(plan: SearchTapPlan) {
    when (plan) {
        is SearchTapPlan.Play -> playQueue(plan.queue, plan.start, plan.queueName)
        is SearchTapPlan.PlayNext -> {
            playNext(plan.song)
            toast(R.string.search_tap_toast_play_next)
        }
        is SearchTapPlan.AddToQueue -> {
            addToQueue(plan.song)
            toast(R.string.search_tap_toast_added)
        }
        is SearchTapPlan.PlayAlbumOf -> playAlbumOf(plan.song)
        is SearchTapPlan.PlayArtistOf -> playArtistOf(plan.song)
    }
}

/** Wires a [SearchTapPlan] to this view model; [text] resolves a string resource for toasts. */
fun io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerViewModel.searchTapActions(
    text: (Int) -> String,
): SearchTapActions = object : SearchTapActions {
    override fun playQueue(queue: List<Song>, start: Song, queueName: String) =
        showAndPlaySong(start, queue, queueName)

    override fun playNext(song: Song) = addSongNextToQueue(song)
    override fun addToQueue(song: Song) = addSongToQueue(song)
    override fun playAlbumOf(song: Song) = this@searchTapActions.playAlbumOf(song)
    override fun playArtistOf(song: Song) = this@searchTapActions.playArtistOf(song)
    override fun toast(messageRes: Int) = sendToast(text(messageRes))
}
