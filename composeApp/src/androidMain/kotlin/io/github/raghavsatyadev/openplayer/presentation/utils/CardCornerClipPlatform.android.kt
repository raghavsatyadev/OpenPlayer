package io.github.raghavsatyadev.openplayer.presentation.utils

import android.os.Build

actual val needsCardCornerClipFallback: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
