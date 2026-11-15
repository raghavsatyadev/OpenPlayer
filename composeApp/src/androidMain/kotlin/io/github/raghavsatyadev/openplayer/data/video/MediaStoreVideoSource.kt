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
import androidx.core.content.ContextCompat

/** Reads video files and their "<video name>-poster" images from MediaStore. */
class MediaStoreVideoSource(private val context: Context) : LocalVideoSource {
    override fun hasPermission(): Boolean = hasVideoPermission(context)

    override fun observeChanges(onChange: () -> Unit) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = onChange()
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer
        )
    }

    override fun queryVideos(): List<LocalVideo> {
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
