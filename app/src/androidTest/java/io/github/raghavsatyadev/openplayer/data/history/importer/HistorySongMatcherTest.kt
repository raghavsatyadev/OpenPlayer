package io.github.raghavsatyadev.openplayer.data.history.importer

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.database.SongSummary
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistorySongMatcherTest {

    private fun song(id: Long, title: String, artist: String, album: String = "Album") =
        SongSummary(id = id, title = title, artistName = artist, albumName = album, duration = 200_000L)

    private fun play(title: String, artist: String, album: String? = null, albumArtist: String? = null) =
        ImportedListen(title = title, artist = artist, album = album, albumArtist = albumArtist, timestampMs = 1L)

    private val matcher = HistorySongMatcher(
        listOf(
            song(1, "Exact Song", "The Artist"),
            song(2, "Cafe Del Mar", "Energy 52"),
            song(3, "Collab Track", "Main Act & Guest"),
            song(4, "Unique Title Only", "Local Band"),
            song(5, "Common Name", "Band A"),
            song(6, "Common Name", "Band B"),
            song(7, "Remastered Hit", "Old Group")
        )
    )

    @Test
    fun exactMatchIgnoresCaseAndSpaces() {
        assertThat(matcher.match(play("  exact SONG ", "the artist"))).isEqualTo("1")
    }

    @Test
    fun featuringBracketsAndRemasterSuffixesAreFolded() {
        assertThat(matcher.match(play("Exact Song (feat. Someone)", "The Artist"))).isEqualTo("1")
        assertThat(matcher.match(play("Exact Song [Live at Wembley]", "The Artist"))).isEqualTo("1")
        assertThat(matcher.match(play("Remastered Hit - 2011 Remastered Version", "Old Group"))).isEqualTo("7")
        assertThat(matcher.match(play("Exact Song", "The Artist feat. Guest"))).isEqualTo("1")
    }

    @Test
    fun accentsAndPunctuationAreFolded() {
        assertThat(matcher.match(play("Café del Mar!", "Energy 52"))).isEqualTo("2")
    }

    @Test
    fun anyOneArtistOfACreditMatches() {
        assertThat(matcher.match(play("Collab Track", "Main Act"))).isEqualTo("3")
        assertThat(matcher.match(play("Collab Track", "Guest"))).isEqualTo("3")
        assertThat(matcher.match(play("Collab Track", "Someone Else", albumArtist = "Main Act"))).isEqualTo("3")
    }

    @Test
    fun titleOnlyMatchesWhenTheTitleIsUnique() {
        assertThat(matcher.match(play("Unique Title Only", "Different Spelling Entirely"))).isEqualTo("4")
    }

    @Test
    fun ambiguousTitleIsNotMatched() {
        assertThat(matcher.match(play("Common Name", "Band C"))).isNull()
        // With an artist that points at one of them it is fine.
        assertThat(matcher.match(play("Common Name", "band b"))).isEqualTo("6")
    }

    @Test
    fun unknownTrackIsNotMatched() {
        assertThat(matcher.match(play("Not In Library", "Nobody"))).isNull()
    }
}
