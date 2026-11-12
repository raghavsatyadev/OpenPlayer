package io.github.raghavsatyadev.openplayer.data.history.importer

import java.io.StringReader
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TakeoutHtmlHistoryTest {

    private fun cell(header: String, body: String, caption: String = "") =
        """<div class="outer-cell mdl-cell mdl-cell--12-col mdl-shadow--2dp"><div class="mdl-grid">""" +
            """<div class="header-cell mdl-cell mdl-cell--12-col"><p class="mdl-typography--title">$header<br></p></div>""" +
            """<div class="content-cell mdl-cell mdl-cell--6-col mdl-typography--body-1">$body</div>""" +
            """<div class="content-cell mdl-cell mdl-cell--6-col mdl-typography--body-1 mdl-typography--text-right"></div>""" +
            """<div class="content-cell mdl-cell mdl-cell--12-col mdl-typography--caption">$caption</div></div></div>"""

    private val page = "<html><body><div class=\"mdl-grid\">" +
        cell(
            "YouTube Music",
            "Watched <a href=\"https://music.youtube.com/watch?v=a\">Chaand Baaliyan</a><br>" +
                "<a href=\"https://www.youtube.com/channel/c\">Aditya A - Topic</a><br>11 Nov 2026, 01:38:27 IST<br>"
        ) +
        cell(
            "YouTube",
            "Watched&nbsp;<a href=\"https://www.youtube.com/watch?v=b\">Tom &amp; Jerry &#39;Live&#39;</a><br>" +
                "<a href=\"https://www.youtube.com/channel/d\">Cartoons</a><br>Nov 2, 2026, 1:05:09 PM PST<br>"
        ) +
        cell(
            "YouTube Music",
            "Watched <a href=\"https://music.youtube.com/watch?v=e\">https://music.youtube.com/watch?v=e</a><br>" +
                "3 Oct 2026, 10:00:00 GMT+01:00<br>"
        ) +
        cell("YouTube", "Watched <a href=\"x\">Ad</a><br><a href=\"y\">Brand</a><br>1 Jan 2026, 00:00:00 UTC<br>", "From Google Ads") +
        "</div></body></html>"

    @Test
    fun `reads every cell across small chunks`() {
        val entries = mutableListOf<TakeoutHtmlHistory.Entry>()
        // A tiny reader forces cells to straddle chunk boundaries.
        val reader = object : java.io.Reader() {
            val inner = StringReader(page)
            override fun read(cbuf: CharArray, off: Int, len: Int) = inner.read(cbuf, off, minOf(len, 7))
            override fun close() = inner.close()
        }
        TakeoutHtmlHistory.forEachEntry(reader) { entries += it }

        assertEquals(4, entries.size)
        assertEquals(TakeoutHtmlHistory.Entry("YouTube Music", "Chaand Baaliyan", "Aditya A - Topic", "11 Nov 2026, 01:38:27 IST", false), entries[0])
        assertEquals("Tom & Jerry 'Live'", entries[1].title)
        assertEquals("Cartoons", entries[1].channel)
        assertNull(entries[2].channel)
        assertEquals("https://music.youtube.com/watch?v=e", entries[2].title)
        assertEquals("3 Oct 2026, 10:00:00 GMT+01:00", entries[2].time)
        assertTrue(entries[3].isAd)
        assertFalse(entries[0].isAd)
    }

    @Test
    fun `parses day-first and month-first dates with zones`() {
        val fallback = ZoneOffset.ofHours(3)
        assertEquals(
            ZonedDateTime.of(2026, 11, 11, 1, 38, 27, 0, ZoneOffset.ofHoursMinutes(5, 30)).toInstant().toEpochMilli(),
            TakeoutHtmlHistory.parseTime("11 Nov 2026, 01:38:27 IST", fallback)
        )
        assertEquals(
            ZonedDateTime.of(2026, 11, 2, 13, 5, 9, 0, ZoneOffset.ofHours(-8)).toInstant().toEpochMilli(),
            TakeoutHtmlHistory.parseTime("Nov 2, 2026, 1:05:09 PM PST", fallback)
        )
        assertEquals(
            ZonedDateTime.of(2026, 10, 3, 10, 0, 0, 0, ZoneOffset.ofHours(1)).toInstant().toEpochMilli(),
            TakeoutHtmlHistory.parseTime("3 Oct 2026, 10:00:00 GMT+01:00", fallback)
        )
        assertEquals(
            ZonedDateTime.of(2026, 9, 4, 8, 0, 0, 0, ZoneOffset.ofHours(-7)).toInstant().toEpochMilli(),
            TakeoutHtmlHistory.parseTime("4 Sept 2026, 08:00:00 GMT-7", fallback)
        )
        assertEquals(
            ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, fallback).toInstant().toEpochMilli(),
            TakeoutHtmlHistory.parseTime("1 Jan 2026, 00:00:00 XYZT", fallback)
        )
        assertNull(TakeoutHtmlHistory.parseTime("yesterday", fallback))
    }
}
