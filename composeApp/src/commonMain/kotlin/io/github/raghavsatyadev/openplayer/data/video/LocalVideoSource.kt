package io.github.raghavsatyadev.openplayer.data.video

/** Where [LocalVideoRepository] reads the device's video files from. Android uses MediaStore. */
interface LocalVideoSource {
    /** Whether the app may read video files. */
    fun hasPermission(): Boolean

    /** Every video file, with posters attached. Blocking; call it off the main thread. */
    fun queryVideos(): List<LocalVideo>

    /** Calls [onChange] whenever the videos change. Called once, after permission is granted. */
    fun observeChanges(onChange: () -> Unit)
}
