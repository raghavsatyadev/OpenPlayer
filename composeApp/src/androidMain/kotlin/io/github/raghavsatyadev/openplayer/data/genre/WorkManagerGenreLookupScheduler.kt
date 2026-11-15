package io.github.raghavsatyadev.openplayer.data.genre

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager

class WorkManagerGenreLookupScheduler(private val workManager: WorkManager) : GenreLookupScheduler {
    override fun enqueue(wifiOnly: Boolean, replace: Boolean) {
        GenreLookupWorker.enqueue(
            workManager,
            wifiOnly,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
        )
    }

    override fun cancel() {
        GenreLookupWorker.cancel(workManager)
    }
}
