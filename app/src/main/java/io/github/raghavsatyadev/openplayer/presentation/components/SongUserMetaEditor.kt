package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.repository.SongUserMeta
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SongUserMetaViewModel

/**
 * The song options sheet entry: a button that shows the song's rating and opens
 * [SongUserMetaEditorSheet]. Reads and saves through [SongUserMetaViewModel].
 */
@Composable
fun SongUserMetaEntryButton(
    song: Song,
    modifier: Modifier = Modifier,
    viewModel: SongUserMetaViewModel = koinViewModel()
) {
    val meta by remember(song.id) { viewModel.metaFor(song.id) }.collectAsStateWithLifecycle(initialValue = null)
    var showEditor by remember { mutableStateOf(false) }

    FilledTonalButton(
        modifier = modifier.fillMaxWidth().heightIn(min = 66.dp),
        onClick = { showEditor = true }
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(
                if ((meta?.rating ?: 0) > 0) R.drawable.rounded_star_filled_24 else R.drawable.rounded_star_24
            ),
            contentDescription = null
        )
        Spacer(Modifier.size(10.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(stringResource(R.string.user_meta_entry_title))
            val summary = summaryText(meta)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                )
            }
        }
    }

    if (showEditor) {
        val moodSuggestions by produceSuggestions { viewModel.moodSuggestions() }
        val tagSuggestions by produceSuggestions { viewModel.tagSuggestions() }
        SongUserMetaEditorSheet(
            songTitle = song.title,
            initial = meta ?: SongUserMeta(song.id),
            moodSuggestions = moodSuggestions,
            tagSuggestions = tagSuggestions,
            onSave = { viewModel.save(it.copy(songId = song.id)) },
            onDismiss = { showEditor = false }
        )
    }
}

@Composable
private fun produceSuggestions(load: suspend () -> List<String>) =
    androidx.compose.runtime.produceState(initialValue = emptyList<String>()) { value = load() }

@Composable
private fun summaryText(meta: SongUserMeta?): String? {
    if (meta == null || meta.isEmpty) return null
    val parts = buildList {
        if (meta.rating > 0) add(stringResource(R.string.user_meta_rating_value, meta.rating))
        if (meta.moods.isNotEmpty()) add(stringResource(R.string.user_meta_summary_moods, meta.moods.size))
        if (meta.tags.isNotEmpty()) add(stringResource(R.string.user_meta_summary_tags, meta.tags.size))
    }
    return parts.joinToString(" - ")
}

/** Bottom sheet around [SongUserMetaEditorContent]; Save hands the edited value to [onSave]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongUserMetaEditorSheet(
    songTitle: String,
    initial: SongUserMeta,
    moodSuggestions: List<String>,
    tagSuggestions: List<String>,
    onSave: (SongUserMeta) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        SongUserMetaEditorContent(
            songTitle = songTitle,
            initial = initial,
            moodSuggestions = moodSuggestions,
            tagSuggestions = tagSuggestions,
            onSave = {
                onSave(it)
                onDismiss()
            },
            onCancel = onDismiss
        )
    }
}

/** The editor itself, with no state of its own beyond the edit in progress, so it can be previewed and tested. */
@Composable
fun SongUserMetaEditorContent(
    songTitle: String,
    initial: SongUserMeta,
    moodSuggestions: List<String>,
    tagSuggestions: List<String>,
    onSave: (SongUserMeta) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rating by remember(initial) { mutableStateOf(initial.rating) }
    var moods by remember(initial) { mutableStateOf(initial.moods) }
    var tags by remember(initial) { mutableStateOf(initial.tags) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.user_meta_editor_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = songTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.user_meta_rating),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            StarRatingRow(rating = rating, onRatingChange = { rating = it })
            Text(
                text = if (rating == 0) {
                    stringResource(R.string.user_meta_rating_none)
                } else {
                    stringResource(R.string.user_meta_rating_value, rating)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ChipInputField(
            label = stringResource(R.string.user_meta_moods),
            hint = stringResource(R.string.user_meta_add_mood_hint),
            values = moods,
            suggestions = moodSuggestions,
            onValuesChange = { moods = it }
        )
        ChipInputField(
            label = stringResource(R.string.user_meta_tags),
            hint = stringResource(R.string.user_meta_add_tag_hint),
            values = tags,
            suggestions = tagSuggestions,
            onValuesChange = { tags = it }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.user_meta_cancel)) }
            FilledTonalButton(
                onClick = { onSave(SongUserMeta(initial.songId, rating, moods, tags)) }
            ) { Text(stringResource(R.string.user_meta_save)) }
        }
    }
}

/** Five stars; tapping star N sets N, tapping the current rating clears it. */
@Composable
fun StarRatingRow(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (star in 1..SongUserMeta.MAX_RATING) {
            val filled = star <= rating
            val tint by animateColorAsState(
                targetValue = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                label = "starTint"
            )
            val scale by animateFloatAsState(
                targetValue = if (filled) 1.1f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "starScale"
            )
            Icon(
                imageVector = ImageVector.vectorResource(
                    if (filled) R.drawable.rounded_star_filled_24 else R.drawable.rounded_star_24
                ),
                contentDescription = stringResource(R.string.user_meta_cd_star, star),
                tint = tint,
                modifier = Modifier
                    .size(44.dp)
                    .padding(6.dp)
                    .scale(scale)
                    .clickable { onRatingChange(if (star == rating) 0 else star) }
            )
        }
    }
}
