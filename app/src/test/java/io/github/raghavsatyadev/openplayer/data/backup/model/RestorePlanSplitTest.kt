package io.github.raghavsatyadev.openplayer.data.backup.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RestorePlanSplitTest {

    private fun plan(selected: Set<BackupSection>) = RestorePlan(
        manifest = BackupManifest(),
        backupUri = "content://backup",
        availableModules = BackupSection.entries.toSet(),
        selectedModules = selected,
        moduleDetails = emptyMap()
    )

    @Test
    fun `settings go before the scan and song-linked sections after`() {
        val (before, after) = plan(BackupSection.entries.toSet()).splitAroundLibraryScan()
        assertEquals(LIBRARY_INDEPENDENT_SECTIONS, before!!.selectedModules)
        val afterModules = after!!.selectedModules
        assertTrue(BackupSection.LISTEN_HISTORY in afterModules)
        assertTrue(BackupSection.PLAYLISTS in afterModules)
        assertTrue(BackupSection.FAVORITES in afterModules)
        assertTrue(BackupSection.GLOBAL_SETTINGS !in afterModules)
        assertEquals(BackupSection.entries.toSet(), before.selectedModules + afterModules)
    }

    @Test
    fun `empty halves are null`() {
        val (before, after) = plan(setOf(BackupSection.GLOBAL_SETTINGS)).splitAroundLibraryScan()
        assertEquals(setOf(BackupSection.GLOBAL_SETTINGS), before!!.selectedModules)
        assertNull(after)
        val (before2, after2) = plan(setOf(BackupSection.LISTEN_HISTORY)).splitAroundLibraryScan()
        assertNull(before2)
        assertEquals(setOf(BackupSection.LISTEN_HISTORY), after2!!.selectedModules)
    }

    @Test
    fun `two successes merge to success`() {
        val result = mergeRestoreResults(
            RestoreResult.Success, setOf(BackupSection.GLOBAL_SETTINGS),
            RestoreResult.Success, setOf(BackupSection.LISTEN_HISTORY)
        )
        assertEquals(RestoreResult.Success, result)
    }

    @Test
    fun `one failed half gives a partial result naming its sections`() {
        val result = mergeRestoreResults(
            RestoreResult.Success, setOf(BackupSection.GLOBAL_SETTINGS),
            RestoreResult.TotalFailure("boom"), setOf(BackupSection.LISTEN_HISTORY)
        ) as RestoreResult.PartialFailure
        assertEquals(setOf(BackupSection.GLOBAL_SETTINGS), result.succeeded)
        assertEquals(mapOf(BackupSection.LISTEN_HISTORY to "boom"), result.failed)
    }

    @Test
    fun `a missing half is ignored`() {
        val failure = RestoreResult.TotalFailure("bad file")
        val result = mergeRestoreResults(null, emptySet(), failure, setOf(BackupSection.PLAYLISTS))
        assertEquals(RestoreResult.TotalFailure("bad file"), result)
    }
}
