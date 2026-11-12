package io.github.raghavsatyadev.openplayer.presentation.screens.historyimport

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.history.importer.HistoryImportState
import io.github.raghavsatyadev.openplayer.data.history.importer.HistorySource
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource

/**
 * The body of the import sheet: pick a source, watch progress, read the summary. Stateless, so
 * it can be shown with made-up state.
 */
@Composable
fun HistoryImportContent(
    state: HistoryImportState,
    onPick: (HistorySource) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.history_import_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 12 }) togetherWith (fadeOut() + slideOutVertically { -it / 12 })
            },
            label = "historyImportState"
        ) { current ->
            when (current) {
                HistoryImportState.Idle -> IdleBody(onPick)
                is HistoryImportState.Working -> WorkingBody(current)
                is HistoryImportState.Done -> HistoryImportSummaryBody(current.summary, onFinish)
                is HistoryImportState.Failed -> FailedBody(onFinish)
            }
        }
    }
}

@Composable
private fun IdleBody(onPick: (HistorySource) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.history_import_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SourceRow(R.drawable.rounded_file_download_24, R.string.history_import_lastfm, R.string.history_import_lastfm_desc) {
            onPick(HistorySource.LASTFM)
        }
        SourceRow(R.drawable.rounded_library_music_24, R.string.history_import_spotify, R.string.history_import_spotify_desc) {
            onPick(HistorySource.SPOTIFY)
        }
        SourceRow(R.drawable.rounded_audio_file_24, R.string.history_import_listenbrainz, R.string.history_import_listenbrainz_desc) {
            onPick(HistorySource.LISTENBRAINZ)
        }
        SourceRow(R.drawable.rounded_play_circle_24, R.string.history_import_youtube_music, R.string.history_import_youtube_music_desc) {
            onPick(HistorySource.YOUTUBE_MUSIC)
        }
    }
}

@Composable
private fun SourceRow(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes subtitle: Int,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(Modifier.padding(end = 16.dp).size(24.dp), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.secondary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WorkingBody(state: HistoryImportState.Working) {
    val phase = when (state.phase) {
        HistoryImportState.Phase.READING -> R.string.history_import_phase_reading
        HistoryImportState.Phase.MATCHING -> R.string.history_import_phase_matching
        HistoryImportState.Phase.SAVING -> R.string.history_import_phase_saving
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(phase), style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
            stringResource(Res.string.history_import_plays_read, state.parsed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FailedBody(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(Res.string.history_import_failed_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.history_import_failed_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onRetry) { Text(stringResource(Res.string.history_import_try_again)) }
    }
}
