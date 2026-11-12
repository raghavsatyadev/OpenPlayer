package io.github.raghavsatyadev.openplayer.data.playlist.rules

import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.repository.SongUserMeta
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** Everything besides the songs themselves that conditions and sorting look up by song id. */
data class SmartRuleContext(
    val playCounts: Map<String, Int> = emptyMap(),
    /** Epoch millis; songs never played are missing or 0. */
    val lastPlayed: Map<String, Long> = emptyMap(),
    val userMeta: Map<String, SongUserMeta> = emptyMap(),
    val favoriteIds: Set<String> = emptySet(),
    val now: Long = System.currentTimeMillis(),
    val random: Random = Random.Default,
)

/** Pure evaluation of a [SmartRuleDefinition]; no Android, database or clock access. */
object SmartRuleEvaluator {

    /** The songs matching [definition], sorted and capped as it says. Library order if unsorted. */
    fun evaluate(
        definition: SmartRuleDefinition,
        songs: List<Song>,
        context: SmartRuleContext,
    ): List<Song> {
        val matched = songs.filter { matches(definition, it, context) }
        val sorted = definition.sort?.let { sort(matched, it, context) } ?: matched
        val limit = definition.limit
        return if (limit != null && limit > 0) sorted.take(limit) else sorted
    }

    fun matches(definition: SmartRuleDefinition, song: Song, context: SmartRuleContext): Boolean {
        val conditions = definition.conditions
        if (conditions.isEmpty()) return true
        return when (definition.matchMode) {
            SmartMatchMode.ALL -> conditions.all { matches(it, song, context) }
            SmartMatchMode.ANY -> conditions.any { matches(it, song, context) }
        }
    }

    fun matches(condition: SmartCondition, song: Song, context: SmartRuleContext): Boolean {
        val op = condition.operator
        return when (condition.field) {
            SmartRuleField.TITLE -> text(op, song.title, condition.text)
            SmartRuleField.ARTIST -> text(op, artistNames(song), condition.text)
            SmartRuleField.ALBUM -> text(op, song.album, condition.text)
            SmartRuleField.GENRE -> text(op, song.genre.orEmpty(), condition.text)
            SmartRuleField.FOLDER_PATH -> text(op, folderOf(song.path), condition.text)
            SmartRuleField.YEAR -> song.year.toLong() in condition.min..condition.max
            SmartRuleField.DURATION ->
                TimeUnit.MILLISECONDS.toSeconds(song.duration) in condition.min..condition.max
            SmartRuleField.PLAY_COUNT ->
                compare(op, (context.playCounts[song.id] ?: 0).toLong(), condition.number)
            SmartRuleField.LAST_PLAYED ->
                within(op, context.lastPlayed[song.id] ?: 0L, condition.number, context.now)
            SmartRuleField.DATE_ADDED ->
                within(op, dateAddedMillis(song.dateAdded), condition.number, context.now)
            SmartRuleField.RATING ->
                compare(op, (context.userMeta[song.id]?.rating ?: 0).toLong(), condition.number)
            SmartRuleField.MOOD -> has(op, context.userMeta[song.id]?.moods, condition.text)
            SmartRuleField.TAG -> has(op, context.userMeta[song.id]?.tags, condition.text)
            SmartRuleField.FAVORITE -> {
                val isFavorite = song.id in context.favoriteIds
                if (op == SmartRuleOperator.IS_NOT) !isFavorite else isFavorite
            }
        }
    }

    fun sort(songs: List<Song>, sort: SmartSort, context: SmartRuleContext): List<Song> {
        if (sort.field == SmartSortField.RANDOM) return songs.shuffled(context.random)
        val comparator: Comparator<Song> = when (sort.field) {
            SmartSortField.TITLE -> compareBy { it.title.lowercase() }
            SmartSortField.ARTIST -> compareBy { artistNames(it).lowercase() }
            SmartSortField.ALBUM -> compareBy { it.album.lowercase() }
            SmartSortField.YEAR -> compareBy { it.year }
            SmartSortField.DATE_ADDED -> compareBy { dateAddedMillis(it.dateAdded) }
            SmartSortField.PLAY_COUNT -> compareBy { context.playCounts[it.id] ?: 0 }
            SmartSortField.LAST_PLAYED -> compareBy { context.lastPlayed[it.id] ?: 0L }
            SmartSortField.RATING -> compareBy { context.userMeta[it.id]?.rating ?: 0 }
            SmartSortField.RANDOM -> error("handled above")
        }
        // Stable: songs that tie keep library order.
        return songs.sortedWith(if (sort.descending) comparator.reversed() else comparator)
    }

    private fun artistNames(song: Song): String =
        if (song.artists.isEmpty()) song.artist else song.artists.joinToString(", ") { it.name }

    private fun folderOf(path: String): String = path.replace('\\', '/').substringBeforeLast('/', "")

    private fun text(op: SmartRuleOperator, value: String, needle: String): Boolean {
        val query = needle.trim()
        return when (op) {
            SmartRuleOperator.CONTAINS -> value.contains(query, ignoreCase = true)
            SmartRuleOperator.NOT_CONTAINS -> !value.contains(query, ignoreCase = true)
            SmartRuleOperator.EQUALS -> value.trim().equals(query, ignoreCase = true)
            else -> false
        }
    }

    private fun compare(op: SmartRuleOperator, value: Long, target: Long): Boolean =
        if (op == SmartRuleOperator.AT_MOST) value <= target else value >= target

    /** Songs with no timestamp (0) are never "within" a window, so they are "not within" it. */
    private fun within(op: SmartRuleOperator, timestamp: Long, days: Long, now: Long): Boolean {
        val isWithin = timestamp > 0L && timestamp >= now - TimeUnit.DAYS.toMillis(days)
        return if (op == SmartRuleOperator.NOT_WITHIN_DAYS) !isWithin else isWithin
    }

    private fun has(op: SmartRuleOperator, values: List<String>?, needle: String): Boolean {
        val found = values.orEmpty().any { it.equals(needle.trim(), ignoreCase = true) }
        return if (op == SmartRuleOperator.HAS_NOT) !found else found
    }

    /** MediaStore stores seconds, the cloud sources millis. */
    private fun dateAddedMillis(dateAdded: Long): Long =
        if (dateAdded in 1 until 10_000_000_000L) TimeUnit.SECONDS.toMillis(dateAdded) else dateAdded
}
