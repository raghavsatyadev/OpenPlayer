package io.github.raghavsatyadev.openplayer.utils

import android.icu.text.Transliterator

private val katakanaLatin by lazy { Transliterator.getInstance("Katakana-Latin; Lower") }

actual fun katakanaToLatinLower(text: String): String = katakanaLatin.transliterate(text)
