package io.github.raghavsatyadev.openplayer.presentation.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.raghavsatyadev.openplayer.presentation.recap.RecapScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.AlbumDetailScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.ArtistDetailScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.AudioBookmarkFolderScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.AudioBookmarksScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.CloudDownloadsScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.DailyMixScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.EditTransitionScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.GenreDetailScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.HomeScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.LibraryScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.MashupScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.PlaylistDetailScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.RecentlyPlayedScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.SearchScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.StatsScreen
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlaylistViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Main tabs, library and media detail screens. */
internal fun EntryProviderScope<NavKey>.mainEntries(deps: AppEntryDeps) {
    val nav = deps.navController
    val player = deps.playerViewModel

    wrappedScreen<Screen.Home>(deps) {
        HomeScreen(
            navController = nav,
            paddingValuesParent = deps.paddingValues,
            playerViewModel = player,
            onOpenSidebar = deps.onOpenSidebar
        )
    }
    wrappedScreen<Screen.Search>(deps) {
        SearchScreen(
            paddingValues = deps.paddingValues,
            playerViewModel = player,
            navController = nav,
            onSearchBarActiveChange = deps.onSearchBarActiveChange
        )
    }
    wrappedScreen<Screen.Library>(deps) {
        LibraryScreen(navController = nav, playerViewModel = player)
    }
    wrappedScreen<Screen.DailyMixScreen>(deps) {
        DailyMixScreen(playerViewModel = player, navController = nav)
    }
    wrappedScreen<Screen.RecentlyPlayed>(deps) {
        RecentlyPlayedScreen(playerViewModel = player, navController = nav)
    }
    wrappedScreen<Screen.Stats>(deps) {
        StatsScreen(navController = nav, playerViewModel = player)
    }
    wrappedScreen<Screen.Recap>(deps) { key ->
        RecapScreen(navController = nav, playerViewModel = player, year = key.year)
    }
    wrappedScreen<Screen.PlaylistDetail>(deps) { key ->
        val playlistViewModel: PlaylistViewModel = koinViewModel()
        PlaylistDetailScreen(
            playlistId = key.playlistId,
            playerViewModel = player,
            playlistViewModel = playlistViewModel,
            onBackClick = { nav.popBackStack() },
            onDeletePlayListClick = { nav.popBackStack() },
            navController = nav
        )
    }
    wrappedScreen<Screen.DJSpace>(deps) { MashupScreen() }
    wrappedScreen<Screen.GenreDetail>(deps) { key ->
        GenreDetailScreen(navController = nav, genreId = key.genreId, playerViewModel = player)
    }
    wrappedScreen<Screen.AlbumDetail>(deps) { key ->
        AlbumDetailScreen(albumId = key.albumId.toString(), navController = nav, playerViewModel = player)
    }
    wrappedScreen<Screen.ArtistDetail>(deps) { key ->
        ArtistDetailScreen(artistId = key.artistId.toString(), navController = nav, playerViewModel = player)
    }
    wrappedScreen<Screen.EditTransition>(deps) { key ->
        EditTransitionScreen(navController = nav, playlistId = key.playlistId)
    }
    wrappedScreen<Screen.NavidromeDashboard>(deps) {
        io.github.raghavsatyadev.openplayer.presentation.navidrome.dashboard.NavidromeDashboardScreen(
            onBack = { nav.popBackStack() }
        )
    }
    wrappedScreen<Screen.JellyfinDashboard>(deps) {
        io.github.raghavsatyadev.openplayer.presentation.jellyfin.dashboard.JellyfinDashboardScreen(
            onBack = { nav.popBackStack() }
        )
    }
    wrappedScreen<Screen.CloudDownloads>(deps) {
        CloudDownloadsScreen(onBack = { nav.popBackStack() })
    }
    // The bookmark screens draw their own ScreenWrapper.
    screen<Screen.AudioBookmarks> {
        AudioBookmarksScreen(navController = nav, playerViewModel = player, onOpenSidebar = deps.onOpenSidebar)
    }
    screen<Screen.AudioBookmarkFolder> { key ->
        AudioBookmarkFolderScreen(songId = key.songId, navController = nav, playerViewModel = player)
    }
}
