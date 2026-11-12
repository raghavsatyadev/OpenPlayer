package io.github.raghavsatyadev.openplayer.di

import com.google.gson.GsonBuilder
import io.github.raghavsatyadev.openplayer.data.backup.AppDataBackupManager
import io.github.raghavsatyadev.openplayer.data.backup.BackupManager
import io.github.raghavsatyadev.openplayer.data.backup.format.BackupFormatDetector
import io.github.raghavsatyadev.openplayer.data.backup.format.BackupReader
import io.github.raghavsatyadev.openplayer.data.backup.format.BackupWriter
import io.github.raghavsatyadev.openplayer.data.backup.format.LegacyPayloadAdapter
import io.github.raghavsatyadev.openplayer.data.backup.history.BackupHistoryRepository
import io.github.raghavsatyadev.openplayer.data.backup.model.BackupSection
import io.github.raghavsatyadev.openplayer.data.backup.module.ArtistImagesModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.AudioBookmarksModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.BackupModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.BackupSongResolver
import io.github.raghavsatyadev.openplayer.data.backup.module.EngagementStatsModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.EqualizerModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.FavoritesModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.GlobalSettingsModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.ListenHistoryModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.LyricsModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.PlaybackHistoryModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.PlaylistsModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.QuickFillModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.SavedQueuesModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.SearchHistoryModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.SongUserMetaModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.TransitionsModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.restore.RestoreExecutor
import io.github.raghavsatyadev.openplayer.data.backup.restore.RestorePlanner
import io.github.raghavsatyadev.openplayer.data.backup.validation.BackupFileValidator
import io.github.raghavsatyadev.openplayer.data.backup.validation.ContentSanitizer
import io.github.raghavsatyadev.openplayer.data.backup.validation.ManifestValidator
import io.github.raghavsatyadev.openplayer.data.backup.validation.ModuleSchemaValidator
import io.github.raghavsatyadev.openplayer.data.backup.validation.ValidationPipeline
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val backupModule = module {
    single(QUALIFIER_BACKUP_GSON) {
        GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create()
    }

    singleOf(::BackupFormatDetector)

    single {
        BackupHistoryRepository(
            dataStore = get(),
            gson = get(QUALIFIER_BACKUP_GSON)
        )
    }

    single {
        PlaylistsModuleHandler(
            context = androidContext(),
            playlistPreferencesRepository = get(),
            userPreferencesRepository = get(),
            musicDao = get(),
            songResolver = get(),
            gson = get(QUALIFIER_BACKUP_GSON),
            syncManagerProvider = { get() },
            appScope = get(QUALIFIER_APP_SCOPE)
        )
    }
    single {
        GlobalSettingsModuleHandler(
            userPreferencesRepository = get(),
            gson = get(QUALIFIER_BACKUP_GSON)
        )
    }
    single {
        FavoritesModuleHandler(
            favoritesDao = get(),
            gson = get(QUALIFIER_BACKUP_GSON)
        )
    }
    single {
        LyricsModuleHandler(
            lyricsDao = get(),
            gson = get(QUALIFIER_BACKUP_GSON)
        )
    }
    single {
        SearchHistoryModuleHandler(
            searchHistoryDao = get(),
            gson = get(QUALIFIER_BACKUP_GSON)
        )
    }
    single {
        TransitionsModuleHandler(
            transitionDao = get(),
            songResolver = get(),
            gson = get(QUALIFIER_BACKUP_GSON)
        )
    }
    single {
        EngagementStatsModuleHandler(
            engagementDao = get(),
            gson = get(QUALIFIER_BACKUP_GSON)
        )
    }
    single {
        PlaybackHistoryModuleHandler(
            playbackStatsRepository = get(),
            gson = get(QUALIFIER_BACKUP_GSON)
        )
    }
    single {
        QuickFillModuleHandler(
            userPreferencesRepository = get(),
            gson = get(QUALIFIER_BACKUP_GSON)
        )
    }
    single {
        ArtistImagesModuleHandler(
            context = androidContext(),
            musicDao = get(),
            gson = get(QUALIFIER_BACKUP_GSON)
        )
    }
    single {
        EqualizerModuleHandler(
            userPreferencesRepository = get(),
            gson = get(QUALIFIER_BACKUP_GSON)
        )
    }
    single {
        AudioBookmarksModuleHandler(
            audioBookmarkDao = get(),
            songResolver = get(),
            gson = get(QUALIFIER_BACKUP_GSON)
        )
    }
    single {
        SongUserMetaModuleHandler(
            dao = get(),
            songResolver = get(),
            gson = get(QUALIFIER_BACKUP_GSON)
        )
    }
    single {
        SavedQueuesModuleHandler(
            dao = get(),
            songResolver = get(),
            gson = get(QUALIFIER_BACKUP_GSON)
        )
    }
    single {
        ListenHistoryModuleHandler(
            dao = get(),
            songResolver = get(),
            preferences = get(),
            gson = get(QUALIFIER_BACKUP_GSON)
        )
    }

    single<Map<BackupSection, BackupModuleHandler>> {
        mapOf(
            BackupSection.PLAYLISTS to get<PlaylistsModuleHandler>(),
            BackupSection.GLOBAL_SETTINGS to get<GlobalSettingsModuleHandler>(),
            BackupSection.FAVORITES to get<FavoritesModuleHandler>(),
            BackupSection.LYRICS to get<LyricsModuleHandler>(),
            BackupSection.SEARCH_HISTORY to get<SearchHistoryModuleHandler>(),
            BackupSection.TRANSITIONS to get<TransitionsModuleHandler>(),
            BackupSection.ENGAGEMENT_STATS to get<EngagementStatsModuleHandler>(),
            BackupSection.PLAYBACK_HISTORY to get<PlaybackHistoryModuleHandler>(),
            BackupSection.QUICK_FILL to get<QuickFillModuleHandler>(),
            BackupSection.ARTIST_IMAGES to get<ArtistImagesModuleHandler>(),
            BackupSection.EQUALIZER to get<EqualizerModuleHandler>(),
            BackupSection.AUDIO_BOOKMARKS to get<AudioBookmarksModuleHandler>(),
            BackupSection.SONG_USER_META to get<SongUserMetaModuleHandler>(),
            BackupSection.SAVED_QUEUES to get<SavedQueuesModuleHandler>(),
            BackupSection.LISTEN_HISTORY to get<ListenHistoryModuleHandler>()
        )
    }

    single { BackupReader(androidContext(), get(QUALIFIER_BACKUP_GSON), get(), get()) }
    single { BackupWriter(androidContext(), get(QUALIFIER_BACKUP_GSON)) }
    singleOf(::LegacyPayloadAdapter)
    singleOf(::BackupSongResolver)
    singleOf(::ContentSanitizer)
    singleOf(::ManifestValidator)
    singleOf(::ModuleSchemaValidator)
    singleOf(::ValidationPipeline)
    singleOf(::RestoreExecutor)
    singleOf(::RestorePlanner)
    singleOf(::BackupFileValidator)
    singleOf(::BackupManager)
    singleOf(::AppDataBackupManager)
}
