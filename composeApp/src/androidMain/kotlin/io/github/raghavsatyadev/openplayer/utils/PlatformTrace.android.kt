package io.github.raghavsatyadev.openplayer.utils

import android.os.SystemClock
import android.os.Trace

actual object PlatformTrace {
    actual fun beginSection(name: String) = Trace.beginSection(name)
    actual fun endSection() = Trace.endSection()
    actual fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
}
