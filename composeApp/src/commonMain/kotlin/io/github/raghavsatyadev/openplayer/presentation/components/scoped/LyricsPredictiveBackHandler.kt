package io.github.raghavsatyadev.openplayer.presentation.components.scoped

import io.github.raghavsatyadev.openplayer.presentation.utils.AppBackHandler
import io.github.raghavsatyadev.openplayer.presentation.utils.AppPredictiveBackHandler
import io.github.raghavsatyadev.openplayer.presentation.utils.isPredictiveBackProgressSupported
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun LyricsPredictiveBackHandler(
    enabled: Boolean,
    onProgressChanged: (Float) -> Unit,
    animationDurationMs: Int = 350,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val progressAnim = remember { Animatable(0f) }

    if (isPredictiveBackProgressSupported) {
        AppPredictiveBackHandler(enabled = enabled) { progressFlow ->
            try {
                progressFlow.collect { backEvent ->
                    progressAnim.snapTo(backEvent.progress)
                    onProgressChanged(backEvent.progress)
                }
                
                scope.launch {
                    progressAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(animationDurationMs / 2)
                    ) { onProgressChanged(value) }
                    onBack()
                }
            } catch (_: CancellationException) {
                scope.launch {
                    progressAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(animationDurationMs)
                    ) { onProgressChanged(value) }
                }
            }
        }
    } else {
        AppBackHandler(enabled = enabled, onBack = onBack)
    }
}