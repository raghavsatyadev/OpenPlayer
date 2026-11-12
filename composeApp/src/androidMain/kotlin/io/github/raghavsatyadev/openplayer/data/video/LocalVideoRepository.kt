package io.github.raghavsatyadev.openplayer.data.video

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
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
    private val context: Context,
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

    private val _hasPermission = MutableStateFlow(hasVideoPermission(context))
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var observerRegistered = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            changes.tryEmit(Unit)
        }
    }

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
        val granted = hasVideoPermission(context)
        _hasPermission.value = granted
        if (!granted) {
            _videos.value = emptyList()
            return
        }
        registerObserverOnce()
        _videos.value = withContext(Dispatchers.IO) {
            runCatching { queryVideos() }
                .onFailure { Log.w(TAG, "Video query failed", it) }
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

    private fun registerObserverOnce() {
        if (observerRegistered) return
        observerRegistered = true
        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer
        )
    }

    private fun queryVideos(): List<LocalVideo> {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
        )
        val posters = queryPosters()
        val result = mutableListOf<LocalVideo>()
        context.contentResolver.query(collection, projection, null, null, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dataCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val widthCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            while (c.moveToNext()) {
                val path = c.getString(dataCol) ?: continue
                val id = c.getLong(idCol)
                result += LocalVideo(
                    id = id,
                    path = path,
                    contentUri = ContentUris.withAppendedId(collection, id).toString(),
                    displayName = c.getString(nameCol) ?: path.substringAfterLast('/'),
                    durationMs = c.getLong(durationCol),
                    width = c.getInt(widthCol),
                    height = c.getInt(heightCol),
                    sizeBytes = c.getLong(sizeCol),
                    dateAddedSec = c.getLong(dateCol),
                    mimeType = c.getString(mimeCol),
                    posterUri = posters[path.substringBeforeLast('.') + POSTER_SUFFIX],
                )
            }
        }
        return result
    }

    /** "<video name>-poster.jpg" images, by path without extension, as content URIs. */
    private fun queryPosters(): Map<String, String> = runCatching {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA)
        val posters = HashMap<String, String>()
        context.contentResolver.query(
            collection, projection,
            "${MediaStore.Images.Media.DATA} LIKE ?", arrayOf("%$POSTER_SUFFIX.%"), null,
        )?.use { c ->
            while (c.moveToNext()) {
                val path = c.getString(1) ?: continue
                posters[path.substringBeforeLast('.')] =
                    ContentUris.withAppendedId(collection, c.getLong(0)).toString()
            }
        }
        posters
    }.getOrDefault(emptyMap())

    companion object {
        private const val TAG = "LocalVideoRepository"
        private const val POSTER_SUFFIX = "-poster"

        /** The permission that lets the app read video files on this Android version. */
        val videoPermission: String
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_VIDEO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        /** Full access, or Android 14's "selected photos and videos" partial access. */
        fun hasVideoPermission(context: Context): Boolean {
            fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
            if (granted(videoPermission)) return true
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        }
    }
}

/** First artist and first genre, as Namida matches by. */
internal fun Song.toMatchInfo(): TrackMatchInfo = TrackMatchInfo(
    path = path,
    title = title,
    artist = artists.firstOrNull { it.isPrimary }?.name ?: artists.firstOrNull()?.name ?: artist,
    genre = genre?.split(';', '/', ',', '\u0000')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() },
)
