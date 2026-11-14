package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import io.github.raghavsatyadev.openplayer.presentation.viewmodel.MetadataEditStateHolder
import io.github.raghavsatyadev.openplayer.resources.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** MediaStore write-permission results: retries the parked edit, lyrics save or genre edit, and restores interrupted saves. */
class PlayerWritePermissions(
    private val scope: CoroutineScope,
    private val context: android.content.Context,
    private val metadataEditStateHolder: MetadataEditStateHolder,
    private val writeRequests: PlayerWriteRequests,
    private val metadataEditor: PlayerMetadataEditor,
    private val genreEditor: PlayerGenreEditor,
    private val lyricsSaver: PlayerLyricsSaver,
    private val toast: suspend (String) -> Unit,
) {
    val writePermissionRequest: SharedFlow<android.content.IntentSender> = writeRequests.writePermissionRequest

    /** Called from the UI after the user approves or denies the MediaStore write permission. */
    fun onWritePermissionResult(granted: Boolean) {
        try {
            dispatchWritePermissionResult(granted)
        } finally {
            // Lets a restore prompt that waited for this request ask next.
            writeRequests.answeredWriteRequests.update { it + 1 }
        }
    }

    private fun dispatchWritePermissionResult(granted: Boolean) {
        val batchMetadata = writeRequests.pendingBatchMetadataEdit
        if (batchMetadata != null) {
            writeRequests.pendingBatchMetadataEdit = null
            if (!granted) {
                scope.launch {
                    toast(getString(Res.string.player_permission_denied_edit_files))
                }
                return
            }
            scope.launch {
                metadataEditor.performBatchMetadataEdit(
                    batchMetadata.songs,
                    batchMetadata.title,
                    batchMetadata.artist,
                    batchMetadata.album,
                    batchMetadata.albumArtist,
                    batchMetadata.composer,
                    batchMetadata.genre,
                    batchMetadata.lyrics,
                    batchMetadata.trackNumber,
                    batchMetadata.discNumber,
                    batchMetadata.replayGainTrackGainDb,
                    batchMetadata.replayGainAlbumGainDb,
                    batchMetadata.coverArtUpdate
                )
            }
            return
        }

        val batchGenre = writeRequests.pendingBatchGenreEdit
        if (batchGenre != null) {
            writeRequests.pendingBatchGenreEdit = null
            if (!granted) {
                scope.launch {
                    toast(getString(Res.string.player_permission_denied_edit_files))
                }
                return
            }
            scope.launch { genreEditor.performBatchEditGenre(batchGenre.first, batchGenre.second) }
            return
        }

        val pendingLyrics = writeRequests.pendingLyricsSave
        if (pendingLyrics != null) {
            writeRequests.pendingLyricsSave = null
            if (!granted) {
                scope.launch {
                    toast(getString(Res.string.player_permission_denied_save_lyrics))
                }
                return
            }
            lyricsSaver.performLyricsSave(pendingLyrics.song, pendingLyrics.lyrics, pendingLyrics.preferSynced)
            return
        }

        val pending = writeRequests.pendingMetadataEdit
        if (pending == null) {
            if (writeRequests.pendingInterruptedSaveRestore) onInterruptedSaveRestorePermissionResult(granted)
            return
        }
        writeRequests.pendingMetadataEdit = null
        if (!granted) {
            scope.launch {
                toast(getString(Res.string.player_permission_denied_edit_this_file))
            }
            return
        }
        scope.launch {
            metadataEditor.performMetadataEdit(
                pending.song, pending.title, pending.artist, pending.album,
                pending.albumArtist, pending.composer, pending.genre, pending.lyrics,
                pending.trackNumber, pending.discNumber,
                pending.replayGainTrackGainDb, pending.replayGainAlbumGainDb, pending.coverArtUpdate,
                pending.customMetadataChanges
            )
        }
    }

    /**
     * Asks the user for write access to songs that an interrupted save left cut off, when the
     * app can no longer write to them (its write access for the edit ended with the process),
     * and restores them once it is granted. Each song is asked about once per ViewModel.
     */
    suspend fun offerToRestoreInterruptedSaves() {
        metadataEditStateHolder.songsAwaitingWriteAccess.collectLatest { uris ->
            if (uris.all { it.toString() in writeRequests.askedInterruptedSaveRestoreUris }) return@collectLatest
            // A request emitted while the player sheet is not collecting would be dropped.
            writeRequests.subscriptionCount.first { it > 0 }
            // Only one write request can be in flight; wait for an edit's or lyrics save's own
            // request to be answered instead of dropping the restore until the next change.
            while (true) {
                val answered = writeRequests.answeredWriteRequests.value
                if (!writeRequests.otherWriteRequestPending()) break
                writeRequests.answeredWriteRequests.first { it != answered }
            }
            val intentSender = io.github.raghavsatyadev.openplayer.utils.MediaStorePermissionHelper
                .createWriteRequestIntentSender(context, uris) ?: return@collectLatest
            uris.mapTo(writeRequests.askedInterruptedSaveRestoreUris) { it.toString() }
            writeRequests.pendingInterruptedSaveRestore = true
            toast(getString(Res.string.metadata_restore_permission_prompt))
            writeRequests.emit(intentSender)
        }
    }

    private fun onInterruptedSaveRestorePermissionResult(granted: Boolean) {
        writeRequests.pendingInterruptedSaveRestore = false
        scope.launch {
            val message = if (!granted) {
                Res.string.metadata_restore_permission_denied
            } else if (metadataEditStateHolder.resumeInterruptedWriteBacks().stillPending == 0) {
                Res.string.metadata_restore_done
            } else {
                Res.string.metadata_restore_incomplete
            }
            toast(getString(message))
        }
    }
}
