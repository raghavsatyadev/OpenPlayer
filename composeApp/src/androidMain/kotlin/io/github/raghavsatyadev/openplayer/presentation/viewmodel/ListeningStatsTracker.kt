package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import io.github.raghavsatyadev.openplayer.utils.elapsedRealtimeMs
import androidx.media3.common.C
import io.github.raghavsatyadev.openplayer.data.DailyMixManager
import io.github.raghavsatyadev.openplayer.data.history.CountedListen
import io.github.raghavsatyadev.openplayer.data.history.ListenHistoryRepository
import io.github.raghavsatyadev.openplayer.data.history.ListenPass
import io.github.raghavsatyadev.openplayer.data.listenbrainz.ScrobbleManager
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.stats.PlaybackStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import co.touchlab.kermit.Logger

/**
 * Tracks listening statistics for songs.
 * Extracted from PlayerViewModel to reduce its size and improve modularity.
 *
 * Responsibilities:
 * - Track active listening sessions
 * - Record play statistics when session ends
 * - Handle voluntary vs automatic plays
 * - Record a listen in the history once a play meets the listen rule (see [ListenPass])
 */
class ListeningStatsTracker(
    private val dailyMixManager: DailyMixManager,
    private val playbackStatsRepository: PlaybackStatsRepository,
    private val scrobbleManager: ScrobbleManager,
    private val listenHistoryRepository: ListenHistoryRepository
) {
    private var currentSession: ActiveSession? = null
    private var pendingVoluntarySongId: String? = null
    private var scope: CoroutineScope? = null
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _playbackHistory = MutableStateFlow<List<PlaybackStatsRepository.PlaybackHistoryEntry>>(emptyList())
    val playbackHistory: StateFlow<List<PlaybackStatsRepository.PlaybackHistoryEntry>> = _playbackHistory.asStateFlow()

    /**
     * Must be called to set the coroutine scope for async operations.
     */
    fun initialize(coroutineScope: CoroutineScope) {
        val activeScope = scope
        if (activeScope == null || activeScope.coroutineContext[Job]?.isActive != true) {
            scope = coroutineScope
        }
        coroutineScope.launch(Dispatchers.IO) {
            _playbackHistory.value = playbackStatsRepository.loadPlaybackHistory(
                limit = MAX_INTERNAL_PLAYBACK_HISTORY_ITEMS
            )
        }
    }

    @Synchronized
    fun onVoluntarySelection(songId: String) {
        pendingVoluntarySongId = songId
    }

    fun onSongChanged(
        song: Song?,
        positionMs: Long,
        durationMs: Long,
        isPlaying: Boolean
    ) {
        onTrackChanged(
            songId = song?.id,
            positionMs = positionMs,
            durationMs = durationMs,
            fallbackDurationMs = song?.duration ?: 0L,
            isPlaying = isPlaying
        )
    }

    @Synchronized
    fun onTrackChanged(
        songId: String?,
        positionMs: Long,
        durationMs: Long,
        isPlaying: Boolean
    ) {
        onTrackChanged(
            songId = songId,
            positionMs = positionMs,
            durationMs = durationMs,
            fallbackDurationMs = 0L,
            isPlaying = isPlaying
        )
    }

    @Synchronized
    fun onTrackChanged(
        songId: String?,
        positionMs: Long,
        durationMs: Long,
        fallbackDurationMs: Long,
        isPlaying: Boolean
    ) {
        finalizeCurrentSession()
        val safeSongId = songId?.takeIf { it.isNotBlank() }
        if (safeSongId == null) {
            return
        }

        val nowRealtime = elapsedRealtimeMs()
        val nowEpoch = System.currentTimeMillis()
        val normalizedDuration = normalizeDuration(durationMs, fallbackDurationMs)

        currentSession = ActiveSession(
            songId = safeSongId,
            totalDurationMs = normalizedDuration,
            startedAtEpochMs = nowEpoch,
            lastKnownPositionMs = positionMs.coerceAtLeast(0L),
            accumulatedListeningMs = 0L,
            lastRealtimeMs = nowRealtime,
            lastUpdateEpochMs = nowEpoch,
            isPlaying = isPlaying,
            isVoluntary = pendingVoluntarySongId == safeSongId,
            listenPass = ListenPass(
                songId = safeSongId,
                startedAtEpochMs = nowEpoch,
                startPositionMs = positionMs
            )
        )
        if (pendingVoluntarySongId == safeSongId) {
            pendingVoluntarySongId = null
        }
        if (isPlaying) {
            scrobbleManager.onPlayingNow(safeSongId)
        }
    }

    @Synchronized
    fun onPlayStateChanged(isPlaying: Boolean, positionMs: Long) {
        val session = currentSession ?: return
        val wasPlaying = session.isPlaying
        val nowRealtime = elapsedRealtimeMs()
        val delta = accumulateRealtimeListening(session, nowRealtime)
        session.isPlaying = isPlaying
        session.lastRealtimeMs = nowRealtime
        session.lastKnownPositionMs = positionMs.coerceAtLeast(0L)
        session.lastUpdateEpochMs = System.currentTimeMillis()
        advanceListenPass(session, delta)
        if (!wasPlaying && isPlaying) {
            scrobbleManager.onPlayingNow(session.songId)
        }
    }

    @Synchronized
    fun onProgress(positionMs: Long, isPlaying: Boolean) {
        val session = currentSession ?: return
        val nowRealtime = elapsedRealtimeMs()
        val delta = accumulateRealtimeListening(session, nowRealtime)
        session.isPlaying = isPlaying
        session.lastRealtimeMs = nowRealtime
        session.lastKnownPositionMs = positionMs.coerceAtLeast(0L)
        session.lastUpdateEpochMs = System.currentTimeMillis()
        advanceListenPass(session, delta)
    }

    /** A seek within the current track; seeking back to the start after a listen begins a new one. */
    @Synchronized
    fun onSeek(fromPositionMs: Long, toPositionMs: Long, isPlaying: Boolean) {
        val session = currentSession ?: return
        val nowRealtime = elapsedRealtimeMs()
        val delta = accumulateRealtimeListening(session, nowRealtime)
        session.lastRealtimeMs = nowRealtime
        session.lastUpdateEpochMs = System.currentTimeMillis()
        session.lastKnownPositionMs = fromPositionMs.coerceAtLeast(0L)
        advanceListenPass(session, delta)
        session.lastKnownPositionMs = toPositionMs.coerceAtLeast(0L)
        advanceListenPass(session, 0L)
        session.isPlaying = isPlaying
    }

    fun ensureSession(
        song: Song?,
        positionMs: Long,
        durationMs: Long,
        isPlaying: Boolean
    ) {
        ensureSession(
            songId = song?.id,
            positionMs = positionMs,
            durationMs = durationMs,
            fallbackDurationMs = song?.duration ?: 0L,
            isPlaying = isPlaying
        )
    }

    @Synchronized
    fun ensureSession(
        songId: String?,
        positionMs: Long,
        durationMs: Long,
        isPlaying: Boolean
    ) {
        ensureSession(
            songId = songId,
            positionMs = positionMs,
            durationMs = durationMs,
            fallbackDurationMs = 0L,
            isPlaying = isPlaying
        )
    }

    @Synchronized
    fun ensureSession(
        songId: String?,
        positionMs: Long,
        durationMs: Long,
        fallbackDurationMs: Long,
        isPlaying: Boolean
    ) {
        val safeSongId = songId?.takeIf { it.isNotBlank() }
        if (safeSongId == null) {
            finalizeCurrentSession()
            return
        }
        val existing = currentSession
        if (existing?.songId == safeSongId) {
            val wasPlaying = existing.isPlaying
            updateDuration(normalizeDuration(durationMs, fallbackDurationMs))
            val nowRealtime = elapsedRealtimeMs()
            val delta = accumulateRealtimeListening(existing, nowRealtime)
            existing.isPlaying = isPlaying
            existing.lastRealtimeMs = nowRealtime
            existing.lastKnownPositionMs = positionMs.coerceAtLeast(0L)
            existing.lastUpdateEpochMs = System.currentTimeMillis()
            advanceListenPass(existing, delta)
            if (!wasPlaying && isPlaying) {
                scrobbleManager.onPlayingNow(existing.songId)
            }
            return
        }
        onTrackChanged(
            songId = safeSongId,
            positionMs = positionMs,
            durationMs = durationMs,
            fallbackDurationMs = fallbackDurationMs,
            isPlaying = isPlaying
        )
    }

    @Synchronized
    fun updateDuration(durationMs: Long) {
        val session = currentSession ?: return
        if (durationMs > 0 && durationMs != C.TIME_UNSET) {
            session.totalDurationMs = durationMs
        }
    }

    @Synchronized
    fun finalizeCurrentSession(forceSynchronousPersistence: Boolean = false) {
        val session = currentSession ?: return
        val nowRealtime = elapsedRealtimeMs()
        val nowEpoch = System.currentTimeMillis()
        val delta = accumulateRealtimeListening(session, nowRealtime)
        advanceListenPass(session, delta)
        val passEnd = if (session.isPlaying) nowEpoch else session.lastUpdateEpochMs.coerceAtMost(nowEpoch)
        session.listenPass?.close(passEnd, session.totalDurationMs, listenHistoryRepository.rule)
            ?.let(::persistListen)
        val listened = session.accumulatedListeningMs.coerceAtLeast(0L)
        scrobbleManager.onSessionFinalized(
            songId = session.songId,
            startedAtEpochMs = session.startedAtEpochMs,
            listenedMs = listened,
            trackDurationMs = session.totalDurationMs
        )
        if (listened >= MIN_SESSION_LISTEN_MS) {
            val rawEndTimestamp = when {
                session.isPlaying -> nowEpoch
                session.lastUpdateEpochMs > 0L -> session.lastUpdateEpochMs
                else -> session.startedAtEpochMs + listened
            }
            val timestamp = rawEndTimestamp
                .coerceAtLeast(session.startedAtEpochMs.coerceAtLeast(0L))
                .coerceAtMost(nowEpoch)
            val songId = session.songId
            val historyEntry = PlaybackStatsRepository.PlaybackHistoryEntry(
                songId = songId,
                timestamp = timestamp
            )
            _playbackHistory.update { current ->
                (listOf(historyEntry) + current).take(MAX_INTERNAL_PLAYBACK_HISTORY_ITEMS)
            }
            persistPlayback(
                songId = songId,
                listened = listened,
                timestamp = timestamp,
                forceSynchronous = forceSynchronousPersistence
            )
        }
        currentSession = null
        if (pendingVoluntarySongId == session.songId) {
            pendingVoluntarySongId = null
        }
    }

    @Synchronized
    fun onPlaybackStopped() {
        finalizeCurrentSession()
    }

    @Synchronized
    fun onCleared() {
        finalizeCurrentSession(forceSynchronousPersistence = true)
        scope = null
    }

    @Suppress("UNUSED_PARAMETER")
    private fun persistPlayback(
        songId: String,
        listened: Long,
        timestamp: Long,
        forceSynchronous: Boolean
    ) {
        persistenceScope.launch {
            runCatching {
                persistPlaybackInternal(songId = songId, listened = listened, timestamp = timestamp)
            }.onFailure { throwable ->
                Logger.e(throwable) { String.format("Failed to persist listening session for song=%s", songId) }
            }
        }
    }

    private suspend fun persistPlaybackInternal(songId: String, listened: Long, timestamp: Long) {
        dailyMixManager.recordPlay(
            songId = songId,
            songDurationMs = listened,
            timestamp = timestamp
        )
        playbackStatsRepository.recordPlayback(
            songId = songId,
            durationMs = listened,
            timestamp = timestamp
        )
    }

    /** Adds the time played since the last update and returns it. */
    private fun accumulateRealtimeListening(session: ActiveSession, nowRealtime: Long): Long {
        if (!session.isPlaying) return 0L
        val delta = (nowRealtime - session.lastRealtimeMs).coerceAtLeast(0L)
        if (delta > 0L) {
            session.accumulatedListeningMs += delta
        }
        return delta
    }

    /** Feeds the listen pass; a repeat or a seek back to the start closes a counted listen. */
    private fun advanceListenPass(session: ActiveSession, deltaMs: Long) {
        val pass = session.listenPass ?: return
        pass.advance(
            deltaMs = deltaMs,
            positionMs = session.lastKnownPositionMs,
            durationMs = session.totalDurationMs,
            nowEpochMs = session.lastUpdateEpochMs,
            rule = listenHistoryRepository.rule
        )?.let(::persistListen)
    }

    private fun persistListen(listen: CountedListen) {
        persistenceScope.launch {
            runCatching { listenHistoryRepository.record(listen) }
                .onFailure { throwable ->
                    Logger.e(throwable) { String.format("Failed to record listen for song=%s", listen.songId) }
                }
        }
    }

    private fun normalizeDuration(durationMs: Long, fallbackDurationMs: Long): Long {
        return when {
            durationMs > 0 && durationMs != C.TIME_UNSET -> durationMs
            fallbackDurationMs > 0 && fallbackDurationMs != C.TIME_UNSET -> fallbackDurationMs
            else -> 0L
        }
    }

    companion object {
        private val MIN_SESSION_LISTEN_MS = TimeUnit.SECONDS.toMillis(5)
        private const val MAX_INTERNAL_PLAYBACK_HISTORY_ITEMS = 500
    }
}

/**
 * Represents an active listening session for a song.
 */
data class ActiveSession(
    val songId: String,
    var totalDurationMs: Long,
    val startedAtEpochMs: Long,
    var lastKnownPositionMs: Long,
    var accumulatedListeningMs: Long,
    var lastRealtimeMs: Long,
    var lastUpdateEpochMs: Long,
    var isPlaying: Boolean,
    val isVoluntary: Boolean,
    val listenPass: ListenPass? = null
)
