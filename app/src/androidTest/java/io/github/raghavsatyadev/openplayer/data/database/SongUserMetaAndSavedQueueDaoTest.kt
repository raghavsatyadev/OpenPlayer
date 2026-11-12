package io.github.raghavsatyadev.openplayer.data.database

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.repository.SavedQueueRepository
import io.github.raghavsatyadev.openplayer.data.repository.SongUserMeta
import io.github.raghavsatyadev.openplayer.data.repository.SongUserMetaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The rating/moods/tags and saved-queue tables, their repositories and codecs, on in-memory Room. */
@RunWith(AndroidJUnit4::class)
class SongUserMetaAndSavedQueueDaoTest {

    private lateinit var database: OpenPlayerDatabase
    private lateinit var metaRepository: SongUserMetaRepository
    private lateinit var queueRepository: SavedQueueRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, OpenPlayerDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .allowMainThreadQueries()
            .build()
        metaRepository = SongUserMetaRepository(database.songUserMetaDao())
        queueRepository = SavedQueueRepository(database.savedQueueDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun savedMetaIsReadBackCleanedAndClamped() = runTest {
        metaRepository.save(
            SongUserMeta("1", rating = 9, moods = listOf(" Calm ", "calm", "", "Happy"), tags = listOf("gym"))
        )

        val meta = metaRepository.get("1")!!
        assertThat(meta.rating).isEqualTo(5)
        assertThat(meta.moods).containsExactly("Calm", "Happy").inOrder()
        assertThat(meta.tags).containsExactly("gym")
        assertThat(metaRepository.observe("1").first()).isEqualTo(meta)
    }

    @Test
    fun clearingEverythingRemovesTheRow() = runTest {
        metaRepository.save(SongUserMeta("1", rating = 3))
        metaRepository.save(SongUserMeta("1"))

        assertThat(metaRepository.get("1")).isNull()
        assertThat(database.songUserMetaDao().getAllOnce()).isEmpty()
    }

    @Test
    fun valuesWithSeparatorsAndQuotesSurviveTheEncoding() {
        val values = listOf("rock, pop", "a\nb", "say \"hi\"", "[x]")
        assertThat(SongUserMetaCodec.decode(SongUserMetaCodec.encode(values)))
            .containsExactlyElementsIn(values).inOrder()
        assertThat(SongUserMetaCodec.decode("not json")).isEmpty()
        assertThat(SongUserMetaCodec.decode("")).isEmpty()
    }

    @Test
    fun suggestionsAreRankedByUseAndMergedIgnoringCase() = runTest {
        metaRepository.save(SongUserMeta("1", moods = listOf("Calm", "Happy"), tags = listOf("gym")))
        metaRepository.save(SongUserMeta("2", moods = listOf("calm")))
        metaRepository.save(SongUserMeta("3", moods = listOf("Sad", "Calm")))

        assertThat(metaRepository.moodSuggestions()).containsExactly("Calm", "Happy", "Sad").inOrder()
        assertThat(metaRepository.tagSuggestions()).containsExactly("gym")
    }

    @Test
    fun ratingsFlowSkipsUnratedSongs() = runTest {
        metaRepository.save(SongUserMeta("1", rating = 4))
        metaRepository.save(SongUserMeta("2", moods = listOf("Calm")))

        assertThat(metaRepository.observeRatings().first()).containsExactly("1", 4)
    }

    @Test
    fun savedQueueKeepsOrderRepeatsIndexAndPosition() = runTest {
        val id = queueRepository.save(
            "Road trip", listOf("3", "1", "3", "2"), currentIndex = 2, positionMs = 91_000L, now = 10L
        )

        val queue = queueRepository.get(id)!!
        assertThat(queue.name).isEqualTo("Road trip")
        assertThat(queue.songIds).containsExactly("3", "1", "3", "2").inOrder()
        assertThat(queue.currentIndex).isEqualTo(2)
        assertThat(queue.positionMs).isEqualTo(91_000L)
        assertThat(queue.createdAt).isEqualTo(10L)
    }

    @Test
    fun queuesListNewestFirstAndDeleteRemovesOnlyOne() = runTest {
        val first = queueRepository.save("old", listOf("1"), 0, 0L, now = 1L)
        val second = queueRepository.save("new", listOf("2"), 0, 0L, now = 2L)

        assertThat(queueRepository.observeAll().first().map { it.name }).containsExactly("new", "old").inOrder()

        queueRepository.delete(second)
        assertThat(queueRepository.observeAll().first().map { it.id }).containsExactly(first)
    }

    @Test
    fun emptyQueueIsNotSavedAndBadIndexIsClamped() = runTest {
        assertThat(queueRepository.save("none", emptyList(), 0, 0L)).isEqualTo(0L)
        assertThat(database.savedQueueDao().getAllOnce()).isEmpty()

        val id = queueRepository.save("clamp", listOf("1", "2"), currentIndex = 99, positionMs = -5L)
        val queue = queueRepository.get(id)!!
        assertThat(queue.currentIndex).isEqualTo(1)
        assertThat(queue.positionMs).isEqualTo(0L)
    }
}
