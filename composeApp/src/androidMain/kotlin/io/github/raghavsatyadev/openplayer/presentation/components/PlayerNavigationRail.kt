package io.github.raghavsatyadev.openplayer.presentation.components

import io.github.raghavsatyadev.openplayer.utils.elapsedRealtimeMs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.raghavsatyadev.openplayer.presentation.navigation.AppNavigator
import io.github.raghavsatyadev.openplayer.BottomNavItem
import io.github.raghavsatyadev.openplayer.presentation.navigation.Screen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.github.raghavsatyadev.openplayer.resources.*
import org.jetbrains.compose.resources.vectorResource

/**
 * Material 3 wide navigation rail used instead of the bottom bar on medium/expanded
 * window widths (tablets, unfolded foldables). The header button expands it to show
 * labels beside the icons. Mirrors the navigation semantics of
 * [PlayerInternalNavigationBar], including the double-tap-on-Search shortcut.
 */
@Composable
fun PlayerNavigationRail(
    navController: AppNavigator,
    navItems: ImmutableList<BottomNavItem>,
    currentTab: Screen?,
    modifier: Modifier = Modifier,
    onSearchIconDoubleTap: () -> Unit = {}
) {
    val latestCurrentTab by rememberUpdatedState(currentTab)
    val latestOnSearchIconDoubleTap by rememberUpdatedState(onSearchIconDoubleTap)
    val latestNavigationEnabled by rememberUpdatedState(currentTab != null)
    val scope = rememberCoroutineScope()
    var lastSearchTapTimestamp by remember { mutableLongStateOf(0L) }
    val railState = rememberWideNavigationRailState()
    val isExpanded = railState.targetValue == WideNavigationRailValue.Expanded

    WideNavigationRail(
        modifier = modifier.fillMaxHeight(),
        state = railState,
        arrangement = Arrangement.Center,
        header = {
            IconButton(
                onClick = { scope.launch { railState.toggle() } },
                modifier = Modifier.padding(start = 24.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) vectorResource(Res.drawable.rounded_menu_open_mirrored_24) else vectorResource(Res.drawable.rounded_menu_24),
                    contentDescription = stringResource(
                        if (isExpanded) Res.string.navigation_rail_collapse else Res.string.navigation_rail_expand
                    )
                )
            }
        }
    ) {
        navItems.forEach { item ->
            val isSelected = currentTab != null && currentTab == item.screen
            val label = stringResource(item.labelResId)
            val iconPainterResId = if (isSelected && item.selectedIconResId != null && item.selectedIconResId != 0) {
                item.selectedIconResId
            } else {
                item.iconResId
            }
            val onClickLambda: () -> Unit = remember(item.screen, navController, scope) {
                click@{
                    if (!latestNavigationEnabled) {
                        lastSearchTapTimestamp = 0L
                        return@click
                    }

                    val itemRoute = item.screen
                    val isSearchTab = itemRoute == Screen.Search
                    val isAlreadySelected = latestCurrentTab == itemRoute

                    if (isSearchTab) {
                        val now = elapsedRealtimeMs()
                        val isDoubleTap = now - lastSearchTapTimestamp <= 350L
                        lastSearchTapTimestamp = now

                        if (!isAlreadySelected) {
                            if (!navController.navigateToTopLevelSafely(itemRoute)) {
                                lastSearchTapTimestamp = 0L
                                return@click
                            }
                        }

                        if (isDoubleTap) {
                            lastSearchTapTimestamp = 0L
                            if (isAlreadySelected) {
                                latestOnSearchIconDoubleTap()
                            } else {
                                scope.launch {
                                    delay(160L)
                                    latestOnSearchIconDoubleTap()
                                }
                            }
                        }
                    } else if (!isAlreadySelected) {
                        lastSearchTapTimestamp = 0L
                        navController.navigateToTopLevelSafely(itemRoute)
                    } else {
                        lastSearchTapTimestamp = 0L
                        navController.navigateToTabRoot(itemRoute)
                    }
                }
            }
            WideNavigationRailItem(
                selected = isSelected,
                onClick = onClickLambda,
                enabled = currentTab != null,
                railExpanded = isExpanded,
                icon = {
                    Icon(
                        painter = painterResource(id = iconPainterResId),
                        contentDescription = label
                    )
                },
                label = { Text(label) }
            )
        }
    }
}
