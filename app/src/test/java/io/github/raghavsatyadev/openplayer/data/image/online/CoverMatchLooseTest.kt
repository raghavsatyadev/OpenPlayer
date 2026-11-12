package io.github.raghavsatyadev.openplayer.data.image.online

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CoverMatchLooseTest {

    private val videoRip = CoverQuery("Heathens", "twenty one pilots", null, 217_896L)
    private fun fields(c: Triple<String, String, Long?>) = c

    @Test
    fun loose_acceptsLongerVideoRip() {
        assertThat(CoverMatch.accepts(videoRip, "Heathens", "twenty one pilots", 195_920L)).isFalse()
        assertThat(CoverMatch.accepts(videoRip, "Heathens", "twenty one pilots", 195_920L, loose = true)).isTrue()
    }

    @Test
    fun loose_rejectsShorterLocalFile() {
        val short = CoverQuery("Heathens", "twenty one pilots", null, 150_000L)
        assertThat(CoverMatch.accepts(short, "Heathens", "twenty one pilots", 195_920L, loose = true)).isFalse()
    }

    @Test
    fun loose_rejectsFarLongerFile() {
        val long = CoverQuery("Heathens", "twenty one pilots", null, 400_000L)
        assertThat(CoverMatch.accepts(long, "Heathens", "twenty one pilots", 195_920L, loose = true)).isFalse()
    }

    @Test
    fun best_prefersStrictThenClosestLoose() {
        val live = Triple<String, String, Long?>("Heathens", "twenty one pilots", 160_000L)
        val album = Triple<String, String, Long?>("Heathens", "twenty one pilots", 195_920L)
        assertThat(CoverMatch.best(videoRip, listOf(live, album), ::fields)).isEqualTo(album)

        val exact = Triple<String, String, Long?>("Heathens", "twenty one pilots", 215_000L)
        assertThat(CoverMatch.best(videoRip, listOf(album, exact), ::fields)).isEqualTo(exact)

        val other = Triple<String, String, Long?>("Ride", "twenty one pilots", 215_000L)
        assertThat(CoverMatch.best(videoRip, listOf(other), ::fields)).isNull()
    }

    @Test
    fun from_dropsRepeatedArtistPrefix() {
        val query = CoverQuery.from("Ariana Grande - Baby I", "Ariana Grande", null, 200_000L)
        assertThat(query?.title).isEqualTo("Baby I")
    }
}
