package io.github.raghavsatyadev.openplayer.utils

internal fun shouldKeepScreenAwake(
    preferenceEnabled: Boolean,
    isPlaying: Boolean,
): Boolean = preferenceEnabled && isPlaying
