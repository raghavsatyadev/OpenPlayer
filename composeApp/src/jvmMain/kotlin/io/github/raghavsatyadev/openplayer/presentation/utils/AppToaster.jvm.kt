package io.github.raghavsatyadev.openplayer.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAppToaster(): AppToaster {
    return remember {
        object : AppToaster {
            override fun show(message: String, long: Boolean) = Unit
        }
    }
}
