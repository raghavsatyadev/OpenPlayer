package io.github.raghavsatyadev.openplayer.di

import io.github.raghavsatyadev.openplayer.data.genre.GenreFiller
import io.github.raghavsatyadev.openplayer.data.genre.OnlineGenreSettings
import io.github.raghavsatyadev.openplayer.data.genre.OnlineGenreSource
import io.github.raghavsatyadev.openplayer.data.history.ListenHistoryRepository
import io.github.raghavsatyadev.openplayer.data.history.recommend.HistoryPicksRepository
import io.github.raghavsatyadev.openplayer.data.image.online.OnlineCoverRepository
import io.github.raghavsatyadev.openplayer.data.jellyfin.JellyfinRepository
import io.github.raghavsatyadev.openplayer.data.listenbrainz.ListenBrainzRepository
import io.github.raghavsatyadev.openplayer.data.media.TrackBpmRepository
import io.github.raghavsatyadev.openplayer.data.musicbrainz.MusicBrainzRepository
import io.github.raghavsatyadev.openplayer.data.navidrome.NavidromeRepository
import io.github.raghavsatyadev.openplayer.data.offline.CloudDownloadScheduler
import io.github.raghavsatyadev.openplayer.data.offline.CloudOfflineRepository
import io.github.raghavsatyadev.openplayer.data.offline.WorkManagerCloudDownloadScheduler
import io.github.raghavsatyadev.openplayer.data.playlist.ArtistGenreLoader
import io.github.raghavsatyadev.openplayer.data.playlist.ConceptTableLoader
import io.github.raghavsatyadev.openplayer.data.playlist.M3uManager
import io.github.raghavsatyadev.openplayer.data.playlist.M3uSyncRepository
import io.github.raghavsatyadev.openplayer.data.playlist.NlpPlaylistGenerator
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleResolver
import io.github.raghavsatyadev.openplayer.data.preferences.EqualizerPreferencesRepository
import io.github.raghavsatyadev.openplayer.data.preferences.ListenBrainzPreferencesRepository
import io.github.raghavsatyadev.openplayer.data.preferences.ListenHistoryPreferences
import io.github.raghavsatyadev.openplayer.data.preferences.M3uSyncPreferences
import io.github.raghavsatyadev.openplayer.data.preferences.MixPreferences
import io.github.raghavsatyadev.openplayer.data.preferences.OnlineCoverPreferences
import io.github.raghavsatyadev.openplayer.data.preferences.OnlineGenrePreferences
import io.github.raghavsatyadev.openplayer.data.preferences.PlaylistPreferencesRepository
import io.github.raghavsatyadev.openplayer.data.preferences.ThemePreferencesRepository
import io.github.raghavsatyadev.openplayer.data.preferences.UserPreferencesRepository
import io.github.raghavsatyadev.openplayer.data.preferences.VideoPreferences
import io.github.raghavsatyadev.openplayer.data.repository.ArtistImageRepository
import io.github.raghavsatyadev.openplayer.data.repository.AudioBookmarkRepository
import io.github.raghavsatyadev.openplayer.data.repository.AudioBookmarkRepositoryImpl
import io.github.raghavsatyadev.openplayer.data.repository.FolderTreeBuilder
import io.github.raghavsatyadev.openplayer.data.repository.LyricsRepository
import io.github.raghavsatyadev.openplayer.data.repository.LyricsRepositoryImpl
import io.github.raghavsatyadev.openplayer.data.repository.MediaStoreSongRepository
import io.github.raghavsatyadev.openplayer.data.repository.MusicRepository
import io.github.raghavsatyadev.openplayer.data.repository.MusicRepositoryImpl
import io.github.raghavsatyadev.openplayer.data.repository.SavedQueueRepository
import io.github.raghavsatyadev.openplayer.data.repository.SongRepository
import io.github.raghavsatyadev.openplayer.data.repository.SongUserMetaRepository
import io.github.raghavsatyadev.openplayer.data.repository.TransitionRepository
import io.github.raghavsatyadev.openplayer.data.repository.TransitionRepositoryImpl
import io.github.raghavsatyadev.openplayer.data.stats.PlaybackStatsRepository
import io.github.raghavsatyadev.openplayer.data.video.LocalVideoRepository
import io.github.raghavsatyadev.openplayer.data.video.VideoLibrarySongs
import io.github.raghavsatyadev.openplayer.data.waveform.WaveformRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::UserPreferencesRepository)
    singleOf(::PlaylistPreferencesRepository)
    singleOf(::EqualizerPreferencesRepository)
    singleOf(::ThemePreferencesRepository)
    singleOf(::ListenBrainzPreferencesRepository)
    singleOf(::ListenHistoryPreferences)
    singleOf(::M3uSyncPreferences)
    singleOf(::OnlineCoverPreferences)
    singleOf(::OnlineGenrePreferences)
    singleOf(::VideoPreferences)
    single { MixPreferences(androidContext()) }
    singleOf(::FolderTreeBuilder)

    singleOf(::MediaStoreSongRepository)
    single<SongRepository> { get<MediaStoreSongRepository>() }

    singleOf(::LyricsRepositoryImpl)
    single<LyricsRepository> { get<LyricsRepositoryImpl>() }

    singleOf(::MusicRepositoryImpl)
    single<MusicRepository> { get<MusicRepositoryImpl>() }

    singleOf(::TransitionRepositoryImpl)
    single<TransitionRepository> { get<TransitionRepositoryImpl>() }

    singleOf(::AudioBookmarkRepositoryImpl)
    single<AudioBookmarkRepository> { get<AudioBookmarkRepositoryImpl>() }

    singleOf(::ArtistImageRepository)
    singleOf(::NavidromeRepository)
    singleOf(::JellyfinRepository)
    singleOf(::ListenBrainzRepository)
    singleOf(::M3uSyncRepository)
    singleOf(::OnlineCoverRepository)
    singleOf(::OnlineGenreSettings)
    singleOf(::OnlineGenreSource)
    singleOf(::GenreFiller)
    singleOf(::MusicBrainzRepository)
    single<CloudDownloadScheduler> { WorkManagerCloudDownloadScheduler(get()) }
    singleOf(::CloudOfflineRepository)
    singleOf(::SavedQueueRepository)
    singleOf(::SongUserMetaRepository)
    singleOf(::PlaybackStatsRepository)
    singleOf(::TrackBpmRepository)
    singleOf(::WaveformRepository)
    singleOf(::LocalVideoRepository)
    singleOf(::VideoLibrarySongs)
    singleOf(::ListenHistoryRepository)
    singleOf(::HistoryPicksRepository)
    singleOf(::ArtistGenreLoader)
    singleOf(::ConceptTableLoader)
    singleOf(::NlpPlaylistGenerator)
    singleOf(::SmartRuleResolver)
    singleOf(::M3uManager)
}
