package io.github.raghavsatyadev.openplayer.presentation.utils

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

@Composable
actual fun AppPredictiveBackHandler(enabled: Boolean, onBack: suspend (progress: Flow<AppBackEvent>) -> Unit) = Unit
