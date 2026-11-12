package io.github.raghavsatyadev.openplayer.data.image.online

import io.github.raghavsatyadev.openplayer.data.history.importer.MatchFolding
import io.github.raghavsatyadev.openplayer.data.text.YouTubeTitles
import kotlin.math.abs

/**
 * Normalized query parameters for online album art lookups.
 */
data class CoverQuery(
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long
) {
    companion object {
        /**
         * Builds a cleaned [CoverQuery] from raw metadata, stripping noise tokens and splitting
         * "Artist - Title" strings when artist is missing. Returns null if title or artist is blank/unknown.
         */
        fun from(
            title: String,
            artist: String,
            album: String?,
            durationMs: Long
        ): CoverQuery? {
            var cleanedTitle = YouTubeTitles.cleanTitle(title)
            var cleanedArtist = YouTubeTitles.cleanChannel(artist)

            if (isUnknownArtist(cleanedArtist)) {
                val split = YouTubeTitles.splitArtistTitle(cleanedTitle, null)
                if (split != null) {
                    cleanedArtist = YouTubeTitles.cleanChannel(split.first)
                    cleanedTitle = YouTubeTitles.cleanTitle(split.second)
                }
            }

            // "Ariana Grande - Baby I" by Ariana Grande: drop the repeated artist.
            val artistPrefix = "$cleanedArtist - "
            if (cleanedTitle.length > artistPrefix.length && cleanedTitle.startsWith(artistPrefix, ignoreCase = true)) {
                cleanedTitle = cleanedTitle.substring(artistPrefix.length).trim()
            }

            if (isUnknownArtist(cleanedArtist) || isBlankOrUnknown(cleanedTitle)) {
                return null
            }

            return CoverQuery(
                title = cleanedTitle,
                artist = cleanedArtist,
                album = album?.trim()?.ifEmpty { null },
                durationMs = durationMs
            )
        }

        private fun isUnknownArtist(artist: String): Boolean {
            val trimmed = artist.trim()
            return trimmed.isEmpty() ||
                trimmed.equals("<unknown>", ignoreCase = true) ||
                trimmed.equals("Unknown Artist", ignoreCase = true)
        }

        private fun isBlankOrUnknown(title: String): Boolean {
            val trimmed = title.trim()
            return trimmed.isEmpty() ||
                trimmed.equals("<unknown>", ignoreCase = true) ||
                trimmed.equals("Unknown", ignoreCase = true)
        }
    }
}

/**
 * Verifies whether an online search result matches a given [CoverQuery].
 */
object CoverMatch {
    /**
     * Checks if search result title, artist, and duration match [query]. With [loose], a local
     * file up to [LOOSE_LONGER_RATIO] longer than the result still matches: music-video rips
     * carry intros and outros the album track does not.
     */
    fun accepts(
        query: CoverQuery,
        title: String,
        artist: String,
        durationMs: Long?,
        loose: Boolean = false
    ): Boolean {
        if (MatchFolding.fold(query.title) != MatchFolding.fold(title)) return false
        if (!artistMatches(query.artist, artist)) return false

        val rDur = durationMs ?: 0L
        if (query.durationMs <= 0L || rDur <= 0L) return true
        val diff = abs(query.durationMs - rDur)
        if (diff <= STRICT_TOLERANCE_MS) return true
        return loose && query.durationMs > rDur && diff <= rDur * LOOSE_LONGER_RATIO
    }

    /**
     * The best of [candidates] for [query]: the first strict match, else the loose match whose
     * length is closest. [fields] gives each candidate's title, artist and duration.
     */
    fun <T> best(
        query: CoverQuery,
        candidates: List<T>,
        fields: (T) -> Triple<String, String, Long?>
    ): T? {
        candidates.firstOrNull { c ->
            val (t, a, d) = fields(c)
            accepts(query, t, a, d)
        }?.let { return it }
        return candidates
            .filter { c ->
                val (t, a, d) = fields(c)
                accepts(query, t, a, d, loose = true)
            }
            .minByOrNull { c -> abs(query.durationMs - (fields(c).third ?: 0L)) }
    }

    private fun artistMatches(queryArtist: String, artist: String): Boolean {
        val qArtistFolded = MatchFolding.fold(queryArtist)
        val rArtistFolded = MatchFolding.fold(artist)
        if (qArtistFolded == rArtistFolded) return true
        val qParts = MatchFolding.splitArtists(queryArtist)
        val rParts = MatchFolding.splitArtists(artist)
        return qParts.contains(rArtistFolded) ||
            rParts.contains(qArtistFolded) ||
            (qParts.isNotEmpty() && rParts.isNotEmpty() && qParts.any { it in rParts })
    }

    private const val STRICT_TOLERANCE_MS = 15_000L
    private const val LOOSE_LONGER_RATIO = 0.5
}
