package io.github.raghavsatyadev.openplayer.data.history.importer

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** The three export readers, fed from strings. */
@RunWith(AndroidJUnit4::class)
class HistoryParsersTest {

    private fun String.stream() = ByteArrayInputStream(toByteArray(Charsets.UTF_8))

    private fun utc(text: String) = Instant.parse(text).toEpochMilli()

    // ---- Last.fm ----

    @Test
    fun lastFmReadsQuotesCommasAndEscapedQuotes() {
        val csv = "Artist One,\"Album, The\",\"Song \"\"Quoted\"\", Part 2\",31 Jan 2021 12:34\n" +
            "Other,,Plain,01 Feb 2021 00:05\r\n"
        val result = LastFmCsvParser.parseAll(csv.stream())
        assertThat(result.unparseable).isEqualTo(0)
        assertThat(result.listens).hasSize(2)
        val first = result.listens[0]
        assertThat(first.artist).isEqualTo("Artist One")
        assertThat(first.album).isEqualTo("Album, The")
        assertThat(first.title).isEqualTo("Song \"Quoted\", Part 2")
        assertThat(first.timestampMs).isEqualTo(utc("2021-01-31T12:34:00Z"))
        assertThat(first.timestampIsEnd).isFalse()
        assertThat(result.listens[1].album).isNull()
        assertThat(result.listens[1].timestampMs).isEqualTo(utc("2021-02-01T00:05:00Z"))
    }

    @Test
    fun lastFmSkipsHeaderAndAcceptsUnixSeconds() {
        val csv = "artist,album,track,date\nA,B,C,1612000000\n"
        val result = LastFmCsvParser.parseAll(csv.stream())
        assertThat(result.listens).hasSize(1)
        assertThat(result.listens[0].title).isEqualTo("C")
        assertThat(result.listens[0].timestampMs).isEqualTo(1_612_000_000_000L)
    }

    @Test
    fun lastFmBadDateUsesPreviousMinus30sAndFirstBadRowIsSkipped() {
        val csv = "A,B,NoDate,garbage\n" +
            "A,B,Good,31 Jan 2021 12:34\n" +
            "A,B,Bad,not a date\n"
        val result = LastFmCsvParser.parseAll(csv.stream())
        assertThat(result.unparseable).isEqualTo(1)
        assertThat(result.listens.map { it.title }).containsExactly("Good", "Bad").inOrder()
        assertThat(result.listens[1].timestampMs)
            .isEqualTo(utc("2021-01-31T12:34:00Z") - 30_000L)
    }

    // ---- Spotify ----

    @Test
    fun spotifyExtendedUsesEndTimeAndSkipsPodcastsAndZeroPlays() {
        val json = """
            [
              {"ts":"2021-03-01T10:00:00Z","ms_played":200000,"master_metadata_track_name":"Song","master_metadata_album_artist_name":"Artist","master_metadata_album_album_name":"Album","spotify_track_uri":"x","skipped":null},
              {"ts":"2021-03-01T10:05:00Z","ms_played":0,"master_metadata_track_name":"Zero","master_metadata_album_artist_name":"Artist","master_metadata_album_album_name":"Album"},
              {"ts":"2021-03-01T10:06:00Z","ms_played":60000,"master_metadata_track_name":null,"master_metadata_album_artist_name":null,"episode_name":"Podcast"}
            ]
        """.trimIndent()
        val result = SpotifyHistoryParser.parseAll(json.stream())
        assertThat(result.unparseable).isEqualTo(0)
        assertThat(result.listens).hasSize(1)
        val play = result.listens[0]
        assertThat(play.title).isEqualTo("Song")
        assertThat(play.artist).isEqualTo("Artist")
        assertThat(play.albumArtist).isEqualTo("Artist")
        assertThat(play.album).isEqualTo("Album")
        assertThat(play.timestampIsEnd).isTrue()
        assertThat(play.msPlayed).isEqualTo(200_000L)
        assertThat(play.timestampMs).isEqualTo(utc("2021-03-01T10:00:00Z"))
    }

