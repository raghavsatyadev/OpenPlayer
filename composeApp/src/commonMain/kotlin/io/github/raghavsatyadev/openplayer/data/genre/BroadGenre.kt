package io.github.raghavsatyadev.openplayer.data.genre

import io.github.raghavsatyadev.openplayer.data.playlist.nlp.GenreFamily
import io.github.raghavsatyadev.openplayer.data.playlist.nlp.GenreTaxonomy
import io.github.raghavsatyadev.openplayer.data.playlist.nlp.NlpText

/**
 * Turns a genre label from the artist table ("southern hip-hop", "nu metal") or an online store
 * ("Hip-Hop/Rap", "R&B/Soul") into one broad, title-cased name ("Hip-Hop", "Metal", "R&B").
 * Filled-in genres then form a few browsable tiles instead of dozens of near-duplicates, and both
 * sources agree on the names. Genres read from song files are never passed through here.
 */
object BroadGenre {

    private class Rule(val name: String, pattern: String) {
        val regex = Regex("""(^|[^a-z])($pattern)($|[^a-z])""")
    }

    // Order matters: the first match wins, so a specific form precedes the broad one it contains
    // ("dance-punk" is punk, "dance pop" is pop, "indian pop" is Bollywood).
    private val RULES = listOf(
        Rule("Metal", "metal|metalcore|deathcore|djent"),
        Rule("Punk", "punk|hardcore punk|pop punk"),
        Rule("Hip-Hop", "hip hop|hip-hop|rap|trap|drill|grime|phonk|crunk|bounce|dirty south"),
        Rule("K-Pop", "k-pop|k pop|kpop"),
        Rule("J-Pop", "j-pop|j pop|jpop|anime"),
        Rule("Bollywood", "bollywood|filmi|indian pop|indian|desi|bhangra|punjabi"),
        Rule("R&B", "r&b|rnb|rhythm and blues|soul|funk|gospel|motown|neo soul"),
        Rule("Latin", "latin|reggaeton|salsa|bachata|cumbia|merengue|regional mexican|banda|corrido"),
        Rule("Reggae", "reggae|dancehall|ska|dub"),
        Rule("Country", "country|bluegrass|americana"),
        Rule("Folk", "folk|singer-songwriter|singer/songwriter|acoustic"),
        Rule("Jazz", "jazz|swing|bossa nova|bebop"),
        Rule("Blues", "blues"),
        Rule("Classical", "classical|orchestral|orchestra|opera|baroque|symphony|chamber music"),
        Rule("Soundtrack", "soundtrack|film score|score|films/games|video game music"),
        Rule("Ambient", "ambient|lo-fi|lofi|new age|downtempo|chillout|chill"),
        Rule("Pop", "dance pop|dance-pop|synth-pop|synthpop|electropop|teen pop"),
        Rule("Electronic", "electronic|electronica|electro|edm|house|techno|trance|dubstep|" +
            "drum and bass|drum & bass|dnb|dance|idm|hardstyle|garage|breakbeat"),
        Rule("Alternative", "alternative|indie"),
        Rule("Rock", "rock|grunge|emo|shoegaze"),
        Rule("Pop", "pop"),
        Rule("World", "world|afrobeat|afrobeats|amapiano|flamenco"),
    )

    private val FAMILY_NAMES = mapOf(
        GenreFamily.GUITAR to "Rock",
        GenreFamily.URBAN to "Hip-Hop",
        GenreFamily.RNB_SOUL to "R&B",
        GenreFamily.ELECTRONIC to "Electronic",
        GenreFamily.POP to "Pop",
        GenreFamily.LATIN to "Latin",
        GenreFamily.JAZZ to "Jazz",
        GenreFamily.FOLK_COUNTRY to "Folk",
        GenreFamily.CLASSICAL to "Classical",
        GenreFamily.CHILL_AMBIENT to "Ambient",
        GenreFamily.REGGAE to "Reggae",
        GenreFamily.WORLD to "World",
    )

    /**
     * The broad name for [label], or null for a blank label. A label no rule or taxonomy family
     * knows (an online store's "Comedy", "Devotional") is kept, title-cased.
     */
    fun of(label: String?): String? {
        val trimmed = label?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        val normalized = NlpText.normalize(trimmed)
        RULES.firstOrNull { it.regex.containsMatchIn(normalized) }?.let { return it.name }
        GenreTaxonomy.resolve(normalized)?.let { entry -> FAMILY_NAMES[entry.family]?.let { return it } }
        return titleCase(trimmed)
    }

    private fun titleCase(text: String): String =
        text.split(' ').joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}
