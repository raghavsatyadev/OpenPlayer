package io.github.raghavsatyadev.openplayer.data.history.importer

/** One track that couldn't be matched to the library, with how many plays it had. */
data class UnmatchedTrack(val label: String, val plays: Int)

/** What an import did. All counts are plays. */
data class HistoryImportSummary(
    val imported: Int = 0,
    val duplicates: Int = 0,
    val unmatched: Int = 0,
    /** Spotify plays too short to count under the listen rule. */
    val belowRule: Int = 0,
    val unparseable: Int = 0,
    /** The most played unmatched tracks, "artist - title". */
    val unmatchedSample: List<UnmatchedTrack> = emptyList(),
    /** Files that could not be read at all. */
    val failedFiles: Int = 0
) {
    val total: Int get() = imported + duplicates + unmatched + belowRule + unparseable
}
