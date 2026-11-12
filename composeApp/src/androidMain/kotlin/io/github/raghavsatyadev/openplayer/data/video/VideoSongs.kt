package io.github.raghavsatyadev.openplayer.data.video

import android.provider.MediaStore
import io.github.raghavsatyadev.openplayer.data.model.Song

/**
 * A video with no matching track, played as its own queue item with the video's own sound.
 *
 * The music service plays the file like any track (audio only); the player shows the picture
 * from [io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerVideoViewModel], kept in sync.
 * These items use their own id space, `video:<MediaStore id>`, so they never collide with songs.
 */
object VideoSongs {
    private const val ID_PREFIX = "video:"

    fun isVideoSongId(id: String?): Boolean = id?.startsWith(ID_PREFIX) == true

    fun videoIdOf(songId: String?): Long? =
        songId?.takeIf { it.startsWith(ID_PREFIX) }?.removePrefix(ID_PREFIX)?.toLongOrNull()

    /**
     * The MediaStore video id behind [song]: a `video:<id>` item, or a library row whose file is
     * a video (added by [VideoLibrarySongs]). Null for audio tracks.
     */
    fun videoIdOf(song: Song?): Long? {
        if (song == null) return null
        videoIdOf(song.id)?.let { return it }
        val uri = song.contentUriString
        if (!uri.startsWith(VIDEO_URI_PREFIX)) return null
        return uri.removePrefix(VIDEO_URI_PREFIX).substringBefore('?').toLongOrNull()
    }

    fun isVideo(song: Song?): Boolean = videoIdOf(song) != null

    private val VIDEO_URI_PREFIX = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString() + "/"

    fun idFor(video: LocalVideo): String = ID_PREFIX + video.id

    fun toSong(video: LocalVideo): Song {
        val name = VideoTitleParser.parse(VideoNameText.filenameWithoutExtension(video.path))
        val folder = VideoNameText.directoryOf(video.path).substringAfterLast('/')
        val artist = name.artist ?: folder
        return Song(
            id = idFor(video),
            title = name.title,
            artist = artist,
            artistId = -1L,
            album = folder,
            albumId = -1L,
            path = video.path,
            contentUriString = video.contentUri,
            albumArtUriString = video.posterUri,
            duration = video.durationMs,
            dateAdded = video.dateAddedSec,
            mimeType = video.mimeType ?: "video/mp4",
            bitrate = null,
            sampleRate = null,
        )
    }
}

/** Turns a downloaded video's file name into a title and, when it has one, an artist. */
internal object VideoTitleParser {
    data class Parsed(val title: String, val artist: String?)

    // Noise that video sites and downloaders add to file names.
    private val noise = listOf(
        Regex("""\s*-\s*YouTube\b""", RegexOption.IGNORE_CASE),
        Regex("""\s*[(\[]\s*\d{3,4}p\d*\s*[)\]]""", RegexOption.IGNORE_CASE),
        Regex(
            """\s*[(\[][^)\]]*(official|video|audio|lyrics?|visuali[sz]er|hd|4k)[^)\]]*[)\]]""",
            RegexOption.IGNORE_CASE,
        ),
    )

    fun parse(fileName: String): Parsed {
        var text = fileName.replace('_', ' ')
        noise.forEach { text = it.replace(text, "") }
        text = text.replace(Regex("""\s{2,}"""), " ").trim().trim('-', ' ')
        if (text.isEmpty()) return Parsed(fileName, null)

        // "Title - Artist" (the BYS style) when there is exactly one separator.
        val parts = text.split(" - ")
        if (parts.size == 2 && parts.all { it.isNotBlank() && it.length <= MAX_PART }) {
            return Parsed(title = parts[0].trim(), artist = parts[1].trim())
        }
        return Parsed(title = text, artist = null)
    }

    private const val MAX_PART = 60
}
