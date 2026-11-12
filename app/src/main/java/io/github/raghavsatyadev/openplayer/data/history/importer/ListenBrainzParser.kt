package io.github.raghavsatyadev.openplayer.data.history.importer

import android.util.JsonReader
import android.util.JsonToken
import java.io.BufferedInputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipInputStream

/**
 * ListenBrainz listens: a JSON array, JSONL (one listen per line) or the zip export (entries like
 * `listens/2021/3.jsonl`; other entries such as feedback are ignored). `listened_at` is in
 * seconds, possibly fractional, and is the start of the play.
 */
object ListenBrainzParser : HistoryParser {

    override fun parse(input: InputStream, sink: ListenSink): Int {
        val buffered = if (input.markSupported()) input else BufferedInputStream(input)
        buffered.mark(4)
        val b0 = buffered.read()
        val b1 = buffered.read()
        buffered.reset()
        return if (b0 == 'P'.code && b1 == 'K'.code) parseZip(buffered, sink) else parseText(buffered, sink)
    }

    private fun parseZip(input: InputStream, sink: ListenSink): Int {
        var unparseable = 0
        val zip = ZipInputStream(input)
        while (true) {
            val entry = zip.nextEntry ?: break
            val name = entry.name.lowercase()
            val isListens = !entry.isDirectory && !name.contains("feedback") &&
                (name.endsWith(".jsonl") || name.endsWith(".json")) &&
                (name.contains("listens") || name.endsWith(".jsonl"))
            if (isListens) unparseable += parseText(NonClosing(zip), sink)
            zip.closeEntry()
        }
        return unparseable
    }

    private fun parseText(input: InputStream, sink: ListenSink): Int {
        val buffered = if (input.markSupported()) input else BufferedInputStream(input)
        buffered.mark(1 shl 16)
        var first = buffered.read()
        while (first != -1 && first.toChar().isWhitespace()) first = buffered.read()
        if (first == -1) return 0
        buffered.reset()
        val reader = buffered.reader(Charsets.UTF_8)
        return if (first == '['.code) parseArray(reader, sink) else parseLines(reader, sink)
    }

    private fun parseArray(source: java.io.Reader, sink: ListenSink): Int {
        var unparseable = 0
        JsonReader(source).use { reader ->
            JsonFields.forEachInArray(reader) {
                if (!readListen(reader, sink)) unparseable++
            }
        }
        return unparseable
    }

    private fun parseLines(source: java.io.Reader, sink: ListenSink): Int {
        var unparseable = 0
        source.buffered().forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val ok = runCatching { JsonReader(StringReader(line)).use { readListen(it, sink) } }
                .getOrDefault(false)
            if (!ok) unparseable++
        }
        return unparseable
    }

    /** Reads one listen object; true when it became a play. Always consumes the value. */
    private fun readListen(reader: JsonReader, sink: ListenSink): Boolean {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return false
        }
        var listenedAt: Double? = null
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        JsonFields.forEachField(reader) { name ->
            when (name) {
                "listened_at" -> listenedAt = JsonFields.nextDouble(reader)
                "track_metadata" -> if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                    JsonFields.forEachField(reader) { key ->
                        when (key) {
                            "track_name" -> title = JsonFields.nextText(reader)
                            "artist_name" -> artist = JsonFields.nextText(reader)
                            "release_name" -> album = JsonFields.nextText(reader)
                            else -> reader.skipValue()
                        }
                    }
                } else reader.skipValue()
                else -> reader.skipValue()
            }
        }
        val seconds = listenedAt
        if (seconds == null || seconds <= 0.0 || title.isNullOrBlank() || artist.isNullOrBlank()) return false
        sink.accept(
            ImportedListen(
                title = title!!,
                artist = artist!!,
                album = album?.takeIf { it.isNotBlank() },
                timestampMs = (seconds * 1000.0).toLong()
            )
        )
        return true
    }

    /** Lets a nested reader consume a zip entry without closing the whole archive. */
    internal class NonClosing(input: InputStream) : FilterInputStream(input) {
        override fun close() = Unit
    }
}
