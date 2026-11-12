package io.github.raghavsatyadev.openplayer.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.raghavsatyadev.openplayer.data.jellyfin.JellyfinRepository
import timber.log.Timber

class JellyfinSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val repository: JellyfinRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("JellyfinSyncWorker: Starting full sync")

        return try {
            repository.syncAllPlaylistsAndSongs()
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "JellyfinSyncWorker: Sync failed")
            Result.failure(workDataOf(ERROR_MESSAGE to e.message))
        }
    }

    companion object {
        const val WORK_NAME_ALL = "jellyfin_sync_all"
        const val ERROR_MESSAGE = "error_message"

        fun startAllSync() = OneTimeWorkRequestBuilder<JellyfinSyncWorker>()
            .build()
    }
}
