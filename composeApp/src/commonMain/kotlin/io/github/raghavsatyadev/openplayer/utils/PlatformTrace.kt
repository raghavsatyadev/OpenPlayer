package io.github.raghavsatyadev.openplayer.utils

/** System tracing and the monotonic clock: android.os.Trace / SystemClock on Android, no-op / nanoTime on desktop. */
expect object PlatformTrace {
    fun beginSection(name: String)
    fun endSection()
    /** Milliseconds since an arbitrary fixed point (boot on Android); only differences are meaningful. */
    fun elapsedRealtimeMs(): Long
}
