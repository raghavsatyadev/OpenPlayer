package io.github.raghavsatyadev.openplayer.presentation.components.player

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerVideoViewModel

/**
 * The one [PlayerVideoViewModel] for the activity, so the player cover and the
 * picture-in-picture window share a single video player.
 */
@Composable
internal fun activityPlayerVideoViewModel(): PlayerVideoViewModel {
    val owner = LocalActivity.current as? ViewModelStoreOwner
    return if (owner != null) koinViewModel(viewModelStoreOwner = owner) else koinViewModel()
}

/** In picture-in-picture, covers the whole (tiny) window with the video and nothing else. */
@Composable
fun PipVideoOverlay(viewModel: PlayerVideoViewModel = activityPlayerVideoViewModel()) {
    val inPip by viewModel.inPictureInPicture.collectAsStateWithLifecycle()
    if (!inPip) return
    LifecycleStartEffect(Unit) {
        viewModel.attach()
        onStopOrDispose { viewModel.detach() }
    }
    val player by viewModel.player.collectAsStateWithLifecycle()
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VideoView(player, Modifier.fillMaxSize())
    }
}
