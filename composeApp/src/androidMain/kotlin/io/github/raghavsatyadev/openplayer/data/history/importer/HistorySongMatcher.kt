package io.github.raghavsatyadev.openplayer.data.history.importer

import io.github.raghavsatyadev.openplayer.data.database.SongSummary

/** Finds the library song an imported play belongs to. */
fun interface SongMatcher {
    fun match(listen: ImportedListen): String?
}

/**
 * Matches imported plays to library songs, from strictest to loosest:
 * 1. exact title + artist (trimmed, lowercase);
 * 2. folded title + folded artist, or any one artist of a multi-artist credit on either side
 *    (and the album artist, for Spotify);
 * 3. folded title alone, when exactly one library song has that title.
 *
 * Built once over the library, so a lookup is a few hash reads; results are cached per
 * title/artist because exports repeat the same tracks thousands of times.
 */
class HistorySongMatcher(library: List<SongSummary>) : SongMatcher {

    private class Entry(val id: String, val album: String)

    private val exact = HashMap<String, MutableList<Entry>>()
    private val folded = HashMap<String, MutableList<Entry>>()
    private val byTitle = HashMap<String, MutableSet<String>>()
    private val cache = HashMap<String, String?>()

    init {
        for (song in library) {
            val entry = Entry(song.id.toString(), MatchFolding.exact(song.albumName))
            exact.getOrPut(key(MatchFolding.exact(song.title), MatchFolding.exact(song.artistName))) { ArrayList(1) } += entry
            val title = MatchFolding.fold(song.title)
            val artists = listOf(MatchFolding.fold(song.artistName)) + MatchFolding.splitArtists(song.artistName)
            for (artist in artists.distinct()) folded.getOrPut(key(title, artist)) { ArrayList(1) } += entry
            byTitle.getOrPut(title) { HashSet(2) } += entry.id
        }
    }

    override fun match(listen: ImportedListen): String? {
        val cacheKey = "${listen.title}\u0000${listen.artist}\u0000${listen.album.orEmpty()}\u0000${listen.albumArtist.orEmpty()}"
        if (cache.containsKey(cacheKey)) return cache[cacheKey]
        return find(listen).also { cache[cacheKey] = it }
    }

    private fun find(listen: ImportedListen): String? {
        val album = MatchFolding.exact(listen.album.orEmpty())
        exact[key(MatchFolding.exact(listen.title), MatchFolding.exact(listen.artist))]
            ?.let { return pick(it, album) }

        val title = MatchFolding.fold(listen.title)
        val credits = listOfNotNull(listen.artist, listen.albumArtist?.takeIf { it.isNotBlank() })
        val artists = LinkedHashSet<String>()
        for (credit in credits) {
            artists += MatchFolding.fold(credit)
            artists += MatchFolding.splitArtists(credit)
        }
        for (artist in artists) {
            folded[key(title, artist)]?.let { return pick(it, album) }
        }

        return byTitle[title]?.singleOrNull()
    }

    /** One song of [candidates]: the one on [album] when there are several, else the first. */
    private fun pick(candidates: List<Entry>, album: String): String =
        (candidates.firstOrNull { album.isNotEmpty() && it.album == album } ?: candidates.first()).id

    private fun key(title: String, artist: String) = "$title\u0000$artist"
}
