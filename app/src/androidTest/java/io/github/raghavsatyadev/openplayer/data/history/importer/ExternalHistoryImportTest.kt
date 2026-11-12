package io.github.raghavsatyadev.openplayer.data.history.importer

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.database.AlbumEntity
import io.github.raghavsatyadev.openplayer.data.database.ArtistEntity
import io.github.raghavsatyadev.openplayer.data.database.ListenHistoryDao
import io.github.raghavsatyadev.openplayer.data.database.ListenHistoryEntity
import io.github.raghavsatyadev.openplayer.data.database.ListenSource
import io.github.raghavsatyadev.openplayer.data.database.OpenPlayerDatabase
import io.github.raghavsatyadev.openplayer.data.database.SongEntity
import io.github.raghavsatyadev.openplayer.data.database.SongSummary
import io.github.raghavsatyadev.openplayer.data.history.ListenCountRule
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream

/** Plans and stores imports on an in-memory Room database, including the dedupe rules. */
@RunWith(AndroidJUnit4::class)
class ExternalHistoryImportTest {

    private lateinit var db: OpenPlayerDatabase
    private lateinit var dao: ListenHistoryDao
    private lateinit var commit: HistoryImportCommit

    private val duration = 200_000L
    private val start = 1_612_000_000_000L // 2021-01-30T09:46:40Z
    private val rule = ListenCountRule(minSeconds = 40, minPercentage = 40)

