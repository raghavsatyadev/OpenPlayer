package io.github.raghavsatyadev.openplayer.data.text

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class YouTubeTitlesTest {

    @Test
    fun cleanChannelDropsTopicVevoAndOfficialSuffixes() {
        assertThat(YouTubeTitles.cleanChannel("The Weeknd - Topic")).isEqualTo("The Weeknd")
        assertThat(YouTubeTitles.cleanChannel("TheWeekndVEVO")).isEqualTo("TheWeeknd")
        assertThat(YouTubeTitles.cleanChannel("Adele VEVO")).isEqualTo("Adele")
        assertThat(YouTubeTitles.cleanChannel("Artist Official")).isEqualTo("Artist")
        assertThat(YouTubeTitles.cleanChannel("Artist Official Channel")).isEqualTo("Artist")
        assertThat(YouTubeTitles.cleanChannel("  Artist - topic  ")).isEqualTo("Artist")
    }

    @Test
    fun cleanTitleDropsVideoNoiseKeepsLiveAndPreservesNonEmpty() {
        assertThat(YouTubeTitles.cleanTitle("Blinding Lights (Official Video)")).isEqualTo("Blinding Lights")
        assertThat(YouTubeTitles.cleanTitle("Song [Official Audio]")).isEqualTo("Song")
        assertThat(YouTubeTitles.cleanTitle("Song (Lyrics)")).isEqualTo("Song")
        assertThat(YouTubeTitles.cleanTitle("Song (Official Lyric Video) | Album")).isEqualTo("Song")
        assertThat(YouTubeTitles.cleanTitle("Song (Live)")).isEqualTo("Song (Live)")
        assertThat(YouTubeTitles.cleanTitle("(Official Video)")).isEqualTo("(Official Video)")
        assertThat(YouTubeTitles.cleanTitle("Song Official Video")).isEqualTo("Song")
        assertThat(YouTubeTitles.cleanTitle("Song [4K]")).isEqualTo("Song")
        assertThat(YouTubeTitles.cleanTitle("Song (MV)")).isEqualTo("Song")
        assertThat(YouTubeTitles.cleanTitle("Song [M/V]")).isEqualTo("Song")
        assertThat(YouTubeTitles.cleanTitle("Song (Visualizer)")).isEqualTo("Song")
        assertThat(YouTubeTitles.cleanTitle("Song (Explicit)")).isEqualTo("Song")
    }

    @Test
    fun splitArtistTitleSplitsWhenChannelMatchesOrBlank() {
        val vevoMatch = YouTubeTitles.splitArtistTitle(
            title = "The Weeknd - Blinding Lights",
            channel = "TheWeekndVEVO"
        )
        assertThat(vevoMatch).isEqualTo(Pair("The Weeknd", "Blinding Lights"))

        val noMatch = YouTubeTitles.splitArtistTitle(
            title = "Part 1 - Intro",
            channel = "Some Label"
        )
        assertThat(noMatch).isNull()

        val blankChannel = YouTubeTitles.splitArtistTitle(
            title = "The Weeknd - Blinding Lights",
            channel = ""
        )
        assertThat(blankChannel).isEqualTo(Pair("The Weeknd", "Blinding Lights"))

        val nullChannel = YouTubeTitles.splitArtistTitle(
            title = "The Weeknd - Blinding Lights",
            channel = null
        )
        assertThat(nullChannel).isEqualTo(Pair("The Weeknd", "Blinding Lights"))

        val enDash = YouTubeTitles.splitArtistTitle(
            title = "The Weeknd \u2013 Blinding Lights",
            channel = "TheWeekndVEVO"
        )
        assertThat(enDash).isEqualTo(Pair("The Weeknd", "Blinding Lights"))

        val emDash = YouTubeTitles.splitArtistTitle(
            title = "The Weeknd \u2014 Blinding Lights",
            channel = "TheWeekndVEVO"
        )
        assertThat(emDash).isEqualTo(Pair("The Weeknd", "Blinding Lights"))
    }
}
