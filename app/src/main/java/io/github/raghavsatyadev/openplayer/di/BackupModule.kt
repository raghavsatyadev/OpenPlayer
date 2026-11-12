package io.github.raghavsatyadev.openplayer.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.raghavsatyadev.openplayer.data.backup.format.BackupFormatDetector
import io.github.raghavsatyadev.openplayer.data.backup.model.BackupSection
import io.github.raghavsatyadev.openplayer.data.backup.module.ArtistImagesModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.AudioBookmarksModuleHandler
import io.github.raghavsatyadev.openplayer.data.backup.module.BackupModuleHandler
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {

    @Provides
    @Singleton
    @BackupGson
    fun provideBackupGson(): Gson {
        return GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create()
    }

    @Provides
    @Singleton
    fun provideBackupFormatDetector(): BackupFormatDetector {
        return BackupFormatDetector()
    }

    @Provides
    @Singleton
    fun provideModuleHandlers(
        playlistsHandler: PlaylistsModuleHandler,
        globalSettingsHandler: GlobalSettingsModuleHandler,
        favoritesHandler: FavoritesModuleHandler,
        lyricsHandler: LyricsModuleHandler,
        searchHistoryHandler: SearchHistoryModuleHandler,
        transitionsHandler: TransitionsModuleHandler,
        engagementStatsHandler: EngagementStatsModuleHandler,
        playbackHistoryHandler: PlaybackHistoryModuleHandler,
        quickFillHandler: QuickFillModuleHandler,
        artistImagesHandler: ArtistImagesModuleHandler,
        equalizerHandler: EqualizerModuleHandler,
        audioBookmarksHandler: AudioBookmarksModuleHandler,
        songUserMetaHandler: SongUserMetaModuleHandler,
        savedQueuesHandler: SavedQueuesModuleHandler,
        listenHistoryHandler: ListenHistoryModuleHandler
    ): Map<BackupSection, BackupModuleHandler> {
        return mapOf(
            BackupSection.PLAYLISTS to playlistsHandler,
            BackupSection.GLOBAL_SETTINGS to globalSettingsHandler,
            BackupSection.FAVORITES to favoritesHandler,
            BackupSection.LYRICS to lyricsHandler,
            BackupSection.SEARCH_HISTORY to searchHistoryHandler,
            BackupSection.TRANSITIONS to transitionsHandler,
            BackupSection.ENGAGEMENT_STATS to engagementStatsHandler,
            BackupSection.PLAYBACK_HISTORY to playbackHistoryHandler,
            BackupSection.QUICK_FILL to quickFillHandler,
            BackupSection.ARTIST_IMAGES to artistImagesHandler,
            BackupSection.EQUALIZER to equalizerHandler,
            BackupSection.AUDIO_BOOKMARKS to audioBookmarksHandler,
            BackupSection.SONG_USER_META to songUserMetaHandler,
            BackupSection.SAVED_QUEUES to savedQueuesHandler,
            BackupSection.LISTEN_HISTORY to listenHistoryHandler
        )
    }
}
