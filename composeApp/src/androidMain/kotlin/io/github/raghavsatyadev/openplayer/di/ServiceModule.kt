package io.github.raghavsatyadev.openplayer.di

import io.github.raghavsatyadev.openplayer.data.DailyMixManager
import io.github.raghavsatyadev.openplayer.data.diagnostics.AdvancedPerformanceDiagnosticsController
import io.github.raghavsatyadev.openplayer.data.diagnostics.DebugPerformanceReportCollector
import io.github.raghavsatyadev.openplayer.data.diagnostics.MainThreadStallMonitor
import io.github.raghavsatyadev.openplayer.data.diagnostics.StallMonitor
import io.github.raghavsatyadev.openplayer.data.equalizer.EqualizerManager
import io.github.raghavsatyadev.openplayer.data.equalizer.ExternalAudioEffectSession
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricEqPreferences
import io.github.raghavsatyadev.openplayer.data.history.importer.ExternalHistoryImporter
import io.github.raghavsatyadev.openplayer.data.jellyfin.JellyfinStreamProxy
import io.github.raghavsatyadev.openplayer.data.listenbrainz.ScrobbleManager
import io.github.raghavsatyadev.openplayer.data.media.ImageCacheManager
import io.github.raghavsatyadev.openplayer.data.media.MediaControllerFactory
import io.github.raghavsatyadev.openplayer.data.media.MediaMapper
import io.github.raghavsatyadev.openplayer.data.media.ReplayGainManager
import io.github.raghavsatyadev.openplayer.data.navidrome.NavidromeStreamProxy
import io.github.raghavsatyadev.openplayer.data.observer.MediaStoreObserver
import io.github.raghavsatyadev.openplayer.data.playlist.M3uSyncCoordinator
import io.github.raghavsatyadev.openplayer.data.service.SleepTimerSession
import io.github.raghavsatyadev.openplayer.data.service.player.DualPlayerEngine
import io.github.raghavsatyadev.openplayer.data.service.player.TransitionController
import io.github.raghavsatyadev.openplayer.data.service.repeatn.RepeatNSession
import io.github.raghavsatyadev.openplayer.data.update.AppReleaseSource
import io.github.raghavsatyadev.openplayer.data.update.AppUpdateManager
import io.github.raghavsatyadev.openplayer.data.update.UpdateCheckScheduler
import io.github.raghavsatyadev.openplayer.data.worker.CloudSyncCoordinator
import io.github.raghavsatyadev.openplayer.data.worker.LocalLibraryChangeTracker
import io.github.raghavsatyadev.openplayer.data.worker.SyncManager
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ColorSchemeProcessor
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ListeningStatsTracker
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val serviceModule = module {
    singleOf(::DualPlayerEngine)
    singleOf(::TransitionController)
    singleOf(::EqualizerManager)
    singleOf(::ParametricEqPreferences)
    singleOf(::ExternalAudioEffectSession)
    singleOf(::ColorSchemeProcessor)
    singleOf(::ReplayGainManager)
    singleOf(::SleepTimerSession)
    singleOf(::RepeatNSession)
    singleOf(::ListeningStatsTracker)
    singleOf(::DailyMixManager)
    single<StallMonitor> { MainThreadStallMonitor() }
    singleOf(::AdvancedPerformanceDiagnosticsController)
    singleOf(::DebugPerformanceReportCollector)
    singleOf(::M3uSyncCoordinator)
    singleOf(::UpdateCheckScheduler)
    singleOf(::AppUpdateManager)
    singleOf(::AppReleaseSource)
    singleOf(::CloudSyncCoordinator)
    singleOf(::SyncManager)
    singleOf(::LocalLibraryChangeTracker)
    singleOf(::MediaStoreObserver)
    singleOf(::NavidromeStreamProxy)
    singleOf(::JellyfinStreamProxy)
    singleOf(::ScrobbleManager)
    singleOf(::MediaMapper)
    singleOf(::MediaControllerFactory)
    singleOf(::ImageCacheManager)
    singleOf(::ExternalHistoryImporter)
}
