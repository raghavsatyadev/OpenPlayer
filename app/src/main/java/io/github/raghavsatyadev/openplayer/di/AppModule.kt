package io.github.raghavsatyadev.openplayer.di

import android.content.ComponentName
import androidx.annotation.OptIn
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.SessionToken
import androidx.work.WorkManager
import com.google.gson.Gson
import io.github.raghavsatyadev.openplayer.BuildConfig
import io.github.raghavsatyadev.openplayer.OpenPlayerApplication
import io.github.raghavsatyadev.openplayer.data.preferences.dataStore
import io.github.raghavsatyadev.openplayer.data.service.MusicService
import io.github.raghavsatyadev.openplayer.data.media.SongMetadataEditor
import io.github.raghavsatyadev.openplayer.data.playlist.M3uSyncCoordinator
import io.github.raghavsatyadev.openplayer.data.update.UpdateCheckScheduler
import io.github.raghavsatyadev.openplayer.utils.AppShortcutManager
import io.github.raghavsatyadev.openplayer.utils.FolderArtworkSettingsCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

@OptIn(UnstableApi::class)
val appModule = module {
    single { androidContext() as OpenPlayerApplication }
    single { Gson() }
    single {
        SessionToken(
            androidContext(),
            ComponentName(androidContext(), MusicService::class.java)
        )
    }
    single<DataStore<Preferences>> { androidContext().dataStore }
    single {
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
    singleOf(::DefaultDispatcherProvider)
    single<DispatcherProvider> { get<DefaultDispatcherProvider>() }
    single(QUALIFIER_APP_SCOPE) { CoroutineScope(SupervisorJob() + get<DispatcherProvider>().io) }
    single<CoroutineScope> { get(QUALIFIER_APP_SCOPE) }
    single { WorkManager.getInstance(androidContext()) }
    singleOf(::AppShortcutManager)
    singleOf(::SongMetadataEditor)
    single { M3uSyncCoordinator(get(), get(), get(), get(QUALIFIER_APP_SCOPE)) }
    single { UpdateCheckScheduler(get(), get(), get(QUALIFIER_APP_SCOPE)) }
    single {
        FolderArtworkSettingsCoordinator(
            context = androidContext(),
            preferences = get(),
            imageCacheManager = get(),
            artistImageRepository = get(),
            syncManager = get(),
            colorSchemeProcessor = lazy { get() }
        )
    }
}

val allModules = listOf(
    appModule,
    dataModule,
    networkModule,
    repositoryModule,
    serviceModule,
    workerModule,
    viewModelModule,
    backupModule
)
