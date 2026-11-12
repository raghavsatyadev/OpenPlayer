package io.github.raghavsatyadev.openplayer.data.playlist.rules

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.repository.SongUserMeta
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** Rule evaluation and storage on plain objects; no database, library or clock involved. */
@RunWith(AndroidJUnit4::class)
class SmartRuleEvaluatorTest {

    private val now = 1_800_000_000_000L
    private fun daysAgo(days: Long) = now - TimeUnit.DAYS.toMillis(days)

    private fun song(
        id: String,
        title: String = "Song $id",
        artist: String = "Artist",
        album: String = "Album",
        genre: String? = null,
        path: String = "/music/$id.mp3",
        year: Int = 2000,
        durationSec: Long = 200,
        dateAddedMs: Long = daysAgo(100),
    ) = Song.emptySong().copy(
        id = id, title = title, artist = artist, album = album, genre = genre, path = path,
        year = year, duration = durationSec * 1000, dateAdded = dateAddedMs,
    )

    private fun context(
        playCounts: Map<String, Int> = emptyMap(),
        lastPlayed: Map<String, Long> = emptyMap(),
        meta: Map<String, SongUserMeta> = emptyMap(),
        favorites: Set<String> = emptySet(),
    ) = SmartRuleContext(playCounts, lastPlayed, meta, favorites, now, Random(1))

    private fun cond(
        field: SmartRuleField,
        op: SmartRuleOperator = field.defaultOperator,
        text: String = "",
        min: Long = 0,
        max: Long = 0,
        number: Long = 0,
    ) = SmartCondition(1, field, op, text, min, max, number)

    private fun ids(
        def: SmartRuleDefinition,
        songs: List<Song>,
        ctx: SmartRuleContext = context(),
    ) = SmartRuleEvaluator.evaluate(def, songs, ctx).map { it.id }

    private fun one(condition: SmartCondition, songs: List<Song>, ctx: SmartRuleContext = context()) =
        ids(SmartRuleDefinition(conditions = listOf(condition)), songs, ctx)

    @Test
    fun textOperators_areCaseInsensitive() {
        val songs = listOf(song("a", title = "Blue Monday"), song("b", title = "Red"), song("c", title = "blue"))
        assertThat(one(cond(SmartRuleField.TITLE, SmartRuleOperator.CONTAINS, "BLUE"), songs))
            .containsExactly("a", "c").inOrder()
        assertThat(one(cond(SmartRuleField.TITLE, SmartRuleOperator.NOT_CONTAINS, "blue"), songs))
            .containsExactly("b")
        assertThat(one(cond(SmartRuleField.TITLE, SmartRuleOperator.EQUALS, "BLUE"), songs))
            .containsExactly("c")
    }

    @Test
    fun artistAlbumGenre_matchTheirOwnField() {
        val songs = listOf(
            song("a", artist = "Daft Punk", album = "Discovery", genre = "House"),
            song("b", artist = "Other", album = "Homework", genre = null),
        )
        assertThat(one(cond(SmartRuleField.ARTIST, text = "daft"), songs)).containsExactly("a")
        assertThat(one(cond(SmartRuleField.ALBUM, text = "work"), songs)).containsExactly("b")
        assertThat(one(cond(SmartRuleField.GENRE, text = "house"), songs)).containsExactly("a")
        assertThat(one(cond(SmartRuleField.GENRE, SmartRuleOperator.NOT_CONTAINS, "house"), songs))
            .containsExactly("b")
    }

    @Test
    fun folderPath_matchesTheFolderNotTheFileName() {
        val songs = listOf(
            song("a", path = "/storage/Music/Rock/a.mp3"),
            song("b", path = "C:\\Music\\Jazz\\rock.mp3"),
        )
        assertThat(one(cond(SmartRuleField.FOLDER_PATH, SmartRuleOperator.CONTAINS, "rock"), songs))
            .containsExactly("a")
        assertThat(one(cond(SmartRuleField.FOLDER_PATH, SmartRuleOperator.EQUALS, "/storage/Music/Rock"), songs))
            .containsExactly("a")
    }

    @Test
    fun yearAndDuration_ranges_areInclusive() {
        val songs = listOf(
            song("a", year = 1999, durationSec = 60),
            song("b", year = 2000, durationSec = 120),
            song("c", year = 2010, durationSec = 121),
        )
        assertThat(one(cond(SmartRuleField.YEAR, min = 2000, max = 2010), songs))
            .containsExactly("b", "c").inOrder()
        assertThat(one(cond(SmartRuleField.DURATION, min = 60, max = 120), songs))
            .containsExactly("a", "b").inOrder()
    }

    @Test
    fun playCount_atLeastAndAtMost_treatMissingAsZero() {
        val songs = listOf(song("a"), song("b"), song("c"))
        val ctx = context(playCounts = mapOf("a" to 5, "b" to 2))
        assertThat(one(cond(SmartRuleField.PLAY_COUNT, SmartRuleOperator.AT_LEAST, number = 2), songs, ctx))
            .containsExactly("a", "b").inOrder()
        assertThat(one(cond(SmartRuleField.PLAY_COUNT, SmartRuleOperator.AT_MOST, number = 2), songs, ctx))
            .containsExactly("b", "c").inOrder()
    }

