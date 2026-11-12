package io.github.raghavsatyadev.openplayer.data.history.importer

import android.content.Context
import android.net.Uri
import io.github.raghavsatyadev.openplayer.data.database.ListenHistoryDao
import io.github.raghavsatyadev.openplayer.data.database.MusicDao
import io.github.raghavsatyadev.openplayer.data.database.OpenPlayerDatabase
import io.github.raghavsatyadev.openplayer.data.history.ListenHistoryRepository
import io.github.raghavsatyadev.openplayer.data.preferences.ListenHistoryPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import co.touchlab.kermit.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports listening history from Last.fm, Spotify, ListenBrainz or YouTube Music exports the user
 * picked with the file picker. Reads and plans off the main thread, writes in chunked transactions,
 * and publishes [state] for the import sheet.
 */
@Singleton
class ExternalHistoryImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: OpenPlayerDatabase,
    private val listenHistoryDao: ListenHistoryDao,
    private val musicDao: MusicDao,
    private val preferences: ListenHistoryPreferences,
    private val listenHistoryRepository: ListenHistoryRepository
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<HistoryImportState>(HistoryImportState.Idle)
    val state: StateFlow<HistoryImportState> = _state.asStateFlow()

    /** Back to the start screen, unless an import is running. */
    fun reset() {
        if (_state.value !is HistoryImportState.Working) _state.value = HistoryImportState.Idle
    }

    suspend fun import(source: HistorySource, uris: List<Uri>): HistoryImportSummary? {
        if (uris.isEmpty() || !mutex.tryLock()) return null
        try {
            _state.value = HistoryImportState.Working(source, HistoryImportState.Phase.READING, 0)
            val summary = withContext(Dispatchers.IO) { run(source, uris) }
            _state.value = HistoryImportState.Done(source, summary)
            return summary
        } catch (e: kotlinx.coroutines.CancellationException) {
            _state.value = HistoryImportState.Idle
            throw e
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "History import failed" }
            _state.value = HistoryImportState.Failed(source)
            return null
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun run(source: HistorySource, uris: List<Uri>): HistoryImportSummary {
        // Let the one-time seed finish first so the dedupe sees its rows.
        listenHistoryRepository.importIfNeeded()

        val coroutine = currentCoroutineContext()
        val listens = ArrayList<ImportedListen>()
        var unparseable = 0
        var failedFiles = 0
        for (uri in uris) {
            coroutine.ensureActive()
            try {
                val stream = context.contentResolver.openInputStream(uri) ?: throw java.io.FileNotFoundException(uri.toString())
                stream.buffered(BUFFER).use { input ->
                    unparseable += parserFor(source).parse(input) { listen ->
                        listens += listen
                        if (listens.size % PROGRESS_STEP == 0) {
                            _state.value = HistoryImportState.Working(source, HistoryImportState.Phase.READING, listens.size)
                            coroutine.ensureActive()
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.withTag(TAG).w(e) { String.format("Could not read %s", uri) }
                failedFiles++
            }
        }
        if (listens.isEmpty() && failedFiles == uris.size) throw java.io.IOException("No file could be read")

        val matcher = HistorySongMatcher(musicDao.getAllLocalSongSummaries())
        val summary = HistoryImportCommit(database, listenHistoryDao).commit(
            listens, source, matcher, preferences.currentRule(), unparseable, failedFiles
        ) { phase -> _state.value = HistoryImportState.Working(source, phase, listens.size) }
        Logger.withTag(TAG).i { String.format("Imported %d of %d plays from %s", summary.imported, listens.size, source) }
        return summary
    }

    private fun parserFor(source: HistorySource): HistoryParser = when (source) {
        HistorySource.LASTFM -> LastFmCsvParser
        HistorySource.SPOTIFY -> SpotifyHistoryParser
        HistorySource.LISTENBRAINZ -> ListenBrainzParser
        HistorySource.YOUTUBE_MUSIC -> YouTubeMusicHistoryParser
    }

    private companion object {
        const val TAG = "HistoryImport"
        const val BUFFER = 64 * 1024
        const val PROGRESS_STEP = 2000
    }
}
