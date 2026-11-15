package io.github.raghavsatyadev.openplayer.data.video

import co.touchlab.kermit.Logger
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.preferences.VideoPreferences
import io.github.raghavsatyadev.openplayer.data.preferences.VideoSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The device's video files, from MediaStore, kept in memory and matched to tracks.
 *
 * Nothing is stored in the database: MediaStore already has the duration and size, and the list
 * is rebuilt whenever MediaStore's videos change.
 */
class LocalVideoRepository(
    private val source: LocalVideoSource,
    private val videoPreferences: VideoPreferences,
    private val scope: CoroutineScope,
) {
    /** Every video on the device, before the folder settings. */
    private val _videos = MutableStateFlow<List<LocalVideo>>(emptyList())

    val settings: StateFlow<VideoSettings> = videoPreferences.settingsFlow
        .stateIn(scope, SharingStarted.Eagerly, VideoSettings())

    /** The videos in the folders the settings allow. */
    val videos: StateFlow<List<LocalVideo>> = combine(_videos, settings) { all, current ->
        VideoFolderFilter.apply(all, current.includedFolders, current.excludedFolders)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Folders that hold videos, for the folder picker. */
    val videoFolders: StateFlow<List<String>> = _videos
        .map(VideoFolderFilter::foldersOf)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val matcher: StateFlow<LocalVideoMatcher> = videos
        .map { LocalVideoMatcher(it) }
        .stateIn(scope, SharingStarted.Eagerly, LocalVideoMatcher(emptyList()))

    private val _hasPermission = MutableStateFlow(source.hasPermission())
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var observerRegistered = false

    init {
        @OptIn(FlowPreview::class)
        scope.launch {
            changes.debounce(1_000).collect { refresh() }
        }
        scope.launch { refresh() }
    }

    /** Call after the video permission is granted, or to rescan. */
    fun requestRefresh() {
        changes.tryEmit(Unit)
    }

    suspend fun refresh() {
        val granted = source.hasPermission()
        _hasPermission.value = granted
        if (!granted) {
            _videos.value = emptyList()
            return
        }
        if (!observerRegistered) {
            observerRegistered = true
            source.observeChanges { changes.tryEmit(Unit) }
        }
        _videos.value = withContext(Dispatchers.IO) {
            runCatching { source.queryVideos() }
                .onFailure { Logger.w(it) { "LocalVideoRepository: video query failed" } }
                .getOrDefault(_videos.value)
        }
    }

    /** Rescans now and returns every video, before the folder settings. Empty without permission. */
    suspend fun loadAll(): List<LocalVideo> {
        refresh()
        return _videos.value
    }

    /** The best local video for [song], using the current matching settings. */
    fun bestMatchFor(song: Song): LocalVideo? {
        if (song.path.isBlank()) return null
        val current = settings.value
        return matcher.value.bestMatchFor(song.toMatchInfo(), current.matchingMode, current.sameFolderOnly)
    }

    fun matchesFor(song: Song): List<LocalVideo> {
        if (song.path.isBlank()) return emptyList()
        val current = settings.value
        return matcher.value.matchesFor(song.toMatchInfo(), current.matchingMode, current.sameFolderOnly)
    }

    // Looks in every video, so a queued video still plays after its folder is excluded.
    fun videoById(id: Long): LocalVideo? = _videos.value.firstOrNull { it.id == id }

    /** The queue item for a `video:<id>` media id, or null for any other id. */
    fun videoSongFor(mediaId: String): Song? =
        VideoSongs.videoIdOf(mediaId)?.let(::videoById)?.let(VideoSongs::toSong)
}

/** First artist and first genre, as Namida matches by. */
internal fun Song.toMatchInfo(): TrackMatchInfo = TrackMatchInfo(
    path = path,
    title = title,
    artist = artists.firstOrNull { it.isPrimary }?.name ?: artists.firstOrNull()?.name ?: artist,
    genre = genre?.split(';', '/', ',', '\u0000')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() },
)
