package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow

@Composable
internal actual fun rememberFloatingDialogWindow(lightBars: Boolean): FloatingDialogWindow =
    remember { FloatingDialogWindow { } }

@Composable
internal actual fun FloatingDialogBackHandler(
    onBack: suspend (progress: Flow<FloatingBackEvent>) -> Unit,
    content: @Composable () -> Unit,
) {
    content()
}

internal actual fun floatingDialogProperties(): DialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
)
