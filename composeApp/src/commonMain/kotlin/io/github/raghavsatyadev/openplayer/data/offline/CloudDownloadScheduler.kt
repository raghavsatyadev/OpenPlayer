package io.github.raghavsatyadev.openplayer.data.offline

/** Runs cloud track downloads in the background (Android: WorkManager). */
interface CloudDownloadScheduler {
    /** Replaces any queued or running download with the same [downloadId]. */
    suspend fun enqueue(downloadId: String, attemptId: String, sourceUri: String)

    suspend fun cancel(downloadId: String)
}
