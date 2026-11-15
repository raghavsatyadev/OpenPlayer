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
internal fun PlayerArtistNavigationEffect(
    navController: AppNavigator,
    sheetCollapsedTargetY: Float,
    sheetMotionController: SheetMotionController,
    playerViewModel: PlayerViewModel
) {
    val latestSheetCollapsedTargetY by rememberUpdatedState(sheetCollapsedTargetY)
    LaunchedEffect(navController) {
        playerViewModel.artistNavigationRequests.collectLatest { artistId ->
            sheetMotionController.snapCollapsed(latestSheetCollapsedTargetY)
            playerViewModel.collapsePlayerSheet()

            navController.navigateSafelyReplacing(
                route = Screen.ArtistDetail(artistId),
                patternToPop = Screen.ArtistDetail::class
            )
        }
    }
}
