package io.github.raghavsatyadev.openplayer.data.history.importer

/** Where an import is, for the sheet. */
sealed interface HistoryImportState {
    data object Idle : HistoryImportState

    data class Working(val source: HistorySource, val phase: Phase, val parsed: Int) : HistoryImportState

    data class Done(val source: HistorySource, val summary: HistoryImportSummary) : HistoryImportState

    data class Failed(val source: HistorySource) : HistoryImportState

    enum class Phase { READING, MATCHING, SAVING }
}
