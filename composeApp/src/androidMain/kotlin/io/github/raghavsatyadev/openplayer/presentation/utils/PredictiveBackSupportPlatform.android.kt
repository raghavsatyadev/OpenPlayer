package io.github.raghavsatyadev.openplayer.presentation.utils

import android.os.Build

actual val isPredictiveBackProgressSupported: Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
