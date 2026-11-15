package io.github.raghavsatyadev.openplayer.presentation.utils

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

/** Platform-neutral copy of the fields OpenPlayer reads from a predictive back event. */
data class AppBackEvent(val progress: Float, val swipeEdge: Int) {
    companion object {
        const val EDGE_LEFT = 0
        const val EDGE_RIGHT = 1
        const val EDGE_NONE = 2
    }
}

/**
 * Predictive back with gesture progress. The flow emits while the gesture runs, completes when it
 * is committed and throws CancellationException when it is cancelled (same contract as
 * androidx.activity.compose.PredictiveBackHandler). Desktop has no back gesture, so it does nothing there.
 */
@Composable
expect fun AppPredictiveBackHandler(enabled: Boolean = true, onBack: suspend (progress: Flow<AppBackEvent>) -> Unit)
