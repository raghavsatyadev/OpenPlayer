package io.github.raghavsatyadev.openplayer.presentation.utils

import androidx.compose.runtime.Composable

interface AppToaster {
    fun show(message: String, long: Boolean = false)
}

@Composable
expect fun rememberAppToaster(): AppToaster
