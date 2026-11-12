package io.github.raghavsatyadev.openplayer.data.genre

import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.database.GenreFillCandidateRow
import org.junit.jupiter.api.Test

class GenreSongKeyTest {

    private fun row(title: String, artist: String, album: String) =
        GenreFillCandidateRow(id = 1, title = title, artistName = artist, albumName = album, duration = 200_000, genre = null)

    @Test
    fun songsOfOneAlbum_shareALookupKey() {
        val a = GenreSongKey.of(row("Song A", "Lil Wayne, Charlie Puth", "Tha Carter"))!!
        val b = GenreSongKey.of(row("Song B", "Lil Wayne", "Tha Carter"))!!
        assertThat(a.lookupKey).isEqualTo(b.lookupKey)
        assertThat(a.lookupKey).isEqualTo("lil wayne|tha carter")
    }

    @Test
    fun songWithoutAlbum_isLookedUpByTitle() {
        val key = GenreSongKey.of(row("Some Song (Official Video)", "Artist - Topic", "<unknown>"))!!
        assertThat(key.lookupKey).isEqualTo("artist||some song")
    }

    @Test
    fun artistKeys_tryWholeCreditThenEachArtist() {
        assertThat(GenreSongKey.artistKeys("Lil Wayne, Charlie Puth"))
            .containsExactly("lil wayne, charlie puth", "lil wayne", "charlie puth").inOrder()
        assertThat(GenreSongKey.artistKeys("Simon & Garfunkel"))
            .containsExactly("simon & garfunkel", "simon", "garfunkel").inOrder()
    }

    @Test
    fun unknownArtistAndTitle_hasNoKey() {
        assertThat(GenreSongKey.of(row("<unknown>", "<unknown>", ""))).isNull()
    }
}
