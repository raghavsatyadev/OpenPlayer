package io.github.raghavsatyadev.openplayer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil.ImageLoader
import coil.ImageLoaderFactory
import io.github.raghavsatyadev.openplayer.data.diagnostics.AdvancedPerformanceDiagnosticsController
import io.github.raghavsatyadev.openplayer.data.media.SongMetadataEditor
import io.github.raghavsatyadev.openplayer.data.playlist.M3uSyncCoordinator
import io.github.raghavsatyadev.openplayer.data.preferences.UserPreferencesRepository
import io.github.raghavsatyadev.openplayer.data.repository.ArtistImageRepository
import io.github.raghavsatyadev.openplayer.di.allModules
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.LibraryStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ThemeStateHolder
import io.github.raghavsatyadev.openplayer.utils.AlbumArtCacheManager
import io.github.raghavsatyadev.openplayer.utils.AppLocaleManager
import io.github.raghavsatyadev.openplayer.utils.CrashHandler
import io.github.raghavsatyadev.openplayer.utils.FolderArtworkSettingsCoordinator
import io.github.raghavsatyadev.openplayer.utils.MediaMetadataRetrieverPool
import io.github.raghavsatyadev.openplayer.utils.PhoneOrientationPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import timber.log.Timber

class OpenPlayerApplication : Application(), ImageLoaderFactory, KoinComponent {

    private val imageLoader: ImageLoader by inject()

    private val navidromeCoilFetcherFactory: io.github.raghavsatyadev.openplayer.data.image.NavidromeCoilFetcher.Factory by inject()

    private val jellyfinCoilFetcherFactory: io.github.raghavsatyadev.openplayer.data.image.JellyfinCoilFetcher.Factory by inject()

    private val localArtworkCoilFetcherFactory: io.github.raghavsatyadev.openplayer.data.image.LocalArtworkCoilFetcher.Factory by inject()

    private val themeStateHolder: ThemeStateHolder by inject()

    private val artistImageRepository: ArtistImageRepository by inject()

    private val libraryStateHolder: LibraryStateHolder by inject()

    private val userPreferencesRepository: UserPreferencesRepository by inject()

    private val folderArtworkSettingsCoordinator: FolderArtworkSettingsCoordinator by inject()

    private val syncManager: io.github.raghavsatyadev.openplayer.data.worker.SyncManager by inject()

    private val advancedPerformanceDiagnosticsController: AdvancedPerformanceDiagnosticsController by inject()

    private val m3uSyncCoordinator: M3uSyncCoordinator by inject()

    private val updateCheckScheduler: io.github.raghavsatyadev.openplayer.data.update.UpdateCheckScheduler by inject()

    private val songMetadataEditor: SongMetadataEditor by inject()

    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "openplayer_music_channel"
    }

    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            libraryStateHolder.restoreAfterTrimIfNeeded()
            m3uSyncCoordinator.onAppForeground()
            advancedPerformanceDiagnosticsController.onAppForeground()
            // Gives songs cut off by an interrupted tag save (a failed write-back, or the app
            // being killed mid-write) their edited audio back, and lists the ones the app can no
            // longer write to so the player can ask the user for access.
            startupScope.launch { songMetadataEditor.resumeInterruptedWriteBacks() }
        }

        override fun onResume(owner: LifecycleOwner) {
            // Image access can change while system settings is open, without restarting us.
            startupScope.launch { folderArtworkSettingsCoordinator.reconcile() }
        }

        override fun onStop(owner: LifecycleOwner) {
            advancedPerformanceDiagnosticsController.onAppBackground()
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocaleManager.wrapContext(base))
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@OpenPlayerApplication)
            workManagerFactory()
            modules(allModules)
        }

        if (BuildConfig.BUILD_TYPE != "benchmark") {
            CrashHandler.install(this)
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "OpenPlayer Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

        registerActivityLifecycleCallbacks(PhoneOrientationPolicy)

        syncManager.start()

        m3uSyncCoordinator.start()

        updateCheckScheduler.start()

        advancedPerformanceDiagnosticsController.start(startupScope)

        startupScope.launch {
            val savedLimit = runCatching {
                userPreferencesRepository.albumArtCacheLimitMbFlow.first()
            }.getOrNull()
            if (savedLimit != null) {
                AlbumArtCacheManager.configuredCacheLimitMb = savedLimit.toLong()
            }
        }

        // Collected rather than read once: the preference can also change underneath us, most
        // notably when a backup restore writes it straight into DataStore. A stale mirror would
        // leave the toggle reporting one thing while artwork resolution did another.
        startupScope.launch {
            userPreferencesRepository.useFolderAlbumArtFlow.collect {
                folderArtworkSettingsCoordinator.reconcile()
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return imageLoader.newBuilder()
            .components {
                add(io.github.raghavsatyadev.openplayer.data.image.SharedCloudArtworkStringMapper(packageName))
                add(io.github.raghavsatyadev.openplayer.data.image.SharedCloudArtworkUriMapper(packageName))
                add(localArtworkCoilFetcherFactory)
                add(navidromeCoilFetcherFactory)
                add(jellyfinCoilFetcherFactory)
            }
            .build()
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        imageLoader.memoryCache?.trimMemory(level)

        if (
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE ||
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
            level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        ) {
            themeStateHolder.trimMemory(level)
        }

        if (
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
            level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        ) {
            artistImageRepository.clearCache()
            MediaMetadataRetrieverPool.clear()
        }

        libraryStateHolder.trimMemory(level)

        if (
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE
        ) {
            imageLoader.memoryCache?.clear()
        }
    }
}
