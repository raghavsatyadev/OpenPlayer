package io.github.raghavsatyadev.openplayer.data.image.online

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CoverQueryTest {

    @Test
    fun from_cleansOfficialVideoAndTopic() {
        val query = CoverQuery.from(
            title = "Blinding Lights (Official Video)",
            artist = "The Weeknd - Topic",
            album = "After Hours",
            durationMs = 200_000L
        )
        assertThat(query).isNotNull()
        assertThat(query?.title).isEqualTo("Blinding Lights")
        assertThat(query?.artist).isEqualTo("The Weeknd")
        assertThat(query?.album).isEqualTo("After Hours")
        assertThat(query?.durationMs).isEqualTo(200_000L)
    }

    @Test
    fun from_splitsUnknownArtistWithDashedTitle() {
        val queryWithAngleBrackets = CoverQuery.from(
            title = "The Weeknd - Blinding Lights",
            artist = "<unknown>",
            album = null,
            durationMs = 200_000L
        )
        assertThat(queryWithAngleBrackets).isNotNull()
        assertThat(queryWithAngleBrackets?.title).isEqualTo("Blinding Lights")
        assertThat(queryWithAngleBrackets?.artist).isEqualTo("The Weeknd")

        val queryWithUnknownArtist = CoverQuery.from(
            title = "The Weeknd - Blinding Lights",
            artist = "Unknown Artist",
            album = null,
            durationMs = 200_000L
        )
        assertThat(queryWithUnknownArtist).isNotNull()
        assertThat(queryWithUnknownArtist?.title).isEqualTo("Blinding Lights")
        assertThat(queryWithUnknownArtist?.artist).isEqualTo("The Weeknd")
    }

    @Test
    fun from_returnsNullWhenUnknownArtistAndNoDash() {
        val query = CoverQuery.from(
            title = "Blinding Lights",
            artist = "<unknown>",
            album = null,
            durationMs = 200_000L
        )
        assertThat(query).isNull()

        val queryUnknownArtist = CoverQuery.from(
            title = "Blinding Lights",
            artist = "Unknown Artist",
            album = null,
            durationMs = 200_000L
        )
        assertThat(queryUnknownArtist).isNull()
    }

    @Test
    fun coverMatch_acceptsEqualFoldedTitleAndArtist() {
        val query = CoverQuery(
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = null,
            durationMs = 200_000L
        )
        val accepted = CoverMatch.accepts(
            query = query,
            title = "blinding lights",
            artist = "the weeknd",
            durationMs = 200_000L
        )
        assertThat(accepted).isTrue()
    }

    @Test
    fun coverMatch_acceptsMultiArtistMatch() {
        val queryMulti = CoverQuery(
            title = "Song Title",
            artist = "Artist A & Artist B",
            album = null,
            durationMs = 180_000L
        )
        assertThat(
            CoverMatch.accepts(
                query = queryMulti,
                title = "Song Title",
                artist = "Artist A",
                durationMs = 180_000L
            )
        ).isTrue()

        val querySingle = CoverQuery(
            title = "Song Title",
            artist = "Artist A",
            album = null,
            durationMs = 180_000L
        )
        assertThat(
            CoverMatch.accepts(
                query = querySingle,
                title = "Song Title",
                artist = "Artist A & Artist B",
                durationMs = 180_000L
            )
        ).isTrue()
    }

    @Test
    fun coverMatch_rejectsDifferentTitle() {
        val query = CoverQuery(
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = null,
            durationMs = 200_000L
        )
        assertThat(
            CoverMatch.accepts(
                query = query,
                title = "Save Your Tears",
                artist = "The Weeknd",
                durationMs = 200_000L
            )
        ).isFalse()
    }

    @Test
    fun coverMatch_rejectsDurationOffByThirtySeconds() {
        val query = CoverQuery(
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = null,
            durationMs = 200_000L
        )
        assertThat(
            CoverMatch.accepts(
                query = query,
                title = "Blinding Lights",
                artist = "The Weeknd",
                durationMs = 230_000L
            )
        ).isFalse()
    }

    @Test
    fun coverMatch_acceptsWhenDurationIsZero() {
        val queryWithZero = CoverQuery(
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = null,
            durationMs = 0L
        )
        assertThat(
            CoverMatch.accepts(
                query = queryWithZero,
                title = "Blinding Lights",
                artist = "The Weeknd",
                durationMs = 200_000L
            )
        ).isTrue()

        val queryWithDuration = CoverQuery(
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = null,
            durationMs = 200_000L
        )
        assertThat(
            CoverMatch.accepts(
                query = queryWithDuration,
                title = "Blinding Lights",
                artist = "The Weeknd",
                durationMs = 0L
            )
        ).isTrue()

        assertThat(
            CoverMatch.accepts(
                query = queryWithDuration,
                title = "Blinding Lights",
                artist = "The Weeknd",
                durationMs = null
            )
        ).isTrue()
    }
}
