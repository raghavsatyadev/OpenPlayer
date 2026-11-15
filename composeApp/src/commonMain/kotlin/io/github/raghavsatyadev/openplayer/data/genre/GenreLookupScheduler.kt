package io.github.raghavsatyadev.openplayer.data.genre

/** Queues or cancels the background online genre lookup (WorkManager on Android). */
interface GenreLookupScheduler {
    /** [replace] false keeps already queued work; true queues it again. */
    fun enqueue(wifiOnly: Boolean, replace: Boolean)
    fun cancel()
}
