package io.github.raghavsatyadev.openplayer.utils

actual object PlatformTrace {
    actual fun beginSection(name: String) = Unit
    actual fun endSection() = Unit
    actual fun elapsedRealtimeMs(): Long = System.nanoTime() / 1_000_000
}