    private val matcher = HistorySongMatcher(
        listOf(
            SongSummary(1, "Song One", "Artist", "Album", duration),
            SongSummary(2, "Song Two", "Artist", "Album", duration)
        )
    )

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, OpenPlayerDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .addCallback(OpenPlayerDatabase.createRuntimeArtifactsCallback())
            .allowMainThreadQueries()
            .build()
        dao = db.listenHistoryDao()
        commit = HistoryImportCommit(db, dao)
        db.musicDao().insertMusicData(
            songs = listOf(songEntity(1, "Song One"), songEntity(2, "Song Two")),
            albums = listOf(AlbumEntity(id = 201, title = "Album", artistName = "Artist", artistId = 101, albumArtUriString = null, songCount = 2, dateAdded = 0L, year = 2021)),
            artists = listOf(ArtistEntity(id = 101, name = "Artist", trackCount = 2, imageUrl = null))
        )
    }

    @After
    fun tearDown() = db.close()

    private fun songEntity(id: Long, title: String) = SongEntity(
        id = id, title = title, artistName = "Artist", artistId = 101, albumName = "Album", albumId = 201,
        contentUriString = "uri_$id", albumArtUriString = null, duration = duration, genre = null,
        filePath = "/music/$id.mp3", parentDirectoryPath = "/music"
    )

    private fun lastFm(vararg rows: Pair<String, Long>): List<ImportedListen> =
        rows.map { (title, ts) -> ImportedListen(title, "Artist", "Album", timestampMs = ts) }

    private suspend fun import(listens: List<ImportedListen>, source: HistorySource) =
        commit.commit(listens, source, matcher, rule)

    @Test
    fun importingTheSameFileTwiceAddsNothingTheSecondTime() = runTest {
        val csv = "Artist,Album,Song One,30 Jan 2021 09:46\nArtist,Album,Song Two,30 Jan 2021 10:00\n"
        val listens = LastFmCsvParser.parseAll(ByteArrayInputStream(csv.toByteArray())).listens

        val first = import(listens, HistorySource.LASTFM)
        assertThat(first.imported).isEqualTo(2)
        assertThat(dao.count()).isEqualTo(2)

        val second = import(listens, HistorySource.LASTFM)
        assertThat(second.imported).isEqualTo(0)
        assertThat(second.duplicates).isEqualTo(2)
        assertThat(dao.count()).isEqualTo(2)

        val row = dao.listensBetween(0, Long.MAX_VALUE).first { it.songId == "1" }
        assertThat(row.source).isEqualTo(ListenSource.IMPORTED_LASTFM)
        assertThat(row.endedAt - row.startedAt).isEqualTo(duration)
        assertThat(row.listenedMs).isEqualTo(duration)
    }

    @Test
    fun lastFmAndListenBrainzOverlapCountOnce() = runTest {
        import(lastFm("Song One" to start), HistorySource.LASTFM)
        // ListenBrainz saw the same play a few seconds apart, plus a genuinely new one.
        val summary = import(
            lastFm("Song One" to start + 8_000L, "Song One" to start + 3_600_000L),
            HistorySource.LISTENBRAINZ
        )
        assertThat(summary.duplicates).isEqualTo(1)
        assertThat(summary.imported).isEqualTo(1)
        assertThat(dao.countForSong("1")).isEqualTo(2)
    }

    @Test
    fun duplicatesInsideOneImportCountOnce() = runTest {
        val summary = import(lastFm("Song One" to start, "Song One" to start + 5_000L), HistorySource.LASTFM)
        assertThat(summary.imported).isEqualTo(1)
        assertThat(summary.duplicates).isEqualTo(1)
    }

    @Test
    fun existingLiveListenAtNearlyTheSameStartIsADuplicate() = runTest {
        dao.insert(
            ListenHistoryEntity(
                songId = "1", startedAt = start - 12_000L, endedAt = start - 12_000L + duration,
                listenedMs = duration, trackDurationMs = duration, source = ListenSource.LIVE
            )
        )
        val summary = import(lastFm("Song One" to start), HistorySource.LISTENBRAINZ)
        assertThat(summary.duplicates).isEqualTo(1)
        assertThat(dao.count()).isEqualTo(1)
    }

    @Test
    fun estimatedRowsDoNotBlockAnImport() = runTest {
        dao.insert(
            ListenHistoryEntity(
                songId = "1", startedAt = start, endedAt = start, listenedMs = duration,
                trackDurationMs = duration, source = ListenSource.ESTIMATED_FROM_PLAY_COUNT
            )
        )
        val summary = import(lastFm("Song One" to start), HistorySource.LASTFM)
        assertThat(summary.imported).isEqualTo(1)
        assertThat(dao.count()).isEqualTo(2)
    }

    @Test
    fun spotifyUsesEndTimeAndTheListenRule() = runTest {
        val end = start + 600_000L
        val listens = listOf(
            ImportedListen("Song One", "Artist", "Album", "Artist", end, timestampIsEnd = true, msPlayed = 150_000L),
            // 10 s of a 200 s track is under both 40 s and 40 %.
            ImportedListen("Song Two", "Artist", "Album", "Artist", end, timestampIsEnd = true, msPlayed = 10_000L)
        )
        val summary = import(listens, HistorySource.SPOTIFY)
        assertThat(summary.imported).isEqualTo(1)
        assertThat(summary.belowRule).isEqualTo(1)
        val row = dao.listensBetween(0, Long.MAX_VALUE).single()
        assertThat(row.songId).isEqualTo("1")
        assertThat(row.endedAt).isEqualTo(end)
        assertThat(row.startedAt).isEqualTo(end - 150_000L)
        assertThat(row.listenedMs).isEqualTo(150_000L)
        assertThat(row.source).isEqualTo(ListenSource.IMPORTED_SPOTIFY)
    }

    @Test
    fun unmatchedPlaysAreCountedAndSampledByFrequency() = runTest {
        val listens = lastFm(
            "Missing A" to start, "Missing A" to start + 400_000L, "Missing B" to start + 800_000L,
            "Song One" to start + 1_200_000L
        )
        val summary = import(listens, HistorySource.LASTFM)
        assertThat(summary.imported).isEqualTo(1)
        assertThat(summary.unmatched).isEqualTo(3)
        assertThat(summary.unmatchedSample.first()).isEqualTo(UnmatchedTrack("Artist - Missing A", 2))
    }
}
