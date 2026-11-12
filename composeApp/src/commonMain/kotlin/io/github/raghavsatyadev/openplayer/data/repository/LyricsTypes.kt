package io.github.raghavsatyadev.openplayer.data.repository

import io.github.raghavsatyadev.openplayer.data.model.Lyrics
import io.github.raghavsatyadev.openplayer.data.network.lyrics.LrcLibResponse

data class LyricsSearchResult(val record: LrcLibResponse, val lyrics: Lyrics, val rawLyrics: String)

data class NoLyricsFoundException(val query: String? = null) : Exception()

class LyricsException(message: String, cause: Throwable? = null) : Exception(message, cause)
