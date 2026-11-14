package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import android.app.Activity
import androidx.annotation.OptIn
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import co.touchlab.kermit.Logger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.di.DispatcherProvider
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.MetadataEditStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.MultiSelectionStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlaybackStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerUiState
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SongRemovalStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.queue.PlayNextInserter
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.removeSongById
import io.github.raghavsatyadev.openplayer.resources.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Actions that stay in PlayerViewModel (favorites and the player sheet live there) and are invoked by [PlayerDeleteActions]. */
class PlayerDeleteCallbacks(
    val toggleFavoriteSpecificSong: (song: Song, removing: Boolean) -> Unit,
    val hidePlayerSheet: () -> Unit,
)

/** Delete-from-device flow (single and batch, MediaStore permission request) extracted from PlayerViewModel. */
@OptIn(UnstableApi::class)
class PlayerDeleteActions(
    private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
    private val playbackStateHolder: PlaybackStateHolder,
    private val metadataEditStateHolder: MetadataEditStateHolder,
    private val songRemovalStateHolder: SongRemovalStateHolder,
    private val multiSelectionStateHolder: MultiSelectionStateHolder,
    private val playerUiState: MutableStateFlow<PlayerUiState>,
    private val playNextInserter: PlayNextInserter,
    private val mediaController: () -> MediaController?,
    private val toast: suspend (String) -> Unit,
    private val callbacks: PlayerDeleteCallbacks,
) {

    private val _deletePermissionRequest = MutableSharedFlow<android.content.IntentSender>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val deletePermissionRequest: SharedFlow<android.content.IntentSender> = _deletePermissionRequest.asSharedFlow()

    private var pendingDeleteSong: Song? = null
    private var pendingDeleteCallback: ((Boolean) -> Unit)? = null

    /**
     * Deletes all selected songs from device with confirmation.
     * Shows a single confirmation dialog for all songs.
     */
    private var pendingBatchDeleteSongs: List<Song>? = null
    private var pendingBatchDeleteSkippedCount: Int = 0
    private var pendingBatchDeleteOnComplete: (() -> Unit)? = null

    fun deleteSelectedFromDevice(activity: Activity, songs: List<Song>, onComplete: () -> Unit) {
        scope.launch {
            val currentSongId = playbackStateHolder.stablePlayerState.value.currentSong?.id
            val deletableSongs = songs.filter { it.id != currentSongId }

            if (deletableSongs.isEmpty()) {
                toast(getString(Res.string.player_cannot_delete_currently_playing))
                return@launch
            }

            val skippedCount = songs.size - deletableSongs.size

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val deleteRequests = withContext(dispatchers.io) {
                    deletableSongs.mapNotNull { song ->
                        io.github.raghavsatyadev.openplayer.utils.MediaStorePermissionHelper
                            .resolveDeleteRequestUri(
                                context = activity,
                                songId = song.id.toLongOrNull(),
                                contentUriString = song.contentUriString,
                                filePath = song.path,
                            )?.let { uri -> song to uri }
                    }
                }
                if (deleteRequests.size == deletableSongs.size) {
                    val uris = deleteRequests.map { it.second }.distinctBy { it.toString() }
                    val deleteRequest = io.github.raghavsatyadev.openplayer.utils.MediaStorePermissionHelper
                        .createDeleteRequest(activity, uris)
                    if (deleteRequest != null) {
                        val acceptedUriStrings = deleteRequest.acceptedUris
                            .mapTo(mutableSetOf()) { it.toString() }
                        val acceptedSongs = deleteRequests
                            .filter { (_, uri) -> uri.toString() in acceptedUriStrings }
                            .map { it.first }
                        val invalidRequestCount = deletableSongs.size - acceptedSongs.size

                        pendingBatchDeleteSongs = acceptedSongs
                        pendingBatchDeleteSkippedCount = skippedCount + invalidRequestCount
                        pendingBatchDeleteOnComplete = onComplete
                        _deletePermissionRequest.emit(deleteRequest.intentSender)
                        return@launch
                    }
                }
            }

            val confirmed = showMultiDeleteConfirmation(activity, deletableSongs.size)
            if (!confirmed) {
                onComplete()
                return@launch
            }

            var successCount = 0
            deletableSongs.forEach { song ->
                val success = metadataEditStateHolder.deleteSong(song)
                if (success) {
                    removeFromMediaControllerQueue(song.id)
                    removeSong(song)
                    successCount++
                }
            }

            when {
                successCount == deletableSongs.size && skippedCount == 0 ->
                    toast(
                        getPluralString(Res.plurals.n_files_deleted, successCount, successCount),
                    )
                successCount == deletableSongs.size && skippedCount > 0 ->
                    toast(
                        getString(Res.string.player_batch_delete_files_deleted_skipped_format,
                            successCount,
                            skippedCount,
                        ),
                    )
                successCount > 0 ->
                    toast(
                        getString(Res.string.player_batch_delete_partial_format,
                            successCount,
                            deletableSongs.size,
                        ),
                    )
                else ->
                    toast(getString(Res.string.player_delete_files_failed))
            }

            multiSelectionStateHolder.clearSelection()
            onComplete()
        }
    }

    private suspend fun showMultiDeleteConfirmation(activity: Activity, count: Int): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                if (activity.isFinishing || activity.isDestroyed) {
                    return@withContext false
                }

                val userChoice = CompletableDeferred<Boolean>()

                val dialog = MaterialAlertDialogBuilder(activity)
                    .setTitle(
                        getPluralString(Res.plurals.delete_songs_confirmation_title,
                            count,
                            count,
                        ),
                    )
                    .setMessage(getString(Res.string.delete_songs_permanent_message))
                    .setPositiveButton(getString(Res.string.delete_action)) { _, _ ->
                        userChoice.complete(true)
                    }
                    .setNegativeButton(getString(Res.string.cancel)) { _, _ ->
                        userChoice.complete(false)
                    }
                    .setOnCancelListener {
                        userChoice.complete(false)
                    }
                    .setCancelable(true)
                    .create()

                dialog.show()
                userChoice.await()
            } catch (e: Exception) {
                false
            }
        }
    }

    fun deleteFromDevice(activity: Activity, song: Song, onResult: (Boolean) -> Unit = {}) {
        scope.launch {
            if (playbackStateHolder.stablePlayerState.value.currentSong?.id == song.id) {
                toast(getString(Res.string.player_cannot_delete_currently_playing))
                onResult(false)
                return@launch
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val intentSender = withContext(dispatchers.io) {
                    io.github.raghavsatyadev.openplayer.utils.MediaStorePermissionHelper
                        .resolveDeleteRequestUri(
                            context = activity,
                            songId = song.id.toLongOrNull(),
                            contentUriString = song.contentUriString,
                            filePath = song.path,
                        )?.let { uri ->
                            io.github.raghavsatyadev.openplayer.utils.MediaStorePermissionHelper
                                .createDeleteRequestIntentSender(activity, listOf(uri))
                        }
                }
                if (intentSender != null) {
                    pendingDeleteSong = song
                    pendingDeleteCallback = onResult
                    _deletePermissionRequest.emit(intentSender)
                    return@launch
                }
            }

            val userConfirmed = songRemovalStateHolder.showDeleteConfirmation(activity, song)
            if (!userConfirmed) {
                onResult(false)
                return@launch
            }

            val success = songRemovalStateHolder.deleteSongFile(song)
            if (success) {
                toast(getString(Res.string.player_file_deleted))
                removeFromMediaControllerQueue(song.id)
                removeSong(song)
                onResult(true)
            } else {
                toast(getString(Res.string.player_delete_file_not_found))
                onResult(false)
            }
        }
    }

    /** Called from the UI after the user approves or denies the MediaStore delete request. */
    fun onDeletePermissionResult(granted: Boolean) {
        val batchSongs = pendingBatchDeleteSongs
        if (batchSongs != null) {
            val skippedCount = pendingBatchDeleteSkippedCount
            val onComplete = pendingBatchDeleteOnComplete
            pendingBatchDeleteSongs = null
            pendingBatchDeleteSkippedCount = 0
            pendingBatchDeleteOnComplete = null
            scope.launch {
                if (granted) {
                    batchSongs.forEach { song ->
                        removeFromMediaControllerQueue(song.id)
                        removeSong(song)
                    }
                    val count = batchSongs.size
                    if (skippedCount > 0) {
                        toast(
                            getString(Res.string.player_batch_delete_files_deleted_skipped_format,
                                count,
                                skippedCount,
                            ),
                        )
                    } else {
                        toast(
                            getPluralString(Res.plurals.n_files_deleted, count, count),
                        )
                    }
                } else {
                    toast(getString(Res.string.player_deletion_cancelled))
                }
                multiSelectionStateHolder.clearSelection()
                onComplete?.invoke()
            }
            return
        }

        val song = pendingDeleteSong ?: return
        val callback = pendingDeleteCallback
        pendingDeleteSong = null
        pendingDeleteCallback = null
        scope.launch {
            if (granted) {
                toast(getString(Res.string.player_file_deleted))
                removeFromMediaControllerQueue(song.id)
                removeSong(song)
                callback?.invoke(true)
            } else {
                callback?.invoke(false)
            }
        }
    }

    suspend fun removeSong(song: Song) {
        callbacks.toggleFavoriteSpecificSong(song, true)
        playbackStateHolder.setCurrentPosition(0L)
        playerUiState.update { currentState ->
            currentState.copy(
                currentPlaybackQueue = currentState.currentPlaybackQueue.removeSongById(song.id),
                currentQueueSourceName = ""
            )
        }
        callbacks.hidePlayerSheet()
        songRemovalStateHolder.removeSongFromLibrary(song)
    }

    private fun removeFromMediaControllerQueue(songId: String) {
        val controller = mediaController() ?: return

        try {
            val timeline = controller.currentTimeline
            val mediaItemCount = timeline.windowCount

            for (i in 0 until mediaItemCount) {
                val window = timeline.getWindow(i, Timeline.Window())
                if (window.mediaItem.mediaId == songId) {
                    playNextInserter.onItemRemoved(i)
                    controller.removeMediaItem(i)
                    break
                }
            }
        } catch (e: Exception) {
            Logger.withTag("MediaController").e { "Error removing from queue: ${e.message}" }
        }
    }
}
