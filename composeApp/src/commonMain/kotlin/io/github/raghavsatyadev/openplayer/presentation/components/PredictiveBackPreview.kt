package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineScope
import io.github.raghavsatyadev.openplayer.presentation.utils.AppBackEvent
import kotlinx.coroutines.launch

/**
 * Progress of an in-screen predictive back gesture (folder up, etc.) that isn't a nav pop, so
 * the content can shrink toward the swipe edge like the system preview does.
 */
@Stable
internal class PredictiveBackPreviewState {
    var progress by mutableFloatStateOf(0f)
        private set
    var swipeEdge by mutableIntStateOf(AppBackEvent.EDGE_LEFT)
        private set

    fun onProgress(event: AppBackEvent) {
        swipeEdge = event.swipeEdge
        progress = event.progress
    }

    /** Animates back to rest after a cancel, or after the commit has swapped the content. */
    fun settle(scope: CoroutineScope, durationMs: Int = 200) {
        val from = progress
        if (from == 0f) return
        scope.launch {
            Animatable(from).animateTo(0f, tween(durationMs)) { progress = value }
        }
    }
}

/** Material predictive-back preview: scale to 90%, shift 8dp toward the swipe edge, round corners. */
internal fun Modifier.predictiveBackPreview(state: PredictiveBackPreviewState): Modifier =
    graphicsLayer {
        val p = state.progress
        if (p == 0f) return@graphicsLayer
        val scale = lerp(1f, 0.9f, p)
        scaleX = scale
        scaleY = scale
        val shift = 8.dp.toPx() * p
        translationX = if (state.swipeEdge == AppBackEvent.EDGE_RIGHT) -shift else shift
        shape = RoundedCornerShape(lerp(0f, 28f, p).dp)
        clip = true
    }
