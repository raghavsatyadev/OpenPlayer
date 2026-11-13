package io.github.raghavsatyadev.openplayer.utils

actual fun elapsedRealtimeMs(): Long = System.nanoTime() / 1_000_000L
