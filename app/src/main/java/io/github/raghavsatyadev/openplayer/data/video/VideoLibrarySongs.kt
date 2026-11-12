package io.github.raghavsatyadev.openplayer.data.video

import io.github.raghavsatyadev.openplayer.data.database.MusicDao
import io.github.raghavsatyadev.openplayer.data.database.SourceType
import io.github.raghavsatyadev.openplayer.data.database.SongEntity
import io.github.raghavsatyadev.openplayer.data.preferences.VideoPreferences
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Adds music videos to the song library, so they show up in Songs, Artists, Albums and Search
 * and count in stats like any track.
 *
 * Only videos in the folders allowed by the video folder settings are added, and only while the
 * setting is on. A row's id is the video's MediaStore id (MediaStore ids are shared across audio
 * and video, so they never collide with tracks) and its content URI points at the video, which
 * the music player plays audio-only.
 */
class VideoLibrarySongs(
    private val videoRepository: LocalVideoRepository,
    private val videoPreferences: VideoPreferences,
    private val musicDao: MusicDao,
) {
    /** Ids of the videos that belong in the library now. Rows not in here get removed. */
    suspend fun currentIds(): Set<Long> = allowedVideos().mapTo(HashSet()) { it.id }

    /**
     * Rows to save: every allowed video when [rebuild] is set (the library is rebuilt from
     * scratch), otherwise only videos that have no row yet.
     */
    suspend fun entitiesToAdd(rebuild: Boolean): List<SongEntity> {
        val videos = allowedVideos()
        if (videos.isEmpty()) return emptyList()
        val existing = if (rebuild) emptySet() else musicDao.getAllMediaStoreSongIds().toHashSet()
        return videos.filter { it.id !in existing }.map(::toEntity)
    }

    private suspend fun allowedVideos(): List<LocalVideo> {
        val settings = videoPreferences.settingsFlow.first()
        if (!settings.addToLibrary) return emptyList()
        return VideoFolderFilter.apply(
            videoRepository.loadAll(),
            settings.includedFolders,
            settings.excludedFolders,
        )
    }

    private fun toEntity(video: LocalVideo): SongEntity {
        val song = VideoSongs.toSong(video)
        val folderPath = VideoNameText.directoryOf(video.path)
        return SongEntity(
            id = video.id,
            title = song.title,
            artistName = song.artist,
            artistId = -1L,
            albumName = song.album,
            albumId = albumIdFor(folderPath),
            contentUriString = video.contentUri,
            albumArtUriString = video.posterUri,
            duration = video.durationMs,
            genre = null,
            filePath = video.path,
            parentDirectoryPath = folderPath,
            dateAdded = TimeUnit.SECONDS.toMillis(video.dateAddedSec).takeIf { it > 0 }
                ?: System.currentTimeMillis(),
            mimeType = video.mimeType ?: "video/mp4",
            sourceType = SourceType.LOCAL,
            mediaStoreDateAdded = video.dateAddedSec,
            mediaStoreDateModified = video.dateAddedSec,
        )
    }

    /** One album per folder, with ids far above MediaStore's so they never clash. */
    private fun albumIdFor(folderPath: String): Long =
        ALBUM_ID_BASE + (folderPath.lowercase().hashCode().toLong() and 0x7fffffffL)

    private companion object {
        const val ALBUM_ID_BASE = 9_000_000_000_000L
    }
}
