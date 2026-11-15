package io.github.raghavsatyadev.openplayer.presentation.utils

import androidx.compose.runtime.Composable

/** Whether the system clock shows 24-hour time. */
@Composable
expect fun rememberIs24HourFormat(): Boolean
