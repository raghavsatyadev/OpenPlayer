package io.github.raghavsatyadev.openplayer.utils

/**
 * Returns monotonic ms; on Android SystemClock.elapsedRealtime, counts deep sleep.
 */
expect fun elapsedRealtimeMs(): Long
