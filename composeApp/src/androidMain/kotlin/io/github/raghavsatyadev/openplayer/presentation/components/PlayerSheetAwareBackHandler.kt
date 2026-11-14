package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.raghavsatyadev.openplayer.presentation.navigation.PlayerSheetBackCoordinator

@Composable
internal fun PlayerSheetAwareBackHandler(
    coordinator: PlayerSheetBackCoordinator,
    enabled: Boolean,
    /** Gesture progress (API 34+); null when this back has no preview, e.g. clearing a selection. */
    onProgress: ((BackEventCompat) -> Unit)? = null,
    onCancel: () -> Unit = {},
    onBack: () -> Unit
) {
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnProgress by rememberUpdatedState(onProgress)
    val currentOnCancel by rememberUpdatedState(onCancel)
    val callback = remember {
        object : OnBackPressedCallback(false) {
            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                currentOnProgress?.invoke(backEvent)
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                currentOnProgress?.invoke(backEvent)
            }

            override fun handleOnBackCancelled() = currentOnCancel()
            override fun handleOnBackPressed() = currentOnBack()
        }
    }
    val dispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current

    SideEffect {
        coordinator.updateCallback(callback, enabled)
    }
    DisposableEffect(dispatcher, lifecycleOwner, coordinator) {
        dispatcher.addCallback(lifecycleOwner, callback)
        onDispose {
            coordinator.removeCallback(callback)
            callback.remove()
        }
    }
}
