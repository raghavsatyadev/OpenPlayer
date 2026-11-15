package io.github.raghavsatyadev.openplayer.presentation.utils

import androidx.compose.runtime.Composable

/** Asks the host view system (Android) not to steal an ongoing drag; a no-op where there is none. */
fun interface ParentTouchInterceptBlocker {
    fun disallowIntercept(disallow: Boolean)
}

@Composable
expect fun rememberParentTouchInterceptBlocker(): ParentTouchInterceptBlocker
