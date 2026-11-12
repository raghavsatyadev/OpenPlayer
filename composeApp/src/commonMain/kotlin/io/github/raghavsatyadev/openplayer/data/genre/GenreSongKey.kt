package io.github.raghavsatyadev.openplayer.data.genre

import io.github.raghavsatyadev.openplayer.data.database.GenreFillCandidateRow
import io.github.raghavsatyadev.openplayer.data.history.importer.MatchFolding
import io.github.raghavsatyadev.openplayer.data.image.online.CoverQuery
import io.github.raghavsatyadev.openplayer.data.playlist.nlp.IndexedSong

/**
 * What the genre fill knows about one song: the cleaned search query, the key its online result
 * is cached under, and the artist keys to try in the offline table, most specific first.
 */
data class GenreSongKey(
    val songId: Long,
    val query: CoverQuery,
    val lookupKey: String,
    val artistKeys: List<String>,
) {
    companion object {
        private val ARTIST_SEPARATORS =
            Regex("""\s*(?:,|;|/|&|\bfeat\b\.?|\bft\b\.?|\bfeaturing\b|\bx\b|\band\b)\s*""", RegexOption.IGNORE_CASE)
        private val UNKNOWN_ALBUMS = setOf("", "unknown", "unknown album", "<unknown>")

        /** Null when the song has no usable artist or title (nothing to look up). */
        fun of(row: GenreFillCandidateRow): GenreSongKey? {
            val query = CoverQuery.from(row.title, row.artistName, row.albumName, row.duration) ?: return null
            return GenreSongKey(row.id, query, lookupKey(query), artistKeys(query.artist))
        }

        /**
         * Songs of one album share a lookup ("artist|album"); a song with no real album is looked
         * up on its own ("artist||title").
         */
        fun lookupKey(query: CoverQuery): String {
            val artist = MatchFolding.fold(primaryArtist(query.artist))
            val album = query.album?.let { MatchFolding.fold(it) }.orEmpty()
            return if (album in UNKNOWN_ALBUMS) "$artist||${MatchFolding.fold(query.title)}" else "$artist|$album"
        }

        /** The whole credit first ("Simon & Garfunkel"), then each credited artist in order. */
        fun artistKeys(credit: String): List<String> {
            val parts = credit.split(ARTIST_SEPARATORS)
            return (listOf(credit) + parts)
                .map { IndexedSong.metadataKey(it) }
                .filter { it.isNotEmpty() }
                .distinct()
        }

        private fun primaryArtist(credit: String): String =
            credit.split(ARTIST_SEPARATORS).firstOrNull { it.isNotBlank() } ?: credit
    }
}
