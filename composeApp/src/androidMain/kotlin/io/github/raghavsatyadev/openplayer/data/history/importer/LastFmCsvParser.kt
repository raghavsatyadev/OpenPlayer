package io.github.raghavsatyadev.openplayer.data.history.importer

import java.io.InputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

/**
 * Last.fm scrobbles as CSV (lastfm-to-csv style): `artist,album,track,date`, usually without a
 * header. A header row is detected and its column names used (so exports with extra columns
 * work). Dates are UTC, like `31 Jan 2021 12:34`, or unix seconds. A play whose date can't be
 * read gets the previous row's time minus 30 seconds, or is skipped when there is none.
 * The timestamp is the start of the play.
 */
object LastFmCsvParser : HistoryParser {

    private const val FALLBACK_STEP_MS = 30_000L

    private val formats: List<DateTimeFormatter> = listOf(
        "d MMM yyyy HH:mm", "d MMM yyyy, HH:mm", "d MMM yyyy HH:mm:ss", "d MMM yyyy, HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd'T'HH:mm:ss"
    ).map {
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(it).toFormatter(Locale.ENGLISH)
    }

    private class Columns(val artist: Int, val album: Int, val track: Int, val date: Int)

    private val defaultColumns = Columns(artist = 0, album = 1, track = 2, date = 3)

    override fun parse(input: InputStream, sink: ListenSink): Int {
        var unparseable = 0
        var columns = defaultColumns
        var previousMs: Long? = null
        var firstRow = true
        CsvRecords.read(input.reader(Charsets.UTF_8)) { row ->
            if (firstRow) {
                firstRow = false
                headerColumns(row)?.let { columns = it; return@read }
            }
            val artist = row.getOrNull(columns.artist).orEmpty()
            val track = row.getOrNull(columns.track).orEmpty()
            if (artist.isBlank() || track.isBlank()) {
                unparseable++
                return@read
            }
            val parsed = parseDate(row.getOrNull(columns.date).orEmpty())
            val ms = parsed ?: previousMs?.minus(FALLBACK_STEP_MS)
            if (ms == null) {
                unparseable++
                return@read
            }
            previousMs = ms
            sink.accept(
                ImportedListen(
                    title = track,
                    artist = artist,
                    album = row.getOrNull(columns.album)?.takeIf { it.isNotBlank() },
                    timestampMs = ms
                )
            )
        }
        return unparseable
    }

    private fun headerColumns(row: List<String>): Columns? {
        val names = row.map { it.trim().lowercase() }
        val artist = names.indexOfFirst { it == "artist" || it == "artist name" }
        val track = names.indexOfFirst { it == "track" || it == "track name" || it == "title" || it == "name" }
        if (artist < 0 || track < 0) return null
        val album = names.indexOfFirst { it == "album" || it == "album name" }
        val date = names.indexOfFirst { it == "date" || it == "uts" || it == "timestamp" || it == "utc_time" || it == "time" }
        return Columns(artist, album, track, if (date >= 0) date else defaultColumns.date)
    }

    internal fun parseDate(text: String): Long? {
        val value = text.trim()
        if (value.isEmpty()) return null
        if (value.all { it.isDigit() }) {
            val number = value.toLongOrNull() ?: return null
            // Unix seconds; a value this large can only be milliseconds.
            return if (number > 100_000_000_000L) number else number * 1000L
        }
        for (format in formats) {
            val local = runCatching { LocalDateTime.parse(value, format) }.getOrNull() ?: continue
            return local.toInstant(ZoneOffset.UTC).toEpochMilli()
        }
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
    }
}
