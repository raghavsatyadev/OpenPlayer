package io.github.raghavsatyadev.openplayer.data.genre

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class BroadGenreTest {

    @Test
    fun onlineStoreNames_mapToBroadNames() {
        assertThat(BroadGenre.of("Hip-Hop/Rap")).isEqualTo("Hip-Hop")
        assertThat(BroadGenre.of("R&B/Soul")).isEqualTo("R&B")
        assertThat(BroadGenre.of("Singer/Songwriter")).isEqualTo("Folk")
        assertThat(BroadGenre.of("Indian Pop")).isEqualTo("Bollywood")
        assertThat(BroadGenre.of("Dance")).isEqualTo("Electronic")
        assertThat(BroadGenre.of("Soundtrack")).isEqualTo("Soundtrack")
    }

    @Test
    fun artistTableLabels_mapToBroadNames() {
        assertThat(BroadGenre.of("southern hip hop")).isEqualTo("Hip-Hop")
        assertThat(BroadGenre.of("nu metal")).isEqualTo("Metal")
        assertThat(BroadGenre.of("dance-punk")).isEqualTo("Punk")
        assertThat(BroadGenre.of("dance pop")).isEqualTo("Pop")
        assertThat(BroadGenre.of("alternative rock")).isEqualTo("Alternative")
        assertThat(BroadGenre.of("dubstep")).isEqualTo("Electronic")
    }

    @Test
    fun unknownLabels_areKeptTitleCased_andBlankIsNull() {
        assertThat(BroadGenre.of("comedy")).isEqualTo("Comedy")
        assertThat(BroadGenre.of("  ")).isNull()
        assertThat(BroadGenre.of(null)).isNull()
    }
}
