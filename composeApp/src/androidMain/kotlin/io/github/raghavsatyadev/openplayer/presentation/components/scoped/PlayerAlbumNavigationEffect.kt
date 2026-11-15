package io.github.raghavsatyadev.openplayer.presentation.components.scoped

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import io.github.raghavsatyadev.openplayer.presentation.navigation.AppNavigator
import io.github.raghavsatyadev.openplayer.presentation.navigation.Screen
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun PlayerAlbumNavigationEffect(
    navController: AppNavigator,
    sheetCollapsedTargetY: Float,
    sheetMotionController: SheetMotionController,
    playerViewModel: PlayerViewModel
) {
    val latestSheetCollapsedTargetY by rememberUpdatedState(sheetCollapsedTargetY)
    LaunchedEffect(navController) {
        playerViewModel.albumNavigationRequests.collectLatest { albumId ->
            sheetMotionController.snapCollapsed(latestSheetCollapsedTargetY)
            playerViewModel.collapsePlayerSheet()

            navController.navigateSafelyReplacing(
                route = Screen.AlbumDetail(albumId),
                patternToPop = Screen.AlbumDetail::class
            )
        }
    }
}
