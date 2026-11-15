package io.github.raghavsatyadev.openplayer.data.worker

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager

class WorkManagerCloudInitialSync(private val workManager: WorkManager) : CloudInitialSync {
    override fun startJellyfin() {
        workManager.enqueueUniqueWork(
            JellyfinSyncWorker.WORK_NAME_ALL,
            ExistingWorkPolicy.KEEP,
            JellyfinSyncWorker.startAllSync()
        )
    }

    override fun startNavidrome() {
        workManager.enqueueUniqueWork(
            NavidromeSyncWorker.WORK_NAME_ALL,
            ExistingWorkPolicy.KEEP,
            NavidromeSyncWorker.startAllSync()
        )
    }
}
