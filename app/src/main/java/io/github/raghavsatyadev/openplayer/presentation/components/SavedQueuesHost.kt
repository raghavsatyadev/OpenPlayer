package io.github.raghavsatyadev.openplayer.presentation.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.model.Song
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
    viewModel: SavedQueuesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
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
                Toast.makeText(context, R.string.saved_queues_saved_toast, Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, R.string.saved_queues_restore_empty, Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    if (plan.missingCount > 0) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.saved_queues_restore_missing, plan.missingCount),
                            Toast.LENGTH_LONG
                        ).show()
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
