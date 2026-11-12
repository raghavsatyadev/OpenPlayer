package io.github.raghavsatyadev.openplayer.data.history.importer

import android.util.JsonReader
import android.util.JsonToken
import io.github.raghavsatyadev.openplayer.data.text.YouTubeTitles
import java.io.BufferedInputStream
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import java.util.zip.ZipInputStream

/**
 * YouTube Music watch history exported via Google Takeout: `watch-history.json`,
 * `watch-history.html` (Takeout's default format, see [TakeoutHtmlHistory]) or a Takeout zip
 * containing either. HTML times without a known zone use the device zone.
 *
 * Entries with headers other than "YouTube Music" (regular YouTube videos) and ads are skipped
 * silently. Missing titles, channels, timestamps or titles left as raw URLs after "Watched " are
 * counted as unparseable. Plays have start timestamps and no duration; the planner treats them as
 * full plays.
 */
object YouTubeMusicHistoryParser : HistoryParser {

    override fun parse(input: InputStream, sink: ListenSink): Int {
        val buffered = if (input.markSupported()) input else BufferedInputStream(input)
        buffered.mark(64)
        val head = ByteArray(64)
        val size = buffered.read(head).coerceAtLeast(0)
        buffered.reset()
        if (size >= 2 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()) {
            return parseZip(buffered, sink)
        }
        val first = (0 until size).map { head[it].toInt() and 0xFF }
            .firstOrNull { it > ' '.code && it != 0xEF && it != 0xBB && it != 0xBF }
        return if (first == '<'.code) parseHtml(buffered, sink) else parseJson(buffered, sink)
    }

    private fun parseZip(input: InputStream, sink: ListenSink): Int {
        var unparseable = 0
        val zip = ZipInputStream(input)
        while (true) {
            val entry = zip.nextEntry ?: break
            val name = entry.name.lowercase()
            if (!entry.isDirectory && name.endsWith("watch-history.json")) {
                unparseable += parseJson(ListenBrainzParser.NonClosing(zip), sink)
            } else if (!entry.isDirectory && name.endsWith("watch-history.html")) {
                unparseable += parseHtml(ListenBrainzParser.NonClosing(zip), sink)
            }
            zip.closeEntry()
        }
        return unparseable
    }

    private fun parseHtml(input: InputStream, sink: ListenSink): Int {
        var unparseable = 0
        val zone = ZoneId.systemDefault()
        val reader = input.reader(Charsets.UTF_8)
        TakeoutHtmlHistory.forEachEntry(reader) { entry ->
            val time = entry.time?.let { TakeoutHtmlHistory.parseTime(it, zone) }
            if (!emit(entry.header, entry.title, entry.channel, time, entry.isAd, sink)) unparseable++
        }
        return unparseable
    }

    private fun parseJson(input: InputStream, sink: ListenSink): Int {
        var unparseable = 0
        JsonReader(input.reader(Charsets.UTF_8)).use { reader ->
            if (reader.peek() != JsonToken.BEGIN_ARRAY) {
                return 0
            }
            JsonFields.forEachInArray(reader) {
                if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                    reader.skipValue()
                    unparseable++
                    return@forEachInArray
                }
                var header: String? = null
                var title: String? = null
                var channelName: String? = null
                var time: String? = null
                var isAd = false

                JsonFields.forEachField(reader) { name ->
                    when (name) {
                        "header" -> header = JsonFields.nextText(reader)
                        "title" -> title = JsonFields.nextText(reader)
                        "time" -> time = JsonFields.nextText(reader)
                        "subtitles" -> {
                            if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                                var first = true
                                JsonFields.forEachInArray(reader) {
                                    if (first && reader.peek() == JsonToken.BEGIN_OBJECT) {
                                        first = false
                                        JsonFields.forEachField(reader) { subName ->
                                            if (subName == "name") {
                                                channelName = JsonFields.nextText(reader)
                                            } else {
                                                reader.skipValue()
                                            }
                                        }
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                            } else {
                                reader.skipValue()
                            }
                        }
                        "details" -> {
                            if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                                JsonFields.forEachInArray(reader) {
                                    if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                                        JsonFields.forEachField(reader) { detailName ->
                                            if (detailName == "name") {
                                                if (JsonFields.nextText(reader) == "From Google Ads") {
                                                    isAd = true
                                                }
                                            } else {
                                                reader.skipValue()
                                            }
                                        }
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                            } else {
                                reader.skipValue()
                            }
                        }
                        else -> reader.skipValue()
                    }
                }

                val timestampMs = time?.let { parseTime(it) }
                if (!emit(header, title, channelName, timestampMs, isAd, sink)) unparseable++
            }
        }
        return unparseable
    }

    /**
     * Applies the shared rules and emits one listen. Returns false when the entry is a YouTube
     * Music play that cannot be used (no title, URL title, no channel or no time); other headers
     * and ads return true without emitting.
     */
    private fun emit(
        header: String?,
        title: String?,
        channel: String?,
        timestampMs: Long?,
        isAd: Boolean,
        sink: ListenSink
    ): Boolean {
        if (header?.trim() != "YouTube Music" || isAd) return true
        val withoutWatched = title?.removePrefix("Watched ")?.trim()
        if (withoutWatched.isNullOrEmpty() || isUrl(withoutWatched)) return false
        if (channel.isNullOrBlank() || timestampMs == null) return false

        var cleanTitle = YouTubeTitles.cleanTitle(withoutWatched)
        var cleanArtist = YouTubeTitles.cleanChannel(channel)
        if (!channel.trim().endsWith(" - Topic", ignoreCase = true)) {
            YouTubeTitles.splitArtistTitle(cleanTitle, cleanArtist)?.let { (artist, song) ->
                cleanArtist = artist
                cleanTitle = YouTubeTitles.cleanTitle(song)
            }
        }
        sink.accept(
            ImportedListen(
                title = cleanTitle,
                artist = cleanArtist,
                album = null,
                albumArtist = null,
                timestampMs = timestampMs,
                timestampIsEnd = false,
                msPlayed = null
            )
        )
        return true
    }

    private fun isUrl(text: String): Boolean =
        text.startsWith("http://", ignoreCase = true) ||
            text.startsWith("https://", ignoreCase = true) ||
            text.startsWith("www.", ignoreCase = true)

    internal fun parseTime(text: String): Long? =
        runCatching { Instant.parse(text.trim()).toEpochMilli() }.getOrNull()
}