    @Test
    fun spotifyBasicAccountExport() {
        val json = """[{"endTime":"2020-12-24 18:30","artistName":"Band","trackName":"Tune","msPlayed":123456}]"""
        val result = SpotifyHistoryParser.parseAll(json.stream())
        assertThat(result.listens).hasSize(1)
        assertThat(result.listens[0].timestampMs).isEqualTo(utc("2020-12-24T18:30:00Z"))
        assertThat(result.listens[0].msPlayed).isEqualTo(123_456L)
        assertThat(result.listens[0].title).isEqualTo("Tune")
    }

    // ---- ListenBrainz ----

    private val lbA = """{"listened_at":1612000000,"track_metadata":{"track_name":"One","artist_name":"Artist","release_name":"Rel","additional_info":{"x":[1,2]}}}"""
    private val lbB = """{"track_metadata":{"track_name":"Two","artist_name":"Artist"},"listened_at":1612000300.5}"""

    @Test
    fun listenBrainzJsonArray() {
        val result = ListenBrainzParser.parseAll("[$lbA,$lbB]".stream())
        assertThat(result.listens.map { it.title }).containsExactly("One", "Two").inOrder()
        assertThat(result.listens[0].album).isEqualTo("Rel")
        assertThat(result.listens[0].timestampMs).isEqualTo(1_612_000_000_000L)
        assertThat(result.listens[1].timestampMs).isEqualTo(1_612_000_300_500L)
    }

    @Test
    fun listenBrainzJsonlWithFloatTimeAndBrokenLine() {
        val result = ListenBrainzParser.parseAll("$lbA\n\n$lbB\n{not json\n".stream())
        assertThat(result.listens).hasSize(2)
        assertThat(result.unparseable).isEqualTo(1)
        assertThat(result.listens[1].timestampMs).isEqualTo(1_612_000_300_500L)
    }

    @Test
    fun listenBrainzZipExportIgnoresFeedback() {
        val bytes = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("listens/2021/2.jsonl"))
                zip.write("$lbA\n$lbB\n".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("listens/2021/3.jsonl"))
                zip.write("""{"listened_at":1614600000,"track_metadata":{"track_name":"Three","artist_name":"Artist"}}""".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("feedback.jsonl"))
                zip.write("""{"created":1612000000,"recording_msid":"abc","score":1}""".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("user.json"))
                zip.write("""{"user_name":"someone"}""".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()
        val result = ListenBrainzParser.parseAll(ByteArrayInputStream(bytes))
        assertThat(result.listens.map { it.title }).containsExactly("One", "Two", "Three").inOrder()
        assertThat(result.unparseable).isEqualTo(0)
    }

    // ---- YouTube Music ----

