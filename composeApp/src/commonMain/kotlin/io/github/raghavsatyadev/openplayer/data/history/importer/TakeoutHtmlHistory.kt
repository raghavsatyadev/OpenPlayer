package io.github.raghavsatyadev.openplayer.data.history.importer

import java.io.Reader
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

/**
 * Reads the HTML `watch-history.html` that Google Takeout writes by default (English export).
 *
 * Each play is an `outer-cell` block: a header ("YouTube" or "YouTube Music"), then
 * "Watched <a>Title</a><br><a>Channel</a><br>11 Nov 2026, 01:38:27 IST". Removed videos have no
 * channel line, and ads mention "From Google Ads". Pure Kotlin so it runs in JVM tests.
 */
internal object TakeoutHtmlHistory {

    data class Entry(
        val header: String?,
        val title: String?,
        val channel: String?,
        val time: String?,
        val isAd: Boolean
    )

    private const val CELL_MARKER = "<div class=\"outer-cell"
    private val headerPattern = Regex("""mdl-typography--title">(.*?)<br""", RegexOption.DOT_MATCHES_ALL)
    private val bodyPattern = Regex("""mdl-typography--body-1">(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
    private val lineBreak = Regex("""<br\s*/?>""")
    private val anchor = Regex("""<a[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
    private val tag = Regex("""<[^>]+>""")
    private val numericEntity = Regex("""&#(x?)([0-9a-fA-F]+);""")

    /** Streams [reader] and calls [action] once per play block, without loading the whole file. */
    fun forEachEntry(reader: Reader, action: (Entry) -> Unit) {
        val buffer = StringBuilder()
        val chunk = CharArray(64 * 1024)
        var start = -1
        var eof = false
        while (true) {
            if (start < 0) {
                start = buffer.indexOf(CELL_MARKER)
                if (start > 0) {
                    buffer.delete(0, start)
                    start = 0
                }
            }
            if (start >= 0) {
                val next = buffer.indexOf(CELL_MARKER, CELL_MARKER.length)
                if (next > 0) {
                    action(parseCell(buffer.substring(0, next)))
                    buffer.delete(0, next)
                    continue
                }
            }
            if (eof) break
            val read = reader.read(chunk)
            if (read < 0) {
                eof = true
            } else {
                buffer.appendRange(chunk, 0, read)
            }
        }
        if (start >= 0 && buffer.isNotEmpty()) action(parseCell(buffer.toString()))
    }

    fun parseCell(cell: String): Entry {
        val header = headerPattern.find(cell)?.groupValues?.get(1)?.let { text(it) }
        val body = bodyPattern.find(cell)?.groupValues?.get(1)
        val isAd = cell.contains("From Google Ads")
        val parts = body?.split(lineBreak)?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        if (parts.isEmpty()) return Entry(header, null, null, null, isAd)

        val first = parts[0]
        val title = anchor.find(first)?.groupValues?.get(1)?.let { text(it) }
            ?: text(first).removePrefix("Watched").trim()
        val channel = if (parts.size >= 3) {
            text(anchor.find(parts[1])?.groupValues?.get(1) ?: parts[1])
        } else {
            null
        }
        val time = if (parts.size >= 2) text(parts.last()) else null
        return Entry(header, title.ifBlank { null }, channel?.ifBlank { null }, time?.ifBlank { null }, isAd)
    }

    /** Strips tags, decodes entities and turns non-breaking spaces into plain ones. */
    fun text(html: String): String {
        val decoded = numericEntity.replace(tag.replace(html, "")) { m ->
            val radix = if (m.groupValues[1].isEmpty()) 10 else 16
            m.groupValues[2].toIntOrNull(radix)?.let { String(Character.toChars(it)) } ?: m.value
        }
        return decoded
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace(' ', ' ')
            .replace(' ', ' ')
            .trim()
    }

    private val formatters: List<DateTimeFormatter> = listOf(
        "d MMM yyyy, H:mm:ss",
        "MMM d, yyyy, h:mm:ss a",
        "MMM d, yyyy, H:mm:ss",
        "d MMM yyyy, h:mm:ss a"
    ).map { pattern ->
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.ENGLISH)
    }

    private val timeAndZone = Regex("""^(.*\d{1,2}:\d{2}:\d{2}(?:\s*[AaPp]\.?[Mm]\.?)?)\s+(\S+)$""")
    private val gmtOffset = Regex("""^(?:GMT|UTC)(?:([+-])(\d{1,2})(?::?(\d{2}))?)?$""", RegexOption.IGNORE_CASE)

    /** Fixed offsets for zone abbreviations Takeout prints; unknown ones use [fallbackZone]. */
    private val abbreviations: Map<String, String> = mapOf(
        "IST" to "+05:30", "PST" to "-08:00", "PDT" to "-07:00", "MST" to "-07:00", "MDT" to "-06:00",
        "CST" to "-06:00", "CDT" to "-05:00", "EST" to "-05:00", "EDT" to "-04:00", "AKST" to "-09:00",
        "AKDT" to "-08:00", "HST" to "-10:00", "WET" to "+00:00", "WEST" to "+01:00", "BST" to "+01:00",
        "CET" to "+01:00", "CEST" to "+02:00", "EET" to "+02:00", "EEST" to "+03:00", "MSK" to "+03:00",
        "PKT" to "+05:00", "NPT" to "+05:45", "WIB" to "+07:00", "SGT" to "+08:00", "HKT" to "+08:00",
        "AWST" to "+08:00", "JST" to "+09:00", "KST" to "+09:00", "ACST" to "+09:30", "AEST" to "+10:00",
        "AEDT" to "+11:00", "NZST" to "+12:00", "NZDT" to "+13:00"
    )

    /** Parses "11 Nov 2026, 01:38:27 IST" or "Nov 11, 2026, 1:38:27 AM PST" to epoch millis. */
    fun parseTime(raw: String, fallbackZone: ZoneId): Long? {
        val normalized = text(raw).replace(Regex("""\s+"""), " ").replace("Sept ", "Sep ")
        val match = timeAndZone.find(normalized)
        val (local, zoneText) = if (match != null) {
            match.groupValues[1].replace(".", "") to match.groupValues[2]
        } else {
            normalized to null
        }
        val dateTime = formatters.firstNotNullOfOrNull { f ->
            runCatching { LocalDateTime.parse(local, f) }.getOrNull()
        } ?: return null
        val zone = zoneText?.let { zoneFor(it) } ?: fallbackZone
        return dateTime.atZone(zone).toInstant().toEpochMilli()
    }

    private fun zoneFor(text: String): ZoneId? {
        gmtOffset.find(text)?.let { m ->
            val sign = m.groupValues[1]
            if (sign.isEmpty()) return ZoneOffset.UTC
            val hours = m.groupValues[2].toInt()
            val minutes = m.groupValues[3].ifEmpty { "0" }.toInt()
            val total = (hours * 3600 + minutes * 60) * if (sign == "-") -1 else 1
            return runCatching { ZoneOffset.ofTotalSeconds(total) }.getOrNull()
        }
        abbreviations[text.uppercase(Locale.ENGLISH)]?.let { return ZoneOffset.of(it) }
        return runCatching { ZoneId.of(text) }.getOrNull()
    }
}
