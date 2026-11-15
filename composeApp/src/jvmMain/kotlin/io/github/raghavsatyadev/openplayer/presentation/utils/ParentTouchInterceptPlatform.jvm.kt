package io.github.raghavsatyadev.openplayer.presentation.utils

import androidx.compose.runtime.Composable

private val NoOpBlocker = ParentTouchInterceptBlocker { }

@Composable
actual fun rememberParentTouchInterceptBlocker(): ParentTouchInterceptBlocker = NoOpBlocker
