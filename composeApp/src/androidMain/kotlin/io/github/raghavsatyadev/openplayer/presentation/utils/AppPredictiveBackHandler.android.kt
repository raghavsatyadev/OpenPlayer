package io.github.raghavsatyadev.openplayer.presentation.utils

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Composable
actual fun AppPredictiveBackHandler(enabled: Boolean, onBack: suspend (progress: Flow<AppBackEvent>) -> Unit) {
    PredictiveBackHandler(enabled = enabled) { events ->
        onBack(events.map { AppBackEvent(progress = it.progress, swipeEdge = it.swipeEdge) })
    }
}
