package io.github.raghavsatyadev.openplayer.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.raghavsatyadev.openplayer.presentation.components.LocalScreenKey
import io.github.raghavsatyadev.openplayer.presentation.components.ScreenWrapper
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerViewModel

/** What every destination needs from the host. */
internal class AppEntryDeps(
    val navController: AppNavigator,
    val playerViewModel: PlayerViewModel,
    val paddingValues: PaddingValues,
    val onSearchBarActiveChange: (Boolean) -> Unit,
    val onOpenSidebar: () -> Unit,
)

@Composable
fun AppNavigation(
    playerViewModel: PlayerViewModel,
    navController: AppNavigator,
    paddingValues: PaddingValues,
    onSearchBarActiveChange: (Boolean) -> Unit,
    onOpenSidebar: () -> Unit
) {
    val deps = AppEntryDeps(
        navController = navController,
        playerViewModel = playerViewModel,
        paddingValues = paddingValues,
        onSearchBarActiveChange = onSearchBarActiveChange,
        onOpenSidebar = onOpenSidebar,
    )
    NavDisplay(
        backStack = navController.backStack,
        onBack = { navController.popBackStack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = { pushTransform(initialState, targetState) },
        popTransitionSpec = { popTransform(initialState, targetState) },
        predictivePopTransitionSpec = { _ -> popTransform(initialState, targetState) },
        entryProvider = entryProvider {
            mainEntries(deps)
            settingsEntries(deps)
        },
    )
}

/**
 * Registers a destination: tags main tabs with their index (for the tab slide) and provides the
 * entry's key to [ScreenWrapper], which uses it to round and dim screens that are covered.
 */
internal inline fun <reified K : Screen> EntryProviderScope<NavKey>.screen(
    noinline content: @Composable (K) -> Unit
) {
    entry<K>(
        metadata = { key -> key.mainTabIndex?.let { mapOf(MAIN_TAB_INDEX to it) } ?: emptyMap() }
    ) { key ->
        CompositionLocalProvider(LocalScreenKey provides key) { content(key) }
    }
}

/** [screen] plus the usual [ScreenWrapper]. */
internal inline fun <reified K : Screen> EntryProviderScope<NavKey>.wrappedScreen(
    deps: AppEntryDeps,
    noinline content: @Composable (K) -> Unit
) {
    screen<K> { key ->
        ScreenWrapper(navController = deps.navController, playerViewModel = deps.playerViewModel) {
            content(key)
        }
    }
}