    @Test
    fun youTubeMusicJsonKeepsOnlyMusicAndCleansNames() {
        val json = """
            [
              {
                "header": "YouTube Music",
                "title": "Watched Blinding Lights",
                "titleUrl": "https://music.youtube.com/watch?v=1",
                "subtitles": [{"name": "The Weeknd - Topic", "url": "https://youtube.com/channel/1"}],
                "time": "2024-03-01T18:22:05.123Z"
              },
              {
                "header": "YouTube Music",
                "title": "Watched Artist - Song (Official Video)",
                "titleUrl": "https://music.youtube.com/watch?v=2",
                "subtitles": [{"name": "ArtistVEVO", "url": "https://youtube.com/channel/2"}],
                "time": "2024-03-01T19:00:00Z"
              },
              {
                "header": "YouTube",
                "title": "Watched Normal Video",
                "titleUrl": "https://www.youtube.com/watch?v=3",
                "subtitles": [{"name": "Creator", "url": "https://youtube.com/channel/3"}],
                "time": "2024-03-01T20:00:00Z"
              },
              {
                "header": "YouTube Music",
                "title": "Watched Some Ad",
                "time": "2024-03-01T21:00:00Z",
                "details": [{"name": "From Google Ads"}]
              },
              {
                "header": "YouTube Music",
                "title": "Watched https://www.youtube.com/watch?v=removed",
                "time": "2024-03-01T22:00:00Z"
              }
            ]
        """.trimIndent()
        val result = YouTubeMusicHistoryParser.parseAll(json.stream())
        assertThat(result.unparseable).isEqualTo(1)
        assertThat(result.listens).hasSize(2)

        val first = result.listens[0]
        assertThat(first.title).isEqualTo("Blinding Lights")
        assertThat(first.artist).isEqualTo("The Weeknd")
        assertThat(first.album).isNull()
        assertThat(first.albumArtist).isNull()
        assertThat(first.timestampMs).isEqualTo(utc("2024-03-01T18:22:05.123Z"))
        assertThat(first.timestampIsEnd).isFalse()
        assertThat(first.msPlayed).isNull()

        val second = result.listens[1]
        assertThat(second.title).isEqualTo("Song")
        assertThat(second.artist).isEqualTo("Artist")
        assertThat(second.album).isNull()
        assertThat(second.albumArtist).isNull()
        assertThat(second.timestampMs).isEqualTo(utc("2024-03-01T19:00:00Z"))
        assertThat(second.timestampIsEnd).isFalse()
        assertThat(second.msPlayed).isNull()
    }

    @Test
    fun youTubeMusicReadsTakeoutZip() {
        val json = """
            [
              {
                "header": "YouTube Music",
                "title": "Watched Starlight",
                "subtitles": [{"name": "Muse - Topic"}],
                "time": "2024-03-02T12:00:00Z"
              }
            ]
        """.trimIndent()
        val bytes = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("Takeout/YouTube and YouTube Music/history/watch-history.json"))
                zip.write(json.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("Takeout/YouTube and YouTube Music/subscriptions/subscriptions.csv"))
                zip.write("Channel Id,Channel Url,Channel Title\n123,http://...,Channel\n".toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }.toByteArray()
        val result = YouTubeMusicHistoryParser.parseAll(ByteArrayInputStream(bytes))
        assertThat(result.unparseable).isEqualTo(0)
        assertThat(result.listens).hasSize(1)
        assertThat(result.listens[0].title).isEqualTo("Starlight")
        assertThat(result.listens[0].artist).isEqualTo("Muse")
        assertThat(result.listens[0].timestampMs).isEqualTo(utc("2024-03-02T12:00:00Z"))
        assertThat(result.listens[0].timestampIsEnd).isFalse()
        assertThat(result.listens[0].msPlayed).isNull()
    }

    @Test
    fun youTubeMusicReadsTakeoutHtml() {
        fun cell(header: String, body: String) =
            """<div class="outer-cell mdl-cell"><p class="mdl-typography--title">$header<br></p>""" +
                """<div class="content-cell mdl-cell mdl-cell--6-col mdl-typography--body-1">$body</div></div>"""
        val html = "﻿<html><body>" +
            cell("YouTube Music", "Watched&nbsp;<a href=\"u\">Starlight</a><br><a href=\"c\">Muse - Topic</a><br>2 Mar 2024, 12:00:00 GMT<br>") +
            cell("YouTube", "Watched&nbsp;<a href=\"u\">Some Vlog</a><br><a href=\"c\">Vlogger</a><br>2 Mar 2024, 13:00:00 GMT<br>") +
            cell("YouTube Music", "Watched&nbsp;<a href=\"u\">https://music.youtube.com/watch?v=x</a><br>2 Mar 2024, 14:00:00 GMT<br>") +
            "</body></html>"
        val result = YouTubeMusicHistoryParser.parseAll(html.stream())
        assertThat(result.unparseable).isEqualTo(1)
        assertThat(result.listens).hasSize(1)
        assertThat(result.listens[0].title).isEqualTo("Starlight")
        assertThat(result.listens[0].artist).isEqualTo("Muse")
        assertThat(result.listens[0].timestampMs).isEqualTo(utc("2024-03-02T12:00:00Z"))
    }
}
