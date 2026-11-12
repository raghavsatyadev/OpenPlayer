package io.github.raghavsatyadev.openplayer.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAppHapticPerformer(): AppHapticPerformer =
    remember { AppHapticPerformer { _, _ -> false } }
