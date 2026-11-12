package io.github.raghavsatyadev.openplayer.data.video

import java.text.Normalizer

/**
 * Text helpers for matching video filenames to tracks.
 *
 * Text is compared without accents and case. Apostrophes are dropped ("don't" == "dont").
 * CJK has no spaces between words, so a CJK run is also split into overlapping character pairs.
 */
internal object VideoNameText {

    private val wordRegex = Regex("[\\p{L}\\p{M}\\p{N}']+")
    private val combiningMarks = Regex("\\p{Mn}+")

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(combiningMarks, "").lowercase()

    fun isCjk(c: Char): Boolean {
        val code = c.code
        return code in 0x3005..0x9FFF || code in 0xF900..0xFAFF || code in 0xFF66..0xFF9F
    }

    private fun hasCjk(word: String): Boolean = word.any(::isCjk)

    /** Tokens of a search text. */
    fun tokenize(text: String): Set<String> = tokenize(text, isHaystack = false)

    /** Tokens of a text that others are searched in: every CJK character is also a token. */
    fun tokenizeHaystack(text: String): Set<String> = tokenize(text, isHaystack = true)

    private fun tokenize(text: String, isHaystack: Boolean): Set<String> {
        val tokens = LinkedHashSet<String>()
        forEachWord(text) { word, _ ->
            if (hasCjk(word)) addCjkAwareTokens(tokens, word, isHaystack) else tokens.add(word)
        }
        return tokens
    }

    private fun addCjkAwareTokens(tokens: MutableSet<String>, word: String, isHaystack: Boolean) {
        var start = 0
        while (start < word.length) {
            val runIsCjk = isCjk(word[start])
            var end = start + 1
            while (end < word.length && isCjk(word[end]) == runIsCjk) end++
            if (!runIsCjk) {
                tokens.add(word.substring(start, end))
            } else {
                if (isHaystack || end - start == 1) {
                    for (i in start until end) tokens.add(word[i].toString())
                }
                for (i in start + 1 until end) tokens.add(word.substring(i - 1, i + 1))
            }
            start = end
        }
    }

    /**
     * Calls [onWord] for each word of [text], normalized. `isAfterSymbol` is true when a symbol
     * (anything other than spaces) separates the word from the previous one, like " - ".
     */
    fun forEachWord(text: String, onWord: (word: String, isAfterSymbol: Boolean) -> Unit) {
        val cleaned = normalize(text)
        var previousEnd = 0
        for (match in wordRegex.findAll(cleaned)) {
            val gap = cleaned.substring(previousEnd, match.range.first)
            previousEnd = match.range.last + 1
            val isAfterSymbol = gap.isNotBlank()
            val word = match.value.replace("'", "")
            if (word.isEmpty()) continue
            onWord(word, isAfterSymbol)
        }
    }

    /** Appends [word], with a space wherever it switches between CJK and other scripts. */
    fun appendWord(builder: StringBuilder, word: String) {
        if (!hasCjk(word)) {
            builder.append(word)
            return
        }
        var wasCjk: Boolean? = null
        for (c in word) {
            val cjk = isCjk(c)
            if (wasCjk != null && cjk != wasCjk) builder.append(' ')
            builder.append(c)
            wasCjk = cjk
        }
    }

    /** Words of [text] separated by single spaces, with a space at both ends. */
    fun toWordsText(text: String): String {
        val builder = StringBuilder(" ")
        forEachWord(text) { word, _ ->
            appendWord(builder, word)
            builder.append(' ')
        }
        return builder.toString()
    }

    fun filenameWithoutExtension(path: String): String {
        val name = path.substringAfterLast('/').substringAfterLast('\\')
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) else name
    }

    fun directoryOf(path: String): String {
        val slash = maxOf(path.lastIndexOf('/'), path.lastIndexOf('\\'))
        return if (slash >= 0) path.substring(0, slash) else ""
    }
}

/**
 * A set of phrases that must all appear, as whole runs of words, in a video's name.
 *
 * Phrases are split where the source texts have symbols, so "Artist - Title" gives two phrases.
 * Longest phrases are checked first and their words are then marked used, so a short phrase
 * can't reuse the words of a longer one that contains it.
 */
internal class PhrasesQuery private constructor(private val phrases: List<Phrase>) {

    private data class Phrase(val text: String, val usedMark: String)

    fun isContainedIn(wordsText: String): Boolean {
        if (phrases.isEmpty()) return false
        var unused = wordsText
        for (phrase in phrases) {
            val index = unused.indexOf(phrase.text)
            if (index == -1) return false
            unused = unused.replaceRange(index, index + phrase.text.length, phrase.usedMark)
        }
        return true
    }

    companion object {
        private const val USED_MARK = "|"

        fun fromTexts(texts: List<String>): PhrasesQuery {
            val phrases = mutableListOf<Phrase>()
            val buffer = StringBuilder()

            fun addPhrase() {
                if (buffer.isEmpty()) return
                val words = buffer.toString()
                buffer.clear()
                // A CJK edge can sit inside a longer run, so it gets no space padding.
                val startPad = if (VideoNameText.isCjk(words.first())) "" else " "
                val endPad = if (VideoNameText.isCjk(words.last())) "" else " "
                phrases.add(Phrase("$startPad$words$endPad", "$startPad$USED_MARK$endPad"))
            }

            for (text in texts) {
                VideoNameText.forEachWord(text) { word, isAfterSymbol ->
                    if (isAfterSymbol) addPhrase()
                    if (buffer.isNotEmpty()) buffer.append(' ')
                    VideoNameText.appendWord(buffer, word)
                }
                addPhrase()
            }
            phrases.sortByDescending { it.text.length }
            return PhrasesQuery(phrases)
        }
    }
}
