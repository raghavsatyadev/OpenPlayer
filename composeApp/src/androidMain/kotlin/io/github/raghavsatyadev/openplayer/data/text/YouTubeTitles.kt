package io.github.raghavsatyadev.openplayer.data.text

/**
 * Cleans YouTube and YouTube Music titles and channel names for matching against library songs.
 *
 * Generic and pure Kotlin with no Android dependencies so it can be reused by metadata lookups.
 */
object YouTubeTitles {

    private val trailingPipe = Regex("""\s*\|.*$""")
    private val trailingOfficialVideo = Regex("""(?:\s*[-–—]\s*|\s+)official\s+video$""", RegexOption.IGNORE_CASE)
    private val bracketPattern = Regex("""\s*(\([^\)]*\)|\[[^\]]*\])""")
    private val spaces = Regex("""\s+""")

    private val noiseTokens = setOf(
        "official", "music", "video", "audio", "visualizer", "visualiser",
        "lyric", "lyrics", "hd", "hq", "4k", "mv", "explicit", "clean", "version"
    )

    private val coreNoiseIndicators = setOf(
        "video", "audio", "visualizer", "visualiser", "lyric", "lyrics",
        "hd", "hq", "4k", "mv", "explicit", "clean"
    )

    /**
     * Trims [name], drops trailing " - Topic" (case-insensitive), trailing "VEVO" glued or
     * separated ("TheWeekndVEVO" -> "TheWeeknd", "Adele VEVO" -> "Adele"), and trailing
     * " Official" / "Official Channel".
     */
    fun cleanChannel(name: String): String {
        var s = name.trim()
        if (s.endsWith(" - Topic", ignoreCase = true)) {
            s = s.substring(0, s.length - 8).trim()
        }
        if (s.endsWith(" Official Channel", ignoreCase = true)) {
            s = s.substring(0, s.length - 17).trim()
        } else if (s.endsWith("Official Channel", ignoreCase = true) && s.length > 16) {
            s = s.substring(0, s.length - 16).trim()
        } else if (s.endsWith(" Official", ignoreCase = true)) {
            s = s.substring(0, s.length - 9).trim()
        }
        if (s.endsWith(" VEVO", ignoreCase = true)) {
            s = s.substring(0, s.length - 5).trim()
        } else if (s.endsWith("VEVO", ignoreCase = true) && s.length > 4) {
            s = s.substring(0, s.length - 4).trim()
        }
        return s.ifEmpty { name.trim() }
    }

    /**
     * Drops bracketed or parenthesised video noise (official video/audio, lyric video, audio,
     * visualizer, HD/HQ/4K, MV, explicit/clean; "(Live)" is preserved), trailing "| ..." segments
     * and trailing unbracketed "Official Video". Collapses spaces and falls back to the trimmed
     * input if cleaning would leave it blank.
     */
    fun cleanTitle(title: String): String {
        var s = title.trim()
        s = trailingPipe.replace(s, "")
        s = bracketPattern.replace(s) { match ->
            if (isNoiseBracket(match.groupValues[1])) "" else match.value
        }
        s = trailingPipe.replace(s, "")
        s = trailingOfficialVideo.replace(s, "")
        s = spaces.replace(s, " ").trim()
        return s.ifEmpty { title.trim() }
    }

    private fun isNoiseBracket(bracket: String): Boolean {
        val inner = bracket.substring(1, bracket.length - 1).trim()
        if (inner.isEmpty()) return true
        if (inner.contains("live", ignoreCase = true)) return false
        val normalized = inner.lowercase().replace("m/v", "mv")
        val tokens = normalized.split(Regex("""[^a-z0-9]+""")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return true
        return tokens.all { it in noiseTokens } && tokens.any { it in coreNoiseIndicators }
    }

    /**
     * Splits "Artist - Song" titles (separator " - ", " – " or " — ", first occurrence) into
     * artist and song. Returns null when [channel] is non-blank and does not match the left part
     * (folded lowercase alphanumeric comparison in either direction), keeping titles with dashes
     * whole.
     */
    fun splitArtistTitle(title: String, channel: String?): Pair<String, String>? {
        val separators = listOf(" - ", " \u2013 ", " \u2014 ")
        val firstMatch = separators.mapNotNull { sep ->
            val idx = title.indexOf(sep)
            if (idx >= 0) idx to sep else null
        }.minByOrNull { it.first } ?: return null

        val (index, sep) = firstMatch
        val left = title.substring(0, index).trim()
        val right = title.substring(index + sep.length).trim()
        if (left.isEmpty() || right.isEmpty()) return null

        if (channel.isNullOrBlank()) {
            return Pair(left, right)
        }

        val cleanedChannel = cleanChannel(channel)
        val foldedChannel = fold(cleanedChannel)
        val foldedLeft = fold(left)

        if (foldedLeft.isNotEmpty() && foldedChannel.isNotEmpty() &&
            (foldedLeft.contains(foldedChannel) || foldedChannel.contains(foldedLeft))
        ) {
            return Pair(left, right)
        }

        return null
    }

    private fun fold(text: String): String =
        text.lowercase().filter { it.isLetterOrDigit() }
}
