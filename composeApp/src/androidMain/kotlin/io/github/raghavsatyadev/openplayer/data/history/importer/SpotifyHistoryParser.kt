package io.github.raghavsatyadev.openplayer.data.history.importer

import android.util.JsonReader
import android.util.JsonToken
import java.io.InputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Spotify streaming history, streamed from a JSON array so 10-20 MB files are never held whole.
 *
 * - Extended history (`Streaming_History_Audio_*.json`): `ts` (ISO-8601 UTC, the END of the play),
 *   `ms_played`, `master_metadata_track_name`, `master_metadata_album_artist_name`,
 *   `master_metadata_album_album_name`.
 * - Basic account data (`StreamingHistory*.json`): `endTime` ("yyyy-MM-dd HH:mm" UTC),
 *   `artistName`, `trackName`, `msPlayed`.
 *
 * Plays that lasted 0 ms and rows without a track name (podcasts) are skipped silently.
 */
object SpotifyHistoryParser : HistoryParser {

    private val basicTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private class Row {
        var ts: String? = null
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var msPlayed: Double? = null
    }

    override fun parse(input: InputStream, sink: ListenSink): Int {
        var unparseable = 0
        JsonReader(input.reader(Charsets.UTF_8)).use { reader ->
            JsonFields.forEachInArray(reader) {
                if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                    reader.skipValue()
                    unparseable++
                    return@forEachInArray
                }
                val row = readRow(reader)
                val title = row.title
                if (title.isNullOrBlank()) return@forEachInArray // podcast or removed track
                val played = row.msPlayed?.toLong() ?: 0L
                if (played <= 0L) return@forEachInArray
                val endMs = row.ts?.let { parseTime(it) }
                val artist = row.artist
                if (endMs == null || artist.isNullOrBlank()) {
                    unparseable++
                    return@forEachInArray
                }
                sink.accept(
                    ImportedListen(
                        title = title,
                        artist = artist,
                        album = row.album?.takeIf { it.isNotBlank() },
                        albumArtist = artist,
                        timestampMs = endMs,
                        timestampIsEnd = true,
                        msPlayed = played
                    )
                )
            }
        }
        return unparseable
    }

    private fun readRow(reader: JsonReader): Row {
        val row = Row()
        JsonFields.forEachField(reader) { name ->
            when (name) {
                "ts", "endTime" -> row.ts = JsonFields.nextText(reader)
                "ms_played", "msPlayed" -> row.msPlayed = JsonFields.nextDouble(reader)
                "master_metadata_track_name", "trackName" -> row.title = JsonFields.nextText(reader)
                "master_metadata_album_artist_name", "artistName" -> row.artist = JsonFields.nextText(reader)
                "master_metadata_album_album_name" -> row.album = JsonFields.nextText(reader)
                else -> reader.skipValue()
            }
        }
        return row
    }

    internal fun parseTime(text: String): Long? {
        val value = text.trim()
        runCatching { return Instant.parse(value).toEpochMilli() }
        runCatching { return LocalDateTime.parse(value, basicTime).toInstant(ZoneOffset.UTC).toEpochMilli() }
        return null
    }
}