    @Test
    fun lastPlayed_withinAndNotWithin_neverPlayedCountsAsNotWithin() {
        val songs = listOf(song("a"), song("b"), song("never"))
        val ctx = context(lastPlayed = mapOf("a" to daysAgo(3), "b" to daysAgo(40)))
        assertThat(one(cond(SmartRuleField.LAST_PLAYED, SmartRuleOperator.WITHIN_DAYS, number = 7), songs, ctx))
            .containsExactly("a")
        assertThat(one(cond(SmartRuleField.LAST_PLAYED, SmartRuleOperator.NOT_WITHIN_DAYS, number = 7), songs, ctx))
            .containsExactly("b", "never").inOrder()
    }

    @Test
    fun dateAdded_readsBothSecondsAndMillis() {
        val songs = listOf(
            song("secs", dateAddedMs = TimeUnit.MILLISECONDS.toSeconds(daysAgo(2))),
            song("millis", dateAddedMs = daysAgo(2)),
            song("old", dateAddedMs = daysAgo(90)),
            song("unknown", dateAddedMs = 0),
        )
        assertThat(one(cond(SmartRuleField.DATE_ADDED, SmartRuleOperator.WITHIN_DAYS, number = 7), songs))
            .containsExactly("secs", "millis").inOrder()
    }

    @Test
    fun rating_comesFromUserMeta_unratedIsZero() {
        val songs = listOf(song("a"), song("b"), song("c"))
        val ctx = context(meta = mapOf("a" to SongUserMeta("a", rating = 5), "b" to SongUserMeta("b", rating = 3)))
        assertThat(one(cond(SmartRuleField.RATING, SmartRuleOperator.AT_LEAST, number = 4), songs, ctx))
            .containsExactly("a")
        assertThat(one(cond(SmartRuleField.RATING, SmartRuleOperator.AT_MOST, number = 3), songs, ctx))
            .containsExactly("b", "c").inOrder()
    }

    @Test
    fun moodAndTag_hasAndHasNot_ignoreCase() {
        val songs = listOf(song("a"), song("b"), song("c"))
        val ctx = context(
            meta = mapOf(
                "a" to SongUserMeta("a", moods = listOf("Chill"), tags = listOf("gym")),
                "b" to SongUserMeta("b", moods = listOf("Sad"), tags = listOf("Gym", "road")),
            )
        )
        assertThat(one(cond(SmartRuleField.MOOD, SmartRuleOperator.HAS, "chill"), songs, ctx))
            .containsExactly("a")
        assertThat(one(cond(SmartRuleField.MOOD, SmartRuleOperator.HAS_NOT, "chill"), songs, ctx))
            .containsExactly("b", "c").inOrder()
        assertThat(one(cond(SmartRuleField.TAG, SmartRuleOperator.HAS, "GYM"), songs, ctx))
            .containsExactly("a", "b").inOrder()
        assertThat(one(cond(SmartRuleField.TAG, SmartRuleOperator.HAS_NOT, "gym"), songs, ctx))
            .containsExactly("c")
    }

    @Test
    fun favorite_isAndIsNot() {
        val songs = listOf(song("a"), song("b"))
        val ctx = context(favorites = setOf("a"))
        assertThat(one(cond(SmartRuleField.FAVORITE, SmartRuleOperator.IS), songs, ctx)).containsExactly("a")
        assertThat(one(cond(SmartRuleField.FAVORITE, SmartRuleOperator.IS_NOT), songs, ctx)).containsExactly("b")
    }

    @Test
    fun matchAll_needsEveryCondition_matchAny_needsOne() {
        val songs = listOf(
            song("a", artist = "X", year = 1990),
            song("b", artist = "X", year = 2020),
            song("c", artist = "Y", year = 1990),
            song("d", artist = "Y", year = 2020),
        )
        val conditions = listOf(
            cond(SmartRuleField.ARTIST, text = "X"),
            cond(SmartRuleField.YEAR, min = 1980, max = 1999),
        )
        assertThat(ids(SmartRuleDefinition(SmartMatchMode.ALL, conditions), songs)).containsExactly("a")
        assertThat(ids(SmartRuleDefinition(SmartMatchMode.ANY, conditions), songs))
            .containsExactly("a", "b", "c").inOrder()
    }

    @Test
    fun noConditions_matchesEverySong() {
        val songs = listOf(song("a"), song("b"))
        assertThat(ids(SmartRuleDefinition(), songs)).containsExactly("a", "b").inOrder()
    }

