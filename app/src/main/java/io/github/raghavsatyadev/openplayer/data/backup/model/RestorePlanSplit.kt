package io.github.raghavsatyadev.openplayer.data.backup.model

/**
 * Sections whose data does not point at songs, so they can be restored before the library exists.
 * Everything else (history, playlists, favorites, ratings...) is matched against the library on
 * restore and is dropped when the library is still empty.
 */
val LIBRARY_INDEPENDENT_SECTIONS: Set<BackupSection> = setOf(
    BackupSection.GLOBAL_SETTINGS,
    BackupSection.SEARCH_HISTORY,
    BackupSection.QUICK_FILL,
    BackupSection.ARTIST_IMAGES,
    BackupSection.EQUALIZER
)

/**
 * Splits a plan into the part to restore before the library scan (settings, including folder
 * rules the scan should follow) and the part to restore after it. Either part is null when empty.
 */
fun RestorePlan.splitAroundLibraryScan(): Pair<RestorePlan?, RestorePlan?> {
    val before = selectedModules intersect LIBRARY_INDEPENDENT_SECTIONS
    val after = selectedModules - LIBRARY_INDEPENDENT_SECTIONS
    return (if (before.isEmpty()) null else copy(selectedModules = before)) to
        (if (after.isEmpty()) null else copy(selectedModules = after))
}

/** Combines the results of two restores of one backup into the result the user sees. */
fun mergeRestoreResults(
    first: RestoreResult?,
    firstModules: Set<BackupSection>,
    second: RestoreResult?,
    secondModules: Set<BackupSection>
): RestoreResult {
    val results = listOfNotNull(first?.let { it to firstModules }, second?.let { it to secondModules })
    if (results.isEmpty()) return RestoreResult.Success
    if (results.all { it.first is RestoreResult.Success }) return RestoreResult.Success
    if (results.all { it.first is RestoreResult.TotalFailure }) {
        return RestoreResult.TotalFailure(
            results.joinToString("; ") { (it.first as RestoreResult.TotalFailure).error }
        )
    }
    val succeeded = mutableSetOf<BackupSection>()
    val failed = mutableMapOf<BackupSection, String>()
    var rolledBack = false
    for ((result, modules) in results) {
        when (result) {
            is RestoreResult.Success -> succeeded += modules
            is RestoreResult.PartialFailure -> {
                succeeded += result.succeeded
                failed += result.failed
                rolledBack = rolledBack || result.rolledBack
            }
            is RestoreResult.TotalFailure -> modules.forEach { failed[it] = result.error }
        }
    }
    return RestoreResult.PartialFailure(succeeded, failed, rolledBack)
}
