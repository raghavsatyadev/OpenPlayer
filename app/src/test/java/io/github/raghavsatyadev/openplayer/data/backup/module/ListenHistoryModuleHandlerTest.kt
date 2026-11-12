package io.github.raghavsatyadev.openplayer.data.backup.module

import com.google.gson.GsonBuilder
import io.github.raghavsatyadev.openplayer.data.database.ListenHistoryDao
import io.github.raghavsatyadev.openplayer.data.database.ListenHistoryEntity
import io.github.raghavsatyadev.openplayer.data.database.ListenSource
import io.github.raghavsatyadev.openplayer.data.database.MusicDao
import io.github.raghavsatyadev.openplayer.data.database.SongSummary
import io.github.raghavsatyadev.openplayer.data.preferences.ListenHistoryPreferences
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ListenHistoryModuleHandlerTest {
    private val gson = GsonBuilder().serializeNulls().create()
    private val sourceDao = mockk<MusicDao>()
    private val targetDao = mockk<MusicDao>()
    private val dao = mockk<ListenHistoryDao>(relaxed = true)
    private val preferences = mockk<ListenHistoryPreferences>(relaxed = true)

    private fun summary(id: Long, title: String) = SongSummary(id, title, "Artist", "Album", 200_000L)

    private fun listen(
        id: Long,
        songId: String,
        startedAt: Long,
        endedAt: Long,
        listenedMs: Long,
        trackDurationMs: Long,
        source: Int
    ) = ListenHistoryEntity(
        id = id,
        songId = songId,
        startedAt = startedAt,
        endedAt = endedAt,
        listenedMs = listenedMs,
        trackDurationMs = trackDurationMs,
        source = source
    )

    private fun handler(musicDao: MusicDao) =
        ListenHistoryModuleHandler(dao, BackupSongResolver(musicDao), preferences, gson)

    @Test
    fun `export and restore across devices maps song ids and preserves listen fields`() = runTest {
        coEvery { sourceDao.getAllLocalSongSummaries() } returns listOf(
            summary(1L, "One"),
            summary(2L, "Two")
        )
        val listen1A = listen(
            id = 101L,
            songId = "1",
            startedAt = 1000L,
            endedAt = 1200L,
            listenedMs = 200L,
            trackDurationMs = 200_000L,
            source = ListenSource.LIVE
        )
        val listen1B = listen(
            id = 102L,
            songId = "1",
            startedAt = 2000L,
            endedAt = 2300L,
            listenedMs = 300L,
            trackDurationMs = 200_000L,
            source = ListenSource.IMPORTED_SPOTIFY
        )
        val listen2 = listen(
            id = 103L,
            songId = "2",
            startedAt = 3000L,
            endedAt = 3400L,
            listenedMs = 400L,
            trackDurationMs = 200_000L,
            source = ListenSource.IMPORTED_LASTFM
        )
        coEvery { dao.getAllOnce() } returns listOf(listen1A, listen1B, listen2)

        val payload = handler(sourceDao).export()

        coEvery { targetDao.getAllLocalSongSummaries() } returns listOf(
            summary(11L, "One"),
            summary(12L, "Two")
        )
        val replaced = slot<List<ListenHistoryEntity>>()
        coEvery { dao.replaceAll(capture(replaced)) } just Runs

        handler(targetDao).restore(payload)

        assertTrue(replaced.isCaptured)
        val restored = replaced.captured
        assertEquals(3, restored.size)

        assertEquals(0L, restored[0].id)
        assertEquals("11", restored[0].songId)
        assertEquals(1000L, restored[0].startedAt)
        assertEquals(1200L, restored[0].endedAt)
        assertEquals(200L, restored[0].listenedMs)
        assertEquals(200_000L, restored[0].trackDurationMs)
        assertEquals(ListenSource.LIVE, restored[0].source)

        assertEquals(0L, restored[1].id)
        assertEquals("11", restored[1].songId)
        assertEquals(2000L, restored[1].startedAt)
        assertEquals(2300L, restored[1].endedAt)
        assertEquals(300L, restored[1].listenedMs)
        assertEquals(200_000L, restored[1].trackDurationMs)
        assertEquals(ListenSource.IMPORTED_SPOTIFY, restored[1].source)

        assertEquals(0L, restored[2].id)
        assertEquals("12", restored[2].songId)
        assertEquals(3000L, restored[2].startedAt)
        assertEquals(3400L, restored[2].endedAt)
        assertEquals(400L, restored[2].listenedMs)
        assertEquals(200_000L, restored[2].trackDurationMs)
        assertEquals(ListenSource.IMPORTED_LASTFM, restored[2].source)

        coVerify { preferences.markImported() }
    }

    @Test
    fun `a song that does not exist on the target is dropped and the other song's listens are kept`() = runTest {
        coEvery { sourceDao.getAllLocalSongSummaries() } returns listOf(
            summary(1L, "One"),
            summary(2L, "Two")
        )
        val listen1A = listen(
            id = 101L,
            songId = "1",
            startedAt = 1000L,
            endedAt = 1200L,
            listenedMs = 200L,
            trackDurationMs = 200_000L,
            source = ListenSource.LIVE
        )
        val listen1B = listen(
            id = 102L,
            songId = "1",
            startedAt = 2000L,
            endedAt = 2300L,
            listenedMs = 300L,
            trackDurationMs = 200_000L,
            source = ListenSource.IMPORTED_SPOTIFY
        )
        val listen2 = listen(
            id = 103L,
            songId = "2",
            startedAt = 3000L,
            endedAt = 3400L,
            listenedMs = 400L,
            trackDurationMs = 200_000L,
            source = ListenSource.IMPORTED_LASTFM
        )
        coEvery { dao.getAllOnce() } returns listOf(listen1A, listen1B, listen2)

        val payload = handler(sourceDao).export()

        // Target only has song "One" (as id 11); song "Two" does not exist on target
        coEvery { targetDao.getAllLocalSongSummaries() } returns listOf(summary(11L, "One"))
        val replaced = slot<List<ListenHistoryEntity>>()
        coEvery { dao.replaceAll(capture(replaced)) } just Runs

        handler(targetDao).restore(payload)

        assertTrue(replaced.isCaptured)
        val restored = replaced.captured
        assertEquals(2, restored.size)
        assertTrue(restored.all { it.songId == "11" })
        assertEquals(1000L, restored[0].startedAt)
        assertEquals(2000L, restored[1].startedAt)
    }

    @Test
    fun `snapshot then rollback keeps original song ids without calling getAllLocalSongSummaries on the library`() = runTest {
        val listen1 = listen(
            id = 101L,
            songId = "1",
            startedAt = 1000L,
            endedAt = 1200L,
            listenedMs = 200L,
            trackDurationMs = 200_000L,
            source = ListenSource.LIVE
        )
        val listen2 = listen(
            id = 102L,
            songId = "2",
            startedAt = 3000L,
            endedAt = 3400L,
            listenedMs = 400L,
            trackDurationMs = 200_000L,
            source = ListenSource.IMPORTED_LASTFM
        )
        coEvery { dao.getAllOnce() } returns listOf(listen1, listen2)

        val h = handler(targetDao)
        val snapshot = h.snapshot()

        val replaced = slot<List<ListenHistoryEntity>>()
        coEvery { dao.replaceAll(capture(replaced)) } just Runs

        h.rollback(snapshot)

        assertTrue(replaced.isCaptured)
        val restored = replaced.captured
        assertEquals(2, restored.size)
        assertEquals("1", restored[0].songId)
        assertEquals("2", restored[1].songId)
        assertEquals(0L, restored[0].id)
        assertEquals(0L, restored[1].id)
        assertEquals(1000L, restored[0].startedAt)
        assertEquals(3000L, restored[1].startedAt)

        coVerify(exactly = 0) { targetDao.getAllLocalSongSummaries() }
        coVerify { preferences.markImported() }
    }

    @Test
    fun `a malformed listen (array of 3 numbers) is skipped, the rest are restored`() = runTest {
        coEvery { targetDao.getAllLocalSongSummaries() } returns listOf(summary(11L, "One"))
        val replaced = slot<List<ListenHistoryEntity>>()
        coEvery { dao.replaceAll(capture(replaced)) } just Runs

        val payload = """[
            {
                "songId": "1",
                "song": {
                    "title": "One",
                    "artist": "Artist",
                    "album": "Album",
                    "duration": 200000
                },
                "listens": [
                    [1000, 1200, 200],
                    [2000, 2300, 300, 200000, 0]
                ]
            }
        ]"""

        handler(targetDao).restore(payload)

        assertTrue(replaced.isCaptured)
        val restored = replaced.captured
        assertEquals(1, restored.size)
        val listen = restored[0]
        assertEquals(0L, listen.id)
        assertEquals("11", listen.songId)
        assertEquals(2000L, listen.startedAt)
        assertEquals(2300L, listen.endedAt)
        assertEquals(300L, listen.listenedMs)
        assertEquals(200_000L, listen.trackDurationMs)
        assertEquals(0, listen.source)
    }
}
