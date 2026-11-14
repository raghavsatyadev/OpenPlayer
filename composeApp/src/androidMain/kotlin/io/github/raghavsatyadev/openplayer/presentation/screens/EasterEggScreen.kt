package io.github.raghavsatyadev.openplayer.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.raghavsatyadev.openplayer.presentation.components.brickbreaker.BrickBreakerOverlay
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerViewModel
import io.github.raghavsatyadev.openplayer.presentation.utils.rememberAppToaster
import io.github.raghavsatyadev.openplayer.resources.*

@Composable
fun EasterEggScreen(
    viewModel: PlayerViewModel,
    onNavigationIconClick: () -> Unit,
) {
    val toaster = rememberAppToaster()
    val toastEasterEggThankYou = stringResource(Res.string.easter_egg_thank_you)
    var isVisible by rememberSaveable { mutableStateOf(false) }
    var hasShownFanToast by rememberSaveable { mutableStateOf(false) }

    val stablePlayerState by viewModel.stablePlayerState.collectAsStateWithLifecycle()
    val currentSong = stablePlayerState.currentSong

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(hasShownFanToast) {
        if (hasShownFanToast) return@LaunchedEffect
        toaster.show(toastEasterEggThankYou)
        hasShownFanToast = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 260)) +
            scaleIn(
                initialScale = 0.97f,
                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            ) +
            slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight / 10 },
                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            ),
    ) {
        BrickBreakerOverlay(
            isMiniPlayerVisible = currentSong != null,
            onPlayRandom = { viewModel.playRandomSong() },
            onClose = {
                isVisible = false
                onNavigationIconClick()
            },
        )
    }
}
