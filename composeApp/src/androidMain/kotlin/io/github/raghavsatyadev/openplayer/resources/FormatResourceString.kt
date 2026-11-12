package io.github.raghavsatyadev.openplayer.resources

import java.util.Locale

internal actual fun formatResourceString(pattern: String, args: Array<out Any>): String =
    if (args.isEmpty()) pattern else String.format(Locale.getDefault(), pattern, *args)
