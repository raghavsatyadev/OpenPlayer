package io.github.raghavsatyadev.openplayer.di

import io.github.raghavsatyadev.openplayer.presentation.jellyfin.auth.JellyfinLoginViewModel
import io.github.raghavsatyadev.openplayer.presentation.jellyfin.dashboard.JellyfinDashboardViewModel
import io.github.raghavsatyadev.openplayer.presentation.navidrome.auth.NavidromeLoginViewModel
import io.github.raghavsatyadev.openplayer.presentation.navidrome.dashboard.NavidromeDashboardViewModel
import io.github.raghavsatyadev.openplayer.presentation.screens.historyimport.HistoryImportViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.AccountsViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.AlbumDetailViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ArtistDetailViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ArtistSettingsViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.AudioBookmarksViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.AudioOutputStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.AudioOutputViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.CloudDownloadsViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ConnectivityStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.DailyMixStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.DeviceCapabilitiesViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.DuplicateSongsViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.EqualizerViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ExternalMediaStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.FolderNavigationStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.GenreDetailViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.HistoryPicksViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.LibraryStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.LibraryTabsStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.LibraryViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.LyricsStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.M3uSyncViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.MainViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.MashupViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.MetadataEditStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.MultiSelectionStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ParametricEqViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlaybackStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerVideoViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlaylistDismissUndoStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlaylistSelectionStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlaylistViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.QueueStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.QueueUndoStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.RecapEntryViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.RecapViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.RepeatNStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SavedQueuesViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SearchStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SeekbarViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SettingsViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SetupViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SleepTimerStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SmartMixStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SmartMixViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SmartRulePlaylistViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SongInfoBottomSheetViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SongRemovalStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SongUserMetaViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.StatsViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.StreamingProviderLoginViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ThemeStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.TransitionViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.UpdatesViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.VideoSettingsViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.VideosLibraryViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.search.SearchTapModeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    // State holders
    singleOf(::AudioOutputStateHolder)
    singleOf(::ConnectivityStateHolder)
    singleOf(::DailyMixStateHolder)
    singleOf(::LibraryStateHolder)
    singleOf(::LyricsStateHolder)
    singleOf(::MultiSelectionStateHolder)
    singleOf(::PlaybackStateHolder)
    singleOf(::PlaylistSelectionStateHolder)
    singleOf(::QueueStateHolder)
    singleOf(::RepeatNStateHolder)
    singleOf(::SearchStateHolder)
    singleOf(::SleepTimerStateHolder)
    singleOf(::SmartMixStateHolder)
    singleOf(::ThemeStateHolder)

    factoryOf(::ExternalMediaStateHolder)
    factoryOf(::FolderNavigationStateHolder)
    factoryOf(::LibraryTabsStateHolder)
    factoryOf(::MetadataEditStateHolder)
    factoryOf(::PlaylistDismissUndoStateHolder)
    factoryOf(::QueueUndoStateHolder)
    factoryOf(::SongRemovalStateHolder)

    // ViewModels
    viewModelOf(::JellyfinLoginViewModel)
    viewModelOf(::JellyfinDashboardViewModel)
    viewModelOf(::NavidromeLoginViewModel)
    viewModelOf(::NavidromeDashboardViewModel)
    viewModelOf(::HistoryImportViewModel)
    viewModelOf(::AccountsViewModel)
    viewModelOf(::AlbumDetailViewModel)
    viewModelOf(::ArtistDetailViewModel)
    viewModelOf(::ArtistSettingsViewModel)
    viewModelOf(::AudioBookmarksViewModel)
    viewModelOf(::AudioOutputViewModel)
    viewModelOf(::CloudDownloadsViewModel)
    viewModelOf(::DeviceCapabilitiesViewModel)
    viewModelOf(::DuplicateSongsViewModel)
    viewModelOf(::EqualizerViewModel)
    viewModelOf(::GenreDetailViewModel)
    viewModelOf(::HistoryPicksViewModel)
    viewModelOf(::LibraryViewModel)
    viewModelOf(::M3uSyncViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::MashupViewModel)
    viewModelOf(::ParametricEqViewModel)
    viewModelOf(::PlayerVideoViewModel)
    viewModel {
        PlayerViewModel(
            context = androidContext(),
            musicRepository = get(),
            userPreferencesRepository = get(),
            themePreferencesRepository = get(),
            albumArtThemeDao = get(),
            syncManager = get(),
            dualPlayerEngine = get(),
            appShortcutManager = get(),
            listeningStatsTracker = get(),
            dailyMixStateHolder = get(),
            lyricsStateHolder = get(),
            queueStateHolder = get(),
            queueUndoStateHolder = get(),
            playlistDismissUndoStateHolder = get(),
            playbackStateHolder = get(),
            connectivityStateHolder = get(),
            sleepTimerStateHolder = get(),
            repeatNStateHolder = get(),
            searchStateHolder = get(),
            libraryStateHolder = get(),
            folderNavigationStateHolder = get(),
            libraryTabsStateHolder = get(),
            metadataEditStateHolder = get(),
            songRemovalStateHolder = get(),
            externalMediaStateHolder = get(),
            themeStateHolder = get(),
            multiSelectionStateHolder = get(),
            playlistSelectionStateHolder = get(),
            sessionToken = get(),
            mediaControllerFactory = get(),
            dispatchers = get()
        )
    }
    viewModelOf(::PlaylistViewModel)
    viewModelOf(::RecapEntryViewModel)
    viewModelOf(::RecapViewModel)
    viewModelOf(::SavedQueuesViewModel)
    viewModelOf(::SeekbarViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::SetupViewModel)
    viewModelOf(::SmartMixViewModel)
    viewModelOf(::SmartRulePlaylistViewModel)
    viewModelOf(::SongInfoBottomSheetViewModel)
    viewModelOf(::SongUserMetaViewModel)
    viewModelOf(::StatsViewModel)
    viewModelOf(::StreamingProviderLoginViewModel)
    viewModelOf(::TransitionViewModel)
    viewModelOf(::UpdatesViewModel)
    viewModelOf(::VideoSettingsViewModel)
    viewModelOf(::VideosLibraryViewModel)
    viewModelOf(::SearchTapModeViewModel)
}
