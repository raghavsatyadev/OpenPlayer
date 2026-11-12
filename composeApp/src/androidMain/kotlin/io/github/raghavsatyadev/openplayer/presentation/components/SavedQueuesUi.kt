package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.repository.SavedQueue
import io.github.raghavsatyadev.openplayer.utils.formatDuration
import java.text.DateFormat
import java.util.Date
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.pluralStringResource
import io.github.raghavsatyadev.openplayer.resources.stringResource

/** Default name for a new saved queue: the current date and time. */
fun defaultSavedQueueName(nowMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(nowMs))

/** Asks for the name of the queue being saved, prefilled with [defaultName]. */
@Composable
fun SaveQueueDialog(
    defaultName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.saved_queues_save_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.replace("\n", "") },
                label = { Text(stringResource(Res.string.saved_queues_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim().ifEmpty { defaultName }) }) {
                Text(stringResource(Res.string.saved_queues_save_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.saved_queues_cancel)) }
        }
    )
}

/** Bottom sheet listing [queues]; tapping one plays it, the bin deletes it after a confirmation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedQueuesSheet(
    queues: List<SavedQueue>,
    onPlay: (SavedQueue) -> Unit,
    onDelete: (SavedQueue) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        SavedQueuesContent(queues = queues, onPlay = onPlay, onDelete = onDelete)
    }
}

/** The list inside [SavedQueuesSheet], stateless apart from the delete confirmation. */
@Composable
fun SavedQueuesContent(
    queues: List<SavedQueue>,
    onPlay: (SavedQueue) -> Unit,
    onDelete: (SavedQueue) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<SavedQueue?>(null) }

    Column(modifier = modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(
            text = stringResource(Res.string.saved_queues_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        AnimatedVisibility(visible = queues.isEmpty()) {
            Text(
                text = stringResource(Res.string.saved_queues_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
            )
        }
        LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
            items(queues, key = { it.id }) { queue ->
                SavedQueueRow(
                    queue = queue,
                    onPlay = { onPlay(queue) },
                    onDelete = { pendingDelete = queue },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    pendingDelete?.let { queue ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(Res.string.saved_queues_delete_title)) },
            text = { Text(stringResource(Res.string.saved_queues_delete_message, queue.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(queue)
                    pendingDelete = null
                }) { Text(stringResource(Res.string.saved_queues_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(Res.string.saved_queues_cancel))
                }
            }
        )
    }
}

@Composable
private fun SavedQueueRow(
    queue: SavedQueue,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val count = queue.songIds.size
    val countText = pluralStringResource(Res.plurals.saved_queues_song_count, count, count)
    val date = remember(queue.createdAt) { defaultSavedQueueName(queue.createdAt) }
    ListItem(
        modifier = modifier.clickable(onClick = onPlay),
        leadingContent = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.rounded_queue_music_24),
                contentDescription = stringResource(Res.string.saved_queues_cd_play, queue.name)
            )
        },
        headlineContent = { Text(queue.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(
                    text = stringResource(Res.string.saved_queues_item_summary, countText, date),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (queue.positionMs > 0L) {
                    Text(
                        text = stringResource(Res.string.saved_queues_item_position, formatDuration(queue.positionMs)),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.rounded_delete_24),
                        contentDescription = stringResource(Res.string.saved_queues_cd_delete, queue.name)
                    )
                }
            }
        }
    )
}
