package io.github.raghavsatyadev.openplayer.presentation.screens.historyimport

import org.jetbrains.compose.resources.StringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.raghavsatyadev.openplayer.data.history.importer.HistoryImportSummary
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource

private const val SAMPLE_SHOWN = 10

/** What an import did: counts, then the most played tracks that were not found. */
@Composable
fun HistoryImportSummaryBody(summary: HistoryImportSummary, onFinish: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (summary.imported > 0) {
                stringResource(Res.string.history_import_done_title, summary.imported)
            } else {
                stringResource(Res.string.history_import_done_none)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        StatRow(Res.string.history_import_stat_imported, summary.imported, always = true)
        StatRow(Res.string.history_import_stat_duplicates, summary.duplicates, always = true)
        StatRow(Res.string.history_import_stat_unmatched, summary.unmatched, always = true)
        StatRow(Res.string.history_import_stat_below_rule, summary.belowRule)
        StatRow(Res.string.history_import_stat_unparseable, summary.unparseable)
        StatRow(Res.string.history_import_stat_failed_files, summary.failedFiles)

        if (summary.unmatchedSample.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.history_import_unmatched_header),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            summary.unmatchedSample.take(SAMPLE_SHOWN).forEach { track ->
                Text(
                    text = stringResource(Res.string.history_import_unmatched_row, track.label, track.plays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(stringResource(Res.string.history_import_done_button))
        }
    }
}

@Composable
private fun StatRow(label: StringResource, value: Int, always: Boolean = false) {
    if (value == 0 && !always) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
