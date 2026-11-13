package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

/** One step of a back gesture: [progress] 0..1, [fromLeftEdge] true when the swipe started at the left edge. */
internal class FloatingBackEvent(val progress: Float, val fromLeftEdge: Boolean)

/**
 * Call first inside the Dialog content. Android: configures the dialog window (edge-to-edge, no dim, no window animation,
 * transparent bars, light/dark bar icons from [lightBars]) and returns a handle whose awaitFirstDraw() suspends until the
 * dialog view's next pre-draw. Desktop: nothing to configure; awaitFirstDraw() returns at once.
 */
@Composable internal expect fun rememberFloatingDialogWindow(lightBars: Boolean): FloatingDialogWindow
internal fun interface FloatingDialogWindow { suspend fun awaitFirstDraw() }

/**
 * Predictive back for the dialog. Android: provides the dialog's own OnBackPressedDispatcherOwner (from the dialog view,
 * as the old code did) around [content] and runs androidx.activity.compose.PredictiveBackHandler, mapping each BackEventCompat
 * to FloatingBackEvent(progress, swipeEdge == BackEventCompat.EDGE_LEFT /* == 0 */). Desktop: just renders [content]
 * (back gestures come in Phase 7).
 */
@Composable internal expect fun FloatingDialogBackHandler(
    onBack: suspend (progress: Flow<FloatingBackEvent>) -> Unit,
    content: @Composable () -> Unit,
)

/** Full-width, not dismissable by back or outside click (the card handles both); Android also draws edge-to-edge. */
internal expect fun floatingDialogProperties(): DialogProperties
