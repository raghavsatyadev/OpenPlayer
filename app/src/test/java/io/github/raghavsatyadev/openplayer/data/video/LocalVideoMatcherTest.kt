package io.github.raghavsatyadev.openplayer.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalVideoMatcherTest {

    private var nextId = 1L

    private fun video(path: String, height: Int = 720, width: Int = 1280, size: Long = 1000) = LocalVideo(
        id = nextId++,
        path = path,
        contentUri = "content://video/$path",
        displayName = path.substringAfterLast('/'),
        durationMs = 200_000,
        width = width,
        height = height,
        sizeBytes = size,
        dateAddedSec = 0,
    )

    private fun track(path: String, title: String, artist: String? = null, genre: String? = null) =
        TrackMatchInfo(path = path, title = title, artist = artist, genre = genre)

    private fun matcher(vararg paths: String) = LocalVideoMatcher(paths.map { video(it) })

    @Test
    fun `video with artist and title plus extra words matches by title and artist`() {
        val m = matcher("/Movies/Daft Punk - Get Lucky (Official Video) [1080p].mp4", "/Movies/Other Song.mp4")
        val match = m.bestMatchFor(track("/Music/01.flac", title = "Get Lucky", artist = "Daft Punk"))
        assertEquals("/Movies/Daft Punk - Get Lucky (Official Video) [1080p].mp4", match?.path)
    }

    @Test
    fun `video named like the audio file matches by filename`() {
        val m = matcher("/Movies/Some Track.mp4")
        val match = m.bestMatchFor(track("/Music/Some Track.flac", title = "Unrelated tag"))
        assertEquals("/Movies/Some Track.mp4", match?.path)
    }

    @Test
    fun `words split across phrases do not match`() {
        // "Lucky Get" has both words but not as the phrase "get lucky".
        val m = matcher("/Movies/Daft Punk - Lucky Get.mp4")
        assertNull(m.bestMatchFor(track("/Music/track01.flac", title = "Get Lucky", artist = "Daft Punk")))
    }

    @Test
    fun `missing artist word does not match`() {
        val m = matcher("/Movies/Get Lucky.mp4")
        assertNull(m.bestMatchFor(track("/Music/track01.flac", title = "Get Lucky", artist = "Daft Punk")))
    }

    @Test
    fun `title word inside a longer word does not match`() {
        val m = matcher("/Movies/Daft Punk - Getting Luckyish.mp4")
        assertNull(m.bestMatchFor(track("/Music/track01.flac", title = "Get Lucky", artist = "Daft Punk")))
    }

    @Test
    fun `accents case and apostrophes are ignored`() {
        val m = matcher("/Movies/BEYONCE - Dont Stop.mp4")
        val match = m.bestMatchFor(track("/Music/track01.flac", title = "Don't Stop", artist = "Beyoncé"))
        assertEquals("/Movies/BEYONCE - Dont Stop.mp4", match?.path)
    }

    @Test
    fun `cjk title matches inside a name without spaces`() {
        val m = matcher("/Movies/米津玄師 - Lemon MV.mp4", "/Movies/米津 - 別の曲.mp4")
        val match = m.bestMatchFor(track("/Music/track01.flac", title = "Lemon", artist = "米津玄師"))
        assertEquals("/Movies/米津玄師 - Lemon MV.mp4", match?.path)
    }

    @Test
    fun `genre can stand in for the artist`() {
        val m = matcher("/Movies/Nightcore - Angel With A Shotgun.mp4")
        val match = m.bestMatchFor(
            track("/Music/track01.flac", title = "Angel With A Shotgun", artist = "The Cab", genre = "Nightcore")
        )
        assertEquals("/Movies/Nightcore - Angel With A Shotgun.mp4", match?.path)
    }

    @Test
    fun `same folder option drops videos elsewhere`() {
        val m = matcher("/Movies/Song.mp4", "/Music/Album/Song.mkv")
        val t = track("/Music/Album/Song.flac", title = "Song")
        assertEquals(2, m.matchesFor(t).size)
        assertEquals(listOf("/Music/Album/Song.mkv"), m.matchesFor(t, sameFolderOnly = true).map { it.path })
    }

    @Test
    fun `filename mode ignores tags and title mode ignores filename`() {
        val m = matcher("/Movies/Track File.mp4", "/Movies/Artist - Title.mp4")
        val t = track("/Music/Track File.mp3", title = "Title", artist = "Artist")
        assertEquals(listOf("/Movies/Track File.mp4"), m.matchesFor(t, VideoMatchingMode.FILENAME).map { it.path })
        assertEquals(
            listOf("/Movies/Artist - Title.mp4"),
            m.matchesFor(t, VideoMatchingMode.TITLE_AND_ARTIST).map { it.path }
        )
    }

    @Test
    fun `sure match beats a higher resolution loose match`() {
        val loose = video("/Movies/Artist - Title live 4k.mp4", height = 2160, width = 3840)
        val sure = video("/Movies/Artist - Title (480p).mp4", height = 480, width = 854)
        val m = LocalVideoMatcher(listOf(loose, sure))
        val best = m.bestMatchFor(track("/Music/Artist - Title.flac", title = "Title", artist = "Artist"))
        assertEquals(sure.path, best?.path)
    }

    @Test
    fun `higher resolution wins among loose matches`() {
        val low = video("/Movies/Artist - Title A.mp4", height = 480, width = 854)
        val high = video("/Movies/Artist - Title B.mp4", height = 1080, width = 1920)
        val m = LocalVideoMatcher(listOf(low, high))
        val best = m.bestMatchFor(track("/Music/track02.flac", title = "Title", artist = "Artist"))
        assertEquals(high.path, best?.path)
    }

    @Test
    fun `sure matches strip trailing brackets one at a time`() {
        val sure = LocalVideoMatcher.sureMatches(
            listOf(
                "/v/My Song (Official Video) [1080p].mp4",
                "/v/My Song extra.mp4",
                "/v/[Intro] My Song.mp4",
            ),
            "My Song"
        )
        assertEquals(setOf("/v/My Song (Official Video) [1080p].mp4"), sure)
    }

    @Test
    fun `empty library and blank title match nothing`() {
        assertTrue(LocalVideoMatcher(emptyList()).matchesFor(track("/a.mp3", "x")).isEmpty())
        val m = matcher("/Movies/a.mp4")
        assertTrue(m.matchesFor(track("/Music/.mp3", title = ""), VideoMatchingMode.TITLE_AND_ARTIST).isEmpty())
    }
}