    @Test
    fun sort_everyField_bothDirections() {
        val songs = listOf(
            song("a", title = "b", artist = "z", album = "m", year = 2005, dateAddedMs = daysAgo(3)),
            song("b", title = "C", artist = "x", album = "k", year = 1995, dateAddedMs = daysAgo(1)),
            song("c", title = "a", artist = "y", album = "l", year = 2015, dateAddedMs = daysAgo(2)),
        )
        val ctx = context(
            playCounts = mapOf("a" to 1, "b" to 9, "c" to 5),
            lastPlayed = mapOf("a" to daysAgo(1), "b" to daysAgo(9), "c" to daysAgo(5)),
            meta = mapOf("a" to SongUserMeta("a", rating = 2), "b" to SongUserMeta("b", rating = 5)),
        )
        fun sorted(field: SmartSortField, desc: Boolean = false) =
            ids(SmartRuleDefinition(sort = SmartSort(field, desc)), songs, ctx)

        assertThat(sorted(SmartSortField.TITLE)).containsExactly("c", "a", "b").inOrder()
        assertThat(sorted(SmartSortField.ARTIST)).containsExactly("b", "c", "a").inOrder()
        assertThat(sorted(SmartSortField.ALBUM)).containsExactly("b", "c", "a").inOrder()
        assertThat(sorted(SmartSortField.YEAR)).containsExactly("b", "a", "c").inOrder()
        assertThat(sorted(SmartSortField.DATE_ADDED, true)).containsExactly("b", "c", "a").inOrder()
        assertThat(sorted(SmartSortField.PLAY_COUNT, true)).containsExactly("b", "c", "a").inOrder()
        assertThat(sorted(SmartSortField.LAST_PLAYED, true)).containsExactly("a", "c", "b").inOrder()
        assertThat(sorted(SmartSortField.RATING, true)).containsExactly("b", "a", "c").inOrder()
        assertThat(sorted(SmartSortField.YEAR, true)).containsExactly("c", "a", "b").inOrder()
    }

    @Test
    fun sortTies_keepLibraryOrder() {
        val songs = listOf(song("a", year = 2000), song("b", year = 2000), song("c", year = 1990))
        val def = SmartRuleDefinition(sort = SmartSort(SmartSortField.YEAR, descending = true))
        assertThat(ids(def, songs)).containsExactly("a", "b", "c").inOrder()
    }

    @Test
    fun randomSort_isAPermutation_andStableForTheSameSeed() {
        val songs = (1..30).map { song("s$it") }
        val def = SmartRuleDefinition(sort = SmartSort(SmartSortField.RANDOM))
        val first = SmartRuleEvaluator.evaluate(def, songs, context().copy(random = Random(7))).map { it.id }
        val again = SmartRuleEvaluator.evaluate(def, songs, context().copy(random = Random(7))).map { it.id }
        assertThat(first).containsExactlyElementsIn(songs.map { it.id })
        assertThat(first).isEqualTo(again)
        assertThat(first).isNotEqualTo(songs.map { it.id })
    }

    @Test
    fun limit_capsTheSortedResult() {
        val songs = listOf(song("a"), song("b"), song("c"))
        val ctx = context(playCounts = mapOf("a" to 1, "b" to 3, "c" to 2))
        val def = SmartRuleDefinition(sort = SmartSort(SmartSortField.PLAY_COUNT, true), limit = 2)
        assertThat(ids(def, songs, ctx)).containsExactly("b", "c").inOrder()
        assertThat(ids(def.copy(limit = null), songs, ctx)).hasSize(3)
        assertThat(ids(def.copy(limit = 10), songs, ctx)).hasSize(3)
    }

    @Test
    fun codec_roundTripsADefinition_throughTheSourceString() {
        val definition = SmartRuleDefinition(
            matchMode = SmartMatchMode.ANY,
            conditions = listOf(
                cond(SmartRuleField.TITLE, SmartRuleOperator.NOT_CONTAINS, text = "live \"remix\""),
                cond(SmartRuleField.YEAR, min = 1990, max = 1999),
                cond(SmartRuleField.LAST_PLAYED, SmartRuleOperator.NOT_WITHIN_DAYS, number = 30),
            ).mapIndexed { index, c -> c.copy(id = index.toLong()) },
            sort = SmartSort(SmartSortField.RATING, descending = true),
            limit = 50,
        )
        val source = SmartRuleCodec.toSource(definition)
        assertThat(source).startsWith("SMART:")
        assertThat(SmartRuleCodec.isRuleSource(source)).isTrue()
        assertThat(SmartRuleCodec.fromSource(source)).isEqualTo(definition)
    }

    @Test
    fun codec_toleratesJunk() {
        assertThat(SmartRuleCodec.fromSource("LOCAL")).isNull()
        assertThat(SmartRuleCodec.fromSource("SMART:top_played")).isNull()
        assertThat(SmartRuleCodec.fromSource(SmartRuleCodec.SOURCE_PREFIX + "not json")).isNull()
        val partial = SmartRuleCodec.decode(
            """{"conditions":[{"field":"NOPE"},{"field":"RATING","op":"HAS"}],"sort":{"field":"X"}}"""
        )
        // Unknown field dropped; an operator that doesn't fit the field falls back to its default.
        assertThat(partial?.conditions?.map { it.operator }).containsExactly(SmartRuleOperator.AT_LEAST)
        assertThat(partial?.sort).isNull()
        assertThat(partial?.matchMode).isEqualTo(SmartMatchMode.ALL)
    }
}
