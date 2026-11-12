package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import io.github.raghavsatyadev.openplayer.data.preferences.VideoPreferences
import io.github.raghavsatyadev.openplayer.data.video.LocalVideo
import io.github.raghavsatyadev.openplayer.data.video.LocalVideoRepository
import io.github.raghavsatyadev.openplayer.data.video.VideoSongs
import io.github.raghavsatyadev.openplayer.data.video.VideoSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Plays the local video that matches the current track, muted, in step with the audio.
 *
 * The video has its own ExoPlayer in the app process. It never touches the audio player: it reads
 * the audio's position, play state and speed and follows them. The player exists only while a
 * video surface is on screen ([attach]/[detach]).
 */
class PlayerVideoViewModel(
    private val context: Context,
    private val playbackStateHolder: PlaybackStateHolder,
    private val videoRepository: LocalVideoRepository,
    private val videoPreferences: VideoPreferences,
) : ViewModel() {

    private val currentSongId = playbackStateHolder.stablePlayerState
        .map { it.currentSong }
        .distinctUntilChanged { a, b -> a?.id == b?.id && a?.contentUriString == b?.contentUriString }

    /**
     * The video for the current item: the item itself when it is a video played on its own,
     * otherwise the best match for the track, whether or not video is turned on.
     */
    val matchedVideo: StateFlow<LocalVideo?> = combine(
        currentSongId,
        videoRepository.matcher,
        videoRepository.settings,
    ) { song, _, _ ->
        when {
            song == null -> null
            VideoSongs.isVideo(song) -> VideoSongs.videoIdOf(song)?.let(videoRepository::videoById)
            else -> videoRepository.bestMatchFor(song)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The current item is a video playing its own sound, so it always shows its picture. */
    val isOwnVideo: StateFlow<Boolean> = currentSongId
        .map { VideoSongs.isVideo(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val showInPlayer: StateFlow<Boolean> = videoRepository.settings
        .map { it.showInPlayer }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val hasPermission: StateFlow<Boolean> = videoRepository.hasPermission

    /** A video is what the player shows right now (used for the cover and picture-in-picture). */
    val videoOnScreen: StateFlow<LocalVideo?> = combine(
        matchedVideo, isOwnVideo, showInPlayer, hasPermission,
    ) { video, own, show, granted -> video?.takeIf { granted && (own || show) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _inPictureInPicture = MutableStateFlow(false)
    /** The app is shrunk to the corner window; only the video is drawn. */
    val inPictureInPicture: StateFlow<Boolean> = _inPictureInPicture.asStateFlow()

    fun setInPictureInPicture(inPip: Boolean) {
        _inPictureInPicture.value = inPip
    }

    private val _fullscreen = MutableStateFlow(false)
    /** The video fills the whole screen, with its own controls. */
    val fullscreen: StateFlow<Boolean> = _fullscreen.asStateFlow()

    fun setFullscreen(on: Boolean) {
        _fullscreen.value = on
    }

    private val _videoAspect = MutableStateFlow<Float?>(null)
    /** Width over height of the picture being shown, once the player knows it. */
    val videoAspect: StateFlow<Float?> = _videoAspect.asStateFlow()

    /** The music player, for the fullscreen controls. */
    val audioPlayer: Player? get() = playbackStateHolder.mediaController

    val playerState = playbackStateHolder.stablePlayerState

    private val _player = MutableStateFlow<ExoPlayer?>(null)
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()

    private var loadedVideoId: Long? = null
    private var syncJob: Job? = null
    private var attachCount = 0

    fun setShowInPlayer(show: Boolean) {
        viewModelScope.launch { videoPreferences.setShowInPlayer(show) }
    }

    /** Call when the video permission result comes back. */
    fun onPermissionResult() {
        videoRepository.requestRefresh()
    }

    /** Start the video player. Each call needs a matching [detach]; the last one releases it. */
    fun attach() {
        attachCount++
        if (_player.value != null) return
        _player.value = createPlayer()
        syncJob = viewModelScope.launch {
            launch { matchedVideo.collect { load(it) } }
            while (isActive) {
                syncOnce()
                delay(if (isOwnVideo.value) LIP_SYNC_INTERVAL_MS else SYNC_INTERVAL_MS)
            }
        }
    }

    /** Stop and release the video player. */
    fun detach() {
        attachCount = (attachCount - 1).coerceAtLeast(0)
        if (attachCount > 0) return
        syncJob?.cancel()
        syncJob = null
        loadedVideoId = null
        _player.value?.release()
        _player.value = null
    }

    @OptIn(UnstableApi::class)
    private fun createPlayer(): ExoPlayer = ExoPlayer.Builder(context)
        // Never take audio focus from the music.
        .setAudioAttributes(androidx.media3.common.AudioAttributes.DEFAULT, false)
        .setHandleAudioBecomingNoisy(false)
        // Local files load fast; a short buffer keeps big videos from filling the heap.
        .setLoadControl(
            androidx.media3.exoplayer.DefaultLoadControl.Builder()
                .setBufferDurationsMs(10_000, 15_000, 1_000, 2_000)
                .setTargetBufferBytes(32 * 1024 * 1024)
                .setPrioritizeTimeOverSizeThresholds(false)
                .build()
        )
        .build()
        .apply {
            volume = 0f
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        _videoAspect.value =
                            videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                    }
                }
            })
        }

    private fun load(video: LocalVideo?) {
        val player = _player.value ?: return
        if (video?.id == loadedVideoId) return
        loadedVideoId = video?.id
        _videoAspect.value = video?.takeIf { it.width > 0 && it.height > 0 }
            ?.let { it.width.toFloat() / it.height }
        if (video == null) {
            player.stop()
            player.clearMediaItems()
            return
        }
        player.setMediaItem(MediaItem.fromUri(video.contentUri.toUri()))
        player.prepare()
        syncOnce()
    }

    private fun syncOnce() {
        val player = _player.value ?: return
        val audio = playbackStateHolder.mediaController ?: return
        val video = matchedVideo.value ?: return
        if (player.mediaItemCount == 0) return

        val trackDuration = audio.duration.takeIf { it > 0 } ?: video.durationMs
        val videoDuration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: video.durationMs
        val loop = VideoSync.canLoop(videoDuration, trackDuration)

        val audioSpeed = audio.playbackParameters.speed
        val decision = if (isOwnVideo.value) {
            VideoSync.decideLipSync(audio.currentPosition, player.currentPosition, videoDuration)
        } else {
            VideoSync.decide(audio.currentPosition, player.currentPosition, videoDuration, loop)
        }
        val speed = audioSpeed * ((decision as? VideoSync.Decision.Nudge)?.speedFactor ?: 1f)
        if (player.playbackParameters.speed != speed) player.setPlaybackSpeed(speed)

        when (decision) {
            VideoSync.Decision.InSync, is VideoSync.Decision.Nudge -> player.playWhenReady = audio.isPlaying
            is VideoSync.Decision.SeekTo -> {
                player.seekTo(decision.positionMs)
                player.playWhenReady = audio.isPlaying
            }
            VideoSync.Decision.HoldAtEnd -> {
                player.playWhenReady = false
                if (videoDuration > 0 && player.currentPosition < videoDuration - END_HOLD_MS) {
                    player.seekTo(videoDuration - END_HOLD_MS)
                }
            }
        }
    }

    override fun onCleared() {
        attachCount = 1
        detach()
        super.onCleared()
    }

    private companion object {
        const val SYNC_INTERVAL_MS = 500L
        const val LIP_SYNC_INTERVAL_MS = 150L
        const val END_HOLD_MS = 50L
    }
}
