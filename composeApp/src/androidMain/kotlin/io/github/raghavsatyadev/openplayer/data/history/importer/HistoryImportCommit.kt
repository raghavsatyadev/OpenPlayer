package io.github.raghavsatyadev.openplayer.data.history.importer

import androidx.room3.withWriteTransaction
import io.github.raghavsatyadev.openplayer.data.database.ListenHistoryDao
import io.github.raghavsatyadev.openplayer.data.database.OpenPlayerDatabase
import io.github.raghavsatyadev.openplayer.data.history.ListenCountRule
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * The database half of an import: loads the listens already stored around the import's span once,
 * plans the new rows with [ExternalHistoryPlanner] and inserts them in chunked transactions.
 */
class HistoryImportCommit(
    private val database: OpenPlayerDatabase,
    private val dao: ListenHistoryDao
) {
    suspend fun commit(
        listens: List<ImportedListen>,
        source: HistorySource,
        matcher: SongMatcher,
        rule: ListenCountRule,
        unparseable: Int = 0,
        failedFiles: Int = 0,
        onPhase: (HistoryImportState.Phase) -> Unit = {}
    ): HistoryImportSummary {
        onPhase(HistoryImportState.Phase.MATCHING)
        val durations = dao.getSongDurations().associate { it.id.toString() to it.duration }
        val plan = ExternalHistoryPlanner(matcher, durations, rule, loadExisting(listens))
            .plan(listens, source, unparseable, failedFiles)

        onPhase(HistoryImportState.Phase.SAVING)
        plan.rows.chunked(INSERT_CHUNK).forEach { chunk ->
            currentCoroutineContext().ensureActive()
            database.withWriteTransaction { dao.insertAll(chunk) }
        }
        return plan.summary
    }

    /** Start times already stored around the import's span, per song; one query. */
    private suspend fun loadExisting(listens: List<ImportedListen>): Map<String, List<Long>> {
        if (listens.isEmpty()) return emptyMap()
        var min = Long.MAX_VALUE
        var max = Long.MIN_VALUE
        for (l in listens) {
            val low = l.timestampMs - (l.msPlayed ?: 0L)
            if (low < min) min = low
            if (l.timestampMs > max) max = l.timestampMs
        }
        val pad = ExternalHistoryPlanner.SPAN_PADDING_MS
        return dao.startsBetween((min - pad).coerceAtLeast(0L), max + pad)
            .groupBy({ it.songId }, { it.startedAt })
    }

    private companion object {
        const val INSERT_CHUNK = 1000
    }
}
