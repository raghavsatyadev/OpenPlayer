package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.presentation.utils.rememberAppToaster
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SavedQueuesViewModel
import kotlinx.coroutines.launch

/**
 * The save-queue dialog and the saved-queues sheet, wired to the player. Show them with
 * [showSave] and [showList]. Saving stores [queue] with the song at [currentIndex] and the
 * playback position now; picking a saved queue plays it from that song and position.
 */
@Composable
fun SavedQueuesHost(
    showSave: Boolean,
    showList: Boolean,
    queue: List<Song>,
    currentIndex: Int,
    playerViewModel: PlayerViewModel,
    onDismissSave: () -> Unit,
    onDismissList: () -> Unit,
    viewModel: SavedQueuesViewModel = koinViewModel()
) {
    val toaster = rememberAppToaster()
    val savedToast = stringResource(Res.string.saved_queues_saved_toast)
    val restoreEmptyToast = stringResource(Res.string.saved_queues_restore_empty)
    val scope = rememberCoroutineScope()

    if (showSave) {
        SaveQueueDialog(
            defaultName = defaultSavedQueueName(System.currentTimeMillis()),
            onConfirm = { name ->
                viewModel.save(
                    name = name,
                    songs = queue,
                    currentIndex = currentIndex.coerceIn(0, (queue.size - 1).coerceAtLeast(0)),
                    positionMs = playerViewModel.currentPlaybackPosition.value
                )
                toaster.show(savedToast)
                onDismissSave()
            },
            onDismiss = onDismissSave
        )
    }

    if (showList) {
        val queues by viewModel.queues.collectAsStateWithLifecycle()
        SavedQueuesSheet(
            queues = queues,
            onPlay = { saved ->
                scope.launch {
                    val plan = viewModel.restorePlan(saved)
                    if (plan.isEmpty) {
                        toaster.show(restoreEmptyToast, long = true)
                        return@launch
                    }
                    if (plan.missingCount > 0) {
                        toaster.show(
                            getString(Res.string.saved_queues_restore_missing, plan.missingCount),
                            long = true
                        )
                    }
                    playerViewModel.playSongsFromPosition(
                        songsToPlay = plan.songs,
                        startSong = plan.songs[plan.startIndex],
                        startPositionMs = plan.positionMs,
                        queueName = saved.name
                    )
                    onDismissList()
                }
            },
            onDelete = viewModel::delete,
            onDismiss = onDismissList
        )
    }
}
