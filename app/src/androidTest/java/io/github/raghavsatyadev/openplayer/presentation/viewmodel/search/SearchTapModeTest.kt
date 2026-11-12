package io.github.raghavsatyadev.openplayer.presentation.viewmodel.search

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.model.Song
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchTapModeTest {

    private fun song(id: String) = Song.emptySong().copy(id = id, title = "Song $id")

    private val a = song("a")
    private val b = song("b")
    private val c = song("c")
    private val results = listOf(a, b, c)

    private fun plan(mode: SearchTapMode, tapped: Song = b) =
        planSearchTap(mode, tapped, results, "Search: x")

    @Test
    fun defaultIsPlayAllResultsFromHere() {
        assertThat(SearchTapMode.DEFAULT).isEqualTo(SearchTapMode.PLAY_RESULTS)
        assertThat(SearchTapMode.fromKey(null)).isEqualTo(SearchTapMode.PLAY_RESULTS)
        assertThat(SearchTapMode.fromKey("garbage")).isEqualTo(SearchTapMode.PLAY_RESULTS)
    }

    @Test
    fun everyKeyRoundTrips() {
        SearchTapMode.entries.forEach { assertThat(SearchTapMode.fromKey(it.key)).isEqualTo(it) }
        assertThat(SearchTapMode.entries.map { it.key }.toSet()).hasSize(SearchTapMode.entries.size)
    }

    @Test
    fun playResultsQueuesEveryResultAndStartsAtTheTappedSong() {
        assertThat(plan(SearchTapMode.PLAY_RESULTS))
            .isEqualTo(SearchTapPlan.Play(results, b, "Search: x"))
    }

    @Test
    fun playResultsPlaysAloneWhenTheSongIsNotInTheList() {
        val other = song("z")
        assertThat(plan(SearchTapMode.PLAY_RESULTS, other))
            .isEqualTo(SearchTapPlan.Play(listOf(other), other, "Search: x"))
    }

    @Test
    fun playOnlyThisQueuesJustTheSong() {
        assertThat(plan(SearchTapMode.PLAY_ONLY_THIS)).isEqualTo(SearchTapPlan.Play(listOf(b), b, "Search: x"))
    }

    @Test
    fun theOtherModesMapToTheirActions() {
        assertThat(plan(SearchTapMode.PLAY_NEXT)).isEqualTo(SearchTapPlan.PlayNext(b))
        assertThat(plan(SearchTapMode.ADD_TO_QUEUE)).isEqualTo(SearchTapPlan.AddToQueue(b))
        assertThat(plan(SearchTapMode.PLAY_ALBUM)).isEqualTo(SearchTapPlan.PlayAlbumOf(b))
        assertThat(plan(SearchTapMode.PLAY_ARTIST)).isEqualTo(SearchTapPlan.PlayArtistOf(b))
    }

    @Test
    fun onlyPlaybackStartingPlansCloseTheSearch() {
        assertThat(plan(SearchTapMode.PLAY_RESULTS).startsPlayback).isTrue()
        assertThat(plan(SearchTapMode.PLAY_ALBUM).startsPlayback).isTrue()
        assertThat(plan(SearchTapMode.PLAY_NEXT).startsPlayback).isFalse()
        assertThat(plan(SearchTapMode.ADD_TO_QUEUE).startsPlayback).isFalse()
    }

    @Test
    fun executeCallsTheMatchingAction() {
        val calls = mutableListOf<String>()
        val actions = object : SearchTapActions {
            override fun playQueue(queue: List<Song>, start: Song, queueName: String) {
                calls += "play:${queue.size}:${start.id}"
            }
            override fun playNext(song: Song) { calls += "next:${song.id}" }
            override fun addToQueue(song: Song) { calls += "add:${song.id}" }
            override fun playAlbumOf(song: Song) { calls += "album:${song.id}" }
            override fun playArtistOf(song: Song) { calls += "artist:${song.id}" }
            override fun toast(messageRes: Int) { calls += "toast" }
        }
        SearchTapMode.entries.forEach { actions.execute(plan(it)) }
        assertThat(calls).containsExactly(
            "play:1:b", "play:3:b", "next:b", "toast", "add:b", "toast", "album:b", "artist:b",
        ).inOrder()
    }
}
