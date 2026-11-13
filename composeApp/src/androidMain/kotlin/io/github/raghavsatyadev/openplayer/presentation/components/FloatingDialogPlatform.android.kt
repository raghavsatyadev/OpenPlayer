package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.ui.window.DialogProperties
import android.graphics.Color as AndroidColor
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.activity.BackEventCompat
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.WindowCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
internal actual fun rememberFloatingDialogWindow(lightBars: Boolean): FloatingDialogWindow {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window
    LaunchedEffect(window) {
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            @Suppress("DEPRECATION")
            it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            it.setDimAmount(0f)
            it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            // The card animates itself; a platform window animation would fight it.
            it.setWindowAnimations(0)
            @Suppress("DEPRECATION")
            it.navigationBarColor = AndroidColor.TRANSPARENT
            @Suppress("DEPRECATION")
            it.statusBarColor = AndroidColor.TRANSPARENT
            if (Build.VERSION.SDK_INT >= 29) it.isNavigationBarContrastEnforced = false
        }
    }
    LaunchedEffect(window, lightBars) {
        window?.let {
            WindowCompat.getInsetsController(it, view).run {
                isAppearanceLightNavigationBars = lightBars
                isAppearanceLightStatusBars = lightBars
            }
        }
    }
    return remember(view) {
        FloatingDialogWindow {
            view.awaitPreDraw()
        }
    }
}

@Composable
internal actual fun FloatingDialogBackHandler(
    onBack: suspend (progress: Flow<FloatingBackEvent>) -> Unit,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    // Use this dialog's dispatcher, even when the caller supplied an activity owner.
    val dispatcherOwner = requireNotNull(view.findViewTreeOnBackPressedDispatcherOwner())
    CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides dispatcherOwner) {
        PredictiveBackHandler { progress ->
            onBack(
                progress.map { event ->
                    FloatingBackEvent(
                        progress = event.progress,
                        fromLeftEdge = event.swipeEdge == BackEventCompat.EDGE_LEFT,
                    )
                }
            )
        }
        content()
    }
}

private suspend fun View.awaitPreDraw() = suspendCancellableCoroutine { continuation ->
    val listener = OneShotPreDrawListener.add(this) { if (continuation.isActive) continuation.resume(Unit) }
    continuation.invokeOnCancellation { listener.removeListener() }
}

internal actual fun floatingDialogProperties(): DialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false,
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
)
