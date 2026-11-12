package io.github.raghavsatyadev.openplayer.data.offline

import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import io.github.raghavsatyadev.openplayer.data.worker.CloudTrackDownloadWorker

class WorkManagerCloudDownloadScheduler(
    private val workManager: WorkManager
) : CloudDownloadScheduler {

    override suspend fun enqueue(downloadId: String, attemptId: String, sourceUri: String) {
        val request = OneTimeWorkRequestBuilder<CloudTrackDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .setInputData(
                workDataOf(
                    CloudTrackDownloadWorker.KEY_DOWNLOAD_ID to downloadId,
                    CloudTrackDownloadWorker.KEY_ATTEMPT_ID to attemptId,
                    CloudTrackDownloadWorker.KEY_SOURCE_URI to sourceUri
                )
            )
            .addTag(CloudTrackDownloadWorker.TAG)
            .addTag(CloudOfflineRepository.workName(downloadId))
            .build()
        workManager.enqueueUniqueWork(
            CloudOfflineRepository.workName(downloadId),
            ExistingWorkPolicy.REPLACE,
            request
        ).await()
    }

    override suspend fun cancel(downloadId: String) {
        workManager.cancelUniqueWork(CloudOfflineRepository.workName(downloadId)).await()
    }
}

/** Offline copy of a cloud track as a `file://` Uri for ExoPlayer, or null when there is none. */
suspend fun CloudOfflineRepository.resolveLocalUri(sourceUri: String): Uri? =
    resolveLocalFile(sourceUri)?.let(Uri::fromFile)
