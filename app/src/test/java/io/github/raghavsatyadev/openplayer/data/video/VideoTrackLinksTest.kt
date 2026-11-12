package io.github.raghavsatyadev.openplayer.data.video

import io.github.raghavsatyadev.openplayer.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTrackLinksTest {

    private fun video(id: Long, path: String) = LocalVideo(
        id = id,
        path = path,
        contentUri = "content://video/$id",
        displayName = path.substringAfterLast('/'),
        durationMs = 200_000,
        width = 1280,
        height = 720,
        sizeBytes = 1000,
        dateAddedSec = 0,
    )

    private fun song(id: String, path: String, title: String, artist: String) = Song(
        id = id,
        title = title,
        artist = artist,
        artistId = 0,
        album = "Album",
        albumId = 0,
        path = path,
        contentUriString = "content://audio/$id",
        albumArtUriString = null,
        duration = 200_000,
        mimeType = null,
        bitrate = null,
        sampleRate = null,
    )

    private val getLucky = video(1, "/Movies/Daft Punk - Get Lucky.mp4")
    private val other = video(2, "/Movies/Nothing Matches This.mp4")
    private val matcher = LocalVideoMatcher(listOf(getLucky, other))

    private fun map(vararg songs: Song) =
        videoToSongMap(matcher, songs.toList(), VideoMatchingMode.AUTO, sameFolderOnly = false)

    @Test
    fun `track whose file name matches the video wins over an earlier tag-only match`() {
        val live = song("live", "/Music/Live/track07.flac", "Get Lucky", "Daft Punk")
        val studio = song("studio", "/Music/Daft Punk - Get Lucky.flac", "Get Lucky", "Daft Punk")
        assertEquals("studio", map(live, studio)[getLucky.id]?.id)
    }

    @Test
    fun `first tag-only match is kept when no file name matches`() {
        val a = song("a", "/Music/a.flac", "Get Lucky", "Daft Punk")
        val b = song("b", "/Music/b.flac", "Get Lucky", "Daft Punk")
        assertEquals("a", map(a, b)[getLucky.id]?.id)
    }

    @Test
    fun `videos with no matching track are left out`() {
        val result = map(song("x", "/Music/x.flac", "Unrelated", "Someone"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `empty video list gives an empty map`() {
        val result = videoToSongMap(
            LocalVideoMatcher(emptyList()),
            listOf(song("a", "/Music/a.flac", "Get Lucky", "Daft Punk")),
            VideoMatchingMode.AUTO,
            sameFolderOnly = false,
        )
        assertTrue(result.isEmpty())
    }
}
