package io.github.raghavsatyadev.openplayer.presentation.utils

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat

@Composable
actual fun rememberAppHapticPerformer(): AppHapticPerformer {
    val view = LocalView.current
    return remember(view) {
        AppHapticPerformer { type, ignore ->
            ViewCompat.performHapticFeedback(
                view,
                type.constant(),
                if (ignore) HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING else 0,
            )
        }
    }
}

private fun AppHapticType.constant(): Int =
    when (this) {
        AppHapticType.ClockTick -> HapticFeedbackConstantsCompat.CLOCK_TICK
        AppHapticType.GestureStart -> HapticFeedbackConstantsCompat.GESTURE_START
        AppHapticType.GestureEnd -> HapticFeedbackConstantsCompat.GESTURE_END
        AppHapticType.GestureThresholdActivate ->
            HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_ACTIVATE
        AppHapticType.GestureThresholdDeactivate ->
            HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_DEACTIVATE
        AppHapticType.KeyboardTap -> HapticFeedbackConstantsCompat.KEYBOARD_TAP
        AppHapticType.LongPress -> HapticFeedbackConstantsCompat.LONG_PRESS
    }
