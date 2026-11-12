package io.github.raghavsatyadev.openplayer.utils

/** ICU "Katakana-Latin; Lower". Returns the input unchanged where ICU is not available. */
expect fun katakanaToLatinLower(text: String): String
