package io.github.raghavsatyadev.openplayer.presentation.navigation

import DelimiterConfigScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.raghavsatyadev.openplayer.presentation.screens.AboutScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.AccountsScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.ArtistSettingsScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.DeviceCapabilitiesScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.DuplicateSongsScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.EasterEggScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.EqualizerScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.ExcludedDirectoriesScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.ExperimentalSettingsScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.LicensesScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.LyricsSettingsScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.NavBarCornerRadiusScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.PaletteStyleSettingsScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.SettingsCategoryScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.SettingsScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.UpdatesScreen
import io.github.raghavsatyadev.openplayer.presentation.screens.WordDelimiterConfigScreen
import io.github.raghavsatyadev.openplayer.presentation.settings.search.ProvideSettingsHighlight
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import io.github.raghavsatyadev.openplayer.presentation.model.SettingsCategory as SettingsCategoryModel

/** Settings, about and tool screens. */
internal fun EntryProviderScope<NavKey>.settingsEntries(deps: AppEntryDeps) {
    val nav = deps.navController
    val player = deps.playerViewModel

    wrappedScreen<Screen.Settings>(deps) {
        SettingsScreen(
            navController = nav,
            playerViewModel = player,
            onNavigationIconClick = { nav.popBackStack() }
        )
    }
    wrappedScreen<Screen.Accounts>(deps) {
        AccountsScreen(
            onBackClick = { nav.popBackStack() },
            onOpenNavidromeDashboard = { nav.navigateSafely(Screen.NavidromeDashboard) },
            onOpenJellyfinDashboard = { nav.navigateSafely(Screen.JellyfinDashboard) }
        )
    }
    wrappedScreen<Screen.SettingsCategory>(deps) { key ->
        ProvideSettingsHighlight(key.highlightKey) {
            SettingsCategoryScreen(
                categoryId = key.categoryId,
                highlightKey = key.highlightKey,
                navController = nav,
                playerViewModel = player,
                onBackClick = { nav.popBackStack() }
            )
        }
    }
    wrappedScreen<Screen.PaletteStyle>(deps) {
        PaletteStyleSettingsScreen(playerViewModel = player, onBackClick = { nav.popBackStack() })
    }
    wrappedScreen<Screen.Experimental>(deps) {
        ExperimentalSettingsScreen(
            navController = nav,
            playerViewModel = player,
            onNavigationIconClick = { nav.popBackStack() }
        )
    }
    wrappedScreen<Screen.LyricsSettings>(deps) { key ->
        ProvideSettingsHighlight(key.highlightKey) {
            LyricsSettingsScreen(navController = nav, playerViewModel = player, highlightKey = key.highlightKey)
        }
    }
    wrappedScreen<Screen.Duplicates>(deps) {
        DuplicateSongsScreen(navController = nav, playerViewModel = player)
    }
    wrappedScreen<Screen.NavBarCrRad>(deps) { NavBarCornerRadiusScreen(nav) }
    wrappedScreen<Screen.About>(deps) {
        AboutScreen(navController = nav, viewModel = player, onNavigationIconClick = { nav.popBackStack() })
    }
    wrappedScreen<Screen.Licenses>(deps) {
        LicensesScreen(onNavigationIconClick = { nav.popBackStack() })
    }
    wrappedScreen<Screen.EasterEgg>(deps) {
        EasterEggScreen(viewModel = player, onNavigationIconClick = { nav.popBackStack() })
    }
    wrappedScreen<Screen.ArtistSettings>(deps) { key ->
        ProvideSettingsHighlight(key.highlightKey) {
            ArtistSettingsScreen(navController = nav, highlightKey = key.highlightKey)
        }
    }
    wrappedScreen<Screen.DelimiterConfig>(deps) { DelimiterConfigScreen(navController = nav) }
    wrappedScreen<Screen.WordDelimiterConfig>(deps) { WordDelimiterConfigScreen(navController = nav) }
    screen<Screen.ExcludedDirectories> {
        val initialMiniplayerVisible = remember(player) {
            player.stablePlayerState.value.currentSong != null
        }
        val isMiniplayerVisible by remember(player) {
            player.stablePlayerState
                .map { state -> state.currentSong != null }
                .distinctUntilChanged()
        }.collectAsStateWithLifecycle(initialValue = initialMiniplayerVisible)
        io.github.raghavsatyadev.openplayer.presentation.components.ScreenWrapper(
            navController = nav,
            playerViewModel = player
        ) {
            ExcludedDirectoriesScreen(
                onNavigateBack = { nav.popBackStack(Screen.ExcludedDirectories::class, inclusive = true) },
                isMiniplayerVisible = isMiniplayerVisible
            )
        }
    }
    wrappedScreen<Screen.Equalizer>(deps) {
        EqualizerScreen(navController = nav, playerViewModel = player)
    }
    wrappedScreen<Screen.DeviceCapabilities>(deps) {
        DeviceCapabilitiesScreen(navController = nav, playerViewModel = player)
    }
    wrappedScreen<Screen.Updates>(deps) {
        UpdatesScreen(
            onBack = { nav.popBackStack() },
            onOpenBackup = {
                nav.navigateSafely(
                    Screen.SettingsCategory(
                        SettingsCategoryModel.BACKUP_RESTORE.id,
                        highlightKey = "item_backup_export"
                    )
                )
            }
        )
    }
}
