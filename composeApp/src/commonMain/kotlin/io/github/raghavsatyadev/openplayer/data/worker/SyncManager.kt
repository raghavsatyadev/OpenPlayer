package io.github.raghavsatyadev.openplayer.data.worker

import kotlinx.coroutines.flow.Flow

/**
 * Data class representing the progress of the sync operation.
 */
data class SyncProgress(
    val isRunning: Boolean = false,
    val currentCount: Int = 0,
    val totalCount: Int = 0,
    val isCompleted: Boolean = false,
    val phase: SyncPhase = SyncPhase.IDLE
) {
    enum class SyncPhase {
        IDLE,
        FETCHING_MEDIASTORE,
        PROCESSING_FILES,
        SAVING_TO_DATABASE,
        SCANNING_LRC,
        CLEANING_CACHE,
        SYNCING_CLOUD,
        COMPLETING
    }

    val progress: Float
        get() = if (totalCount > 0) currentCount.toFloat() / totalCount else 0f

    val hasProgress: Boolean
        get() = totalCount > 0
}

/**
 * Keeps the local library database in step with the files on the device. Android runs the scan
 * through WorkManager; desktop gets its own implementation in Phase 7.
 */
interface SyncManager {
    val isSyncing: Flow<Boolean>

    /** Emits once each time a sync run fails. */
    val syncFailed: Flow<Unit>

    val syncProgress: Flow<SyncProgress>

    /** True while a run is still reading or saving changes (before the maintenance phases). */
    val isFetchingChanges: Flow<Boolean>

    /** Starts periodic and change-triggered syncing. Safe to call more than once. */
    fun start()

    fun sync()

    fun incrementalSync()

    fun fullSync(deepScan: Boolean = true)

    fun rebuildDatabase()

    fun forceRefresh()
}
