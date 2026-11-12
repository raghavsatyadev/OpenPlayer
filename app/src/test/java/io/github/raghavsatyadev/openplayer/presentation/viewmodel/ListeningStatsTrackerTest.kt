package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import android.os.SystemClock
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.DailyMixManager
import io.github.raghavsatyadev.openplayer.data.history.CountedListen
import io.github.raghavsatyadev.openplayer.data.history.ListenCountRule
import io.github.raghavsatyadev.openplayer.data.history.ListenHistoryRepository
import io.github.raghavsatyadev.openplayer.data.listenbrainz.ScrobbleManager
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.stats.PlaybackStatsRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ListeningStatsTrackerTest {

    private val dailyMixManager: DailyMixManager = mockk(relaxed = true)
    private val playbackStatsRepository: PlaybackStatsRepository = mockk(relaxed = true)
    private val scrobbleManager: ScrobbleManager = mockk(relaxed = true)
    private val listenHistoryRepository: ListenHistoryRepository = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkStatic(SystemClock::class)
        every { listenHistoryRepository.rule } returns ListenCountRule(minSeconds = 40, minPercentage = 40)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `finalizeCurrentSession preserves listening longer than track duration`() {
        val tracker = ListeningStatsTracker(
            dailyMixManager = dailyMixManager,
            playbackStatsRepository = playbackStatsRepository,
            scrobbleManager = scrobbleManager,
            listenHistoryRepository = listenHistoryRepository
        )
        val song = song(
            songId = "looped-song",
            durationMs = TimeUnit.MINUTES.toMillis(3)
        )
        val listenedMs = TimeUnit.MINUTES.toMillis(12)

        every { SystemClock.elapsedRealtime() } returnsMany listOf(
            1_000L,
            1_000L + listenedMs
        )

        tracker.onSongChanged(
            song = song,
            positionMs = 0L,
            durationMs = song.duration,
            isPlaying = true
        )
        tracker.finalizeCurrentSession(forceSynchronousPersistence = true)

        coVerify(timeout = 2_000) {
            dailyMixManager.recordPlay(song.id, listenedMs, any())
        }
        coVerify(timeout = 2_000) {
            playbackStatsRepository.recordPlayback(song.id, listenedMs, any())
        }
    }

    @Test
    fun `onProgress accumulates incremental listening time`() {
        val tracker = ListeningStatsTracker(
            dailyMixManager = dailyMixManager,
            playbackStatsRepository = playbackStatsRepository,
            scrobbleManager = scrobbleManager,
            listenHistoryRepository = listenHistoryRepository
        )
        val song = song(songId = "song-1")
        val firstChunkMs = 7_000L
        val secondChunkMs = 8_000L
        val expectedDurationMs = firstChunkMs + secondChunkMs

        every { SystemClock.elapsedRealtime() } returnsMany listOf(
            5_000L,
            5_000L + firstChunkMs,
            5_000L + firstChunkMs + secondChunkMs
        )

        tracker.onSongChanged(
            song = song,
            positionMs = 0L,
            durationMs = song.duration,
            isPlaying = true
        )
        tracker.onProgress(positionMs = firstChunkMs, isPlaying = true)
        tracker.finalizeCurrentSession(forceSynchronousPersistence = true)

        coVerify(timeout = 2_000) {
            playbackStatsRepository.recordPlayback(song.id, expectedDurationMs, any())
        }
        assertThat(expectedDurationMs).isGreaterThan(TimeUnit.SECONDS.toMillis(5))
    }

    @Test
    fun `a play past the listen rule is saved to listen history`() {
        val tracker = tracker()
        val song = song(songId = "counted", durationMs = TimeUnit.MINUTES.toMillis(3))
        every { SystemClock.elapsedRealtime() } returnsMany listOf(0L, 45_000L)

        tracker.onSongChanged(song = song, positionMs = 0L, durationMs = song.duration, isPlaying = true)
        tracker.finalizeCurrentSession(forceSynchronousPersistence = true)

        coVerify(timeout = 2_000) {
            listenHistoryRepository.record(match<CountedListen> { it.songId == "counted" && it.listenedMs == 45_000L })
        }
    }

    @Test
    fun `a skipped play is listening time but not a listen`() {
        val tracker = tracker()
        val song = song(songId = "skipped", durationMs = TimeUnit.MINUTES.toMillis(3))
        every { SystemClock.elapsedRealtime() } returnsMany listOf(0L, 20_000L)

        tracker.onSongChanged(song = song, positionMs = 0L, durationMs = song.duration, isPlaying = true)
        tracker.finalizeCurrentSession(forceSynchronousPersistence = true)

        coVerify(timeout = 2_000) { playbackStatsRepository.recordPlayback("skipped", 20_000L, any()) }
        coVerify(exactly = 0) { listenHistoryRepository.record(any()) }
    }

    @Test
    fun `position wrapping back to the start records a new listen`() {
        val tracker = tracker()
        val duration = TimeUnit.MINUTES.toMillis(3)
        val song = song(songId = "looped", durationMs = duration)
        every { SystemClock.elapsedRealtime() } returnsMany listOf(0L, 170_000L, duration, duration + 60_000L)

        tracker.onSongChanged(song = song, positionMs = 0L, durationMs = duration, isPlaying = true)
        tracker.onProgress(positionMs = 170_000L, isPlaying = true)
        tracker.onProgress(positionMs = 0L, isPlaying = true)
        tracker.finalizeCurrentSession(forceSynchronousPersistence = true)

        coVerify(timeout = 2_000, exactly = 2) { listenHistoryRepository.record(any()) }
    }

    @Test
    fun `seeking back to the start after a listen begins a new one`() {
        val tracker = tracker()
        val song = song(songId = "replayed", durationMs = TimeUnit.MINUTES.toMillis(3))
        every { SystemClock.elapsedRealtime() } returnsMany listOf(0L, 60_000L, 110_000L)

        tracker.onSongChanged(song = song, positionMs = 0L, durationMs = song.duration, isPlaying = true)
        tracker.onSeek(fromPositionMs = 60_000L, toPositionMs = 0L, isPlaying = true)
        tracker.finalizeCurrentSession(forceSynchronousPersistence = true)

        coVerify(timeout = 2_000, exactly = 2) { listenHistoryRepository.record(any()) }
    }

    private fun tracker() = ListeningStatsTracker(
        dailyMixManager = dailyMixManager,
        playbackStatsRepository = playbackStatsRepository,
        scrobbleManager = scrobbleManager,
        listenHistoryRepository = listenHistoryRepository
    )

    private fun song(songId: String, durationMs: Long = 5 * 60 * 1000L): Song = Song(
        id = songId,
        title = "Song $songId",
        artist = "Artist",
        artistId = 1L,
        album = "Album",
        albumId = 1L,
        path = "/music/$songId.mp3",
        contentUriString = "content://media/external/audio/media/$songId",
        albumArtUriString = null,
        duration = durationMs,
        mimeType = "audio/mpeg",
        bitrate = 320_000,
        sampleRate = 44_100
    )
}
