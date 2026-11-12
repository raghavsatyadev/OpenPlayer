package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.preferences.VideoPreferences
import io.github.raghavsatyadev.openplayer.data.repository.MusicRepository
import io.github.raghavsatyadev.openplayer.data.video.LocalVideo
import io.github.raghavsatyadev.openplayer.data.video.LocalVideoRepository
import io.github.raghavsatyadev.openplayer.data.video.VideoSongs
import io.github.raghavsatyadev.openplayer.data.video.videoToSongMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class VideoRow(val video: LocalVideo, val song: Song?, val librarySong: Song? = null) {
    /** What tapping the row plays: the matched track, the video's library row, or the video on its own. */
    val playable: Song = song ?: librarySong ?: VideoSongs.toSong(video)
}

@Immutable
data class VideosTabState(
    val hasPermission: Boolean = true,
    val rows: ImmutableList<VideoRow> = persistentListOf(),
    val isLoaded: Boolean = false,
) {
    val playables: ImmutableList<Song> get() = rows.map { it.playable }.distinctBy { it.id }.toImmutableList()
}

/** The Videos library tab: every local video and the track it matches, if any. */
@HiltViewModel
class VideosLibraryViewModel @Inject constructor(
    private val videoRepository: LocalVideoRepository,
    private val videoPreferences: VideoPreferences,
    musicRepository: MusicRepository,
) : ViewModel() {

    val state: StateFlow<VideosTabState> = combine(
        videoRepository.matcher,
        videoRepository.settings,
        videoRepository.hasPermission,
        musicRepository.getAudioFiles(),
    ) { matcher, settings, hasPermission, songs ->
        if (!hasPermission) return@combine VideosTabState(hasPermission = false, isLoaded = true)
        // Videos added to the library are their own song; the rest are matched to audio tracks.
        val (videoSongs, audioSongs) = songs.partition(VideoSongs::isVideo)
        val ownSong = videoSongs.associateBy { VideoSongs.videoIdOf(it) }
        val songByVideo = videoToSongMap(matcher, audioSongs, settings.matchingMode, settings.sameFolderOnly)
        val rows = matcher.videos
            .sortedBy { it.displayName.lowercase() }
            .map { VideoRow(it, songByVideo[it.id], ownSong[it.id]) }
        VideosTabState(hasPermission = true, rows = rows.toImmutableList(), isLoaded = true)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VideosTabState())

    fun onPermissionResult() = videoRepository.requestRefresh()

    /** Opening a video from this tab turns video on in the player. */
    fun showVideoInPlayer() {
        viewModelScope.launch { videoPreferences.setShowInPlayer(true) }
    }
}
