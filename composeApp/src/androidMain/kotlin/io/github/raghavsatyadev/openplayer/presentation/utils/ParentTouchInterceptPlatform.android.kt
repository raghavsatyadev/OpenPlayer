package io.github.raghavsatyadev.openplayer.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
actual fun rememberParentTouchInterceptBlocker(): ParentTouchInterceptBlocker {
    val view = LocalView.current
    return remember(view) {
        ParentTouchInterceptBlocker { disallow -> view.parent?.requestDisallowInterceptTouchEvent(disallow) }
    }
}
