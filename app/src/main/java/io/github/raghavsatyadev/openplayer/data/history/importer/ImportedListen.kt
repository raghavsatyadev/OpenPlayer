package io.github.raghavsatyadev.openplayer.data.history.importer

import io.github.raghavsatyadev.openplayer.data.database.ListenSource

/** The services whose listening history can be imported. */
enum class HistorySource(val listenSource: Int) {
    LASTFM(ListenSource.IMPORTED_LASTFM),
    SPOTIFY(ListenSource.IMPORTED_SPOTIFY),
    LISTENBRAINZ(ListenSource.IMPORTED_LISTENBRAINZ),
    YOUTUBE_MUSIC(ListenSource.IMPORTED_YOUTUBE_MUSIC)
}

/**
 * One play read from an export, before it is matched to a library song.
 *
 * [timestampMs] is the start of the play, or its end when [timestampIsEnd] (Spotify).
 * [msPlayed] is how long it was heard, when the export says.
 */
data class ImportedListen(
    val title: String,
    val artist: String,
    val album: String? = null,
    val albumArtist: String? = null,
    val timestampMs: Long,
    val timestampIsEnd: Boolean = false,
    val msPlayed: Long? = null
)

/** Receives plays as a parser reads them. */
fun interface ListenSink {
    fun accept(listen: ImportedListen)
}

/**
 * A reader for one export format. [parse] streams the plays to [sink] and returns how many
 * entries it could not read (plays without a usable time, title or artist).
 */
interface HistoryParser {
    fun parse(input: java.io.InputStream, sink: ListenSink): Int
}

/** Collects what a parser reads; mainly for tests. */
class ParsedListens(val listens: List<ImportedListen>, val unparseable: Int)

fun HistoryParser.parseAll(input: java.io.InputStream): ParsedListens {
    val list = ArrayList<ImportedListen>()
    val bad = parse(input) { list += it }
    return ParsedListens(list, bad)
}
