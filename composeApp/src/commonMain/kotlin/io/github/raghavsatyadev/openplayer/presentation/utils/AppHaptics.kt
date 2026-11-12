package io.github.raghavsatyadev.openplayer.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Immutable
data class AppHapticsConfig(
    val enabled: Boolean = true,
    val intensityScale: Float = 1f
)

val LocalAppHapticsConfig = staticCompositionLocalOf { AppHapticsConfig() }

val NoOpHapticFeedback: HapticFeedback = object : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

enum class AppHapticType {
    ClockTick,
    GestureStart,
    GestureEnd,
    GestureThresholdActivate,
    GestureThresholdDeactivate,
    KeyboardTap,
    LongPress
}

fun interface AppHapticPerformer {
    fun perform(type: AppHapticType, ignoreGlobalSetting: Boolean): Boolean
}

@Composable
expect fun rememberAppHapticPerformer(): AppHapticPerformer

/** Respects the in-app haptics toggle. */
fun AppHapticPerformer.performApp(
    config: AppHapticsConfig,
    type: AppHapticType,
    ignoreGlobalSetting: Boolean = false
): Boolean =
    config.enabled && perform(type, ignoreGlobalSetting)
