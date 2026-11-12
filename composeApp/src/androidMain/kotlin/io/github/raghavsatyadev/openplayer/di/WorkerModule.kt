package io.github.raghavsatyadev.openplayer.di

import io.github.raghavsatyadev.openplayer.data.genre.GenreLookupWorker
import io.github.raghavsatyadev.openplayer.data.playlist.M3uSyncWorker
import io.github.raghavsatyadev.openplayer.data.update.UpdateCheckWorker
import io.github.raghavsatyadev.openplayer.data.worker.CloudTrackDownloadWorker
import io.github.raghavsatyadev.openplayer.data.worker.JellyfinSyncWorker
import io.github.raghavsatyadev.openplayer.data.worker.NavidromeSyncWorker
import io.github.raghavsatyadev.openplayer.data.worker.ScrobbleFlushWorker
import io.github.raghavsatyadev.openplayer.data.worker.SyncWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val workerModule = module {
    workerOf(::GenreLookupWorker)
    workerOf(::M3uSyncWorker)
    workerOf(::UpdateCheckWorker)
    workerOf(::CloudTrackDownloadWorker)
    workerOf(::JellyfinSyncWorker)
    workerOf(::NavidromeSyncWorker)
    workerOf(::ScrobbleFlushWorker)
    workerOf(::SyncWorker)
}
