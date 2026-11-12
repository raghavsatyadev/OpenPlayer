package io.github.raghavsatyadev.openplayer.data.history.importer

import java.text.Normalizer

/** Loose text folding for matching titles and artists between a service and the library. */
internal object MatchFolding {

    private val combiningMarks = Regex("""\p{Mn}+""")
    private val brackets = Regex("""\[[^\]]*]""")
    private val featParens = Regex("""\((?:feat|ft|featuring|with)\b[^)]*\)""")
    private val versionParens = Regex(
        """\((?:\d{4}\s+)?(?:remaster(?:ed)?|digital remaster|deluxe|bonus track|mono|stereo)\b[^)]*\)"""
    )
    private val dashSuffix = Regex("""\s+-\s+(?:\d{4}\s+)?(?:remaster(?:ed)?|digital remaster|mono|stereo)\b.*$""")
    private val dashFeat = Regex("""\s+-\s+(?:feat|ft|featuring)\b.*$""")
    private val featTail = Regex("""\s+(?:feat|ft|featuring)\b\.?\s.*$""")
    private val punctuation = Regex("""[^\p{L}\p{N}]+""")
    private val artistSeparators = Regex("""\s*(?:,|;|/|&|\bfeat\b\.?|\bft\b\.?|\bfeaturing\b)\s*""", RegexOption.IGNORE_CASE)

    /** Plain comparison: trim and lowercase, as the backup resolver does. */
    fun exact(text: String): String = text.trim().lowercase()

    /**
     * Lowercase, accents stripped, "(feat. ...)", "[...]", "- Remastered ..." and "feat. ..." tails
     * dropped, punctuation collapsed to single spaces.
     */
    fun fold(text: String): String {
        var s = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFKD)
        s = combiningMarks.replace(s, "")
        s = brackets.replace(s, " ")
        s = featParens.replace(s, " ")
        s = versionParens.replace(s, " ")
        s = dashSuffix.replace(s, "")
        s = dashFeat.replace(s, "")
        s = featTail.replace(s, "")
        s = s.replace("&", " and ")
        s = punctuation.replace(s, " ").trim()
        return s.ifEmpty { text.trim().lowercase() }
    }

    /** The individual artists in a credit like "A & B feat. C"; folded, without the whole credit. */
    fun splitArtists(credit: String): List<String> {
        val parts = credit.split(artistSeparators).map { fold(it) }.filter { it.isNotEmpty() }
        return if (parts.size > 1) parts.distinct() else emptyList()
    }
}
