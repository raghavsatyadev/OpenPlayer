package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import io.github.raghavsatyadev.openplayer.presentation.navigation.AppNavigator
import io.github.raghavsatyadev.openplayer.presentation.navigation.isMainRootRoute
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerViewModel

/** The back stack key of the entry being composed; provided by AppNavigation for every entry. */
val LocalScreenKey = staticCompositionLocalOf<NavKey?> { null }

@Composable
fun ScreenWrapper(
    navController: AppNavigator,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ScreenSurface(navigator = navController, modifier = modifier, content = content)
}

/**
 * Rounds and dims a screen while another one covers it: the screen right below the top one is
 * the one revealed by a back swipe or covered by a push, so it gets the dim. Reading the back
 * stack (a snapshot list) keeps this in step with pushes, pops and predictive back gestures.
 */
@Composable
internal fun ScreenSurface(
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val myKey = LocalScreenKey.current
    val top = navigator.current
    val isTop = myKey == null || top == myKey
    val isPrevious = myKey != null && navigator.previous == myKey

    val shouldRunDepthEffects = !isMainRootRoute(myKey) || !isMainRootRoute(top)

    val targetRadius = if (shouldRunDepthEffects && !isTop) 32f else 0f
    val cornerRadius by animateFloatAsState(
        targetValue = targetRadius,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "cornerRadius"
    )

    val targetDim = if (shouldRunDepthEffects && isPrevious) 0.4f else 0f
    val dimAlpha by animateFloatAsState(
        targetValue = targetDim,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "dimAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = if (shouldRunDepthEffects) {
                    CompositingStrategy.Offscreen
                } else {
                    CompositingStrategy.Auto
                }
                if (shouldRunDepthEffects && cornerRadius > 0.5f) {
                    this.shape = RoundedCornerShape(cornerRadius.dp)
                    this.clip = true
                } else {
                    this.clip = false
                }
            }
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = dimAlpha }
                .background(Color.Black)
        )
    }
}
