package io.github.raghavsatyadev.openplayer.presentation.utils

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberIs24HourFormat(): Boolean = DateFormat.is24HourFormat(LocalContext.current)
