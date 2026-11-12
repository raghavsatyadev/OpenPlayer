package io.github.raghavsatyadev.openplayer.presentation.screens.historyimport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.history.importer.HistorySource
import io.github.raghavsatyadev.openplayer.presentation.screens.SettingsItem

/** Exports often arrive as octet-stream or text, so the picker is not narrowed by type. */
private val PICKER_TYPES = arrayOf("*/*")

/** The settings row that opens the import sheet. */
@Composable
fun HistoryImportSettingsItem(modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    SettingsItem(
        title = stringResource(R.string.history_import_title),
        subtitle = stringResource(R.string.history_import_subtitle),
        leadingIcon = { Icon(painterResource(R.drawable.rounded_history_24), null, tint = MaterialTheme.colorScheme.secondary) },
        modifier = modifier,
        onClick = { open = true }
    )
    if (open) HistoryImportSheet(onDismiss = { open = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryImportSheet(
    onDismiss: () -> Unit,
    viewModel: HistoryImportViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf(HistorySource.LASTFM) }

    val single = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.import(pending, listOf(uri))
    }
    val multiple = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.import(pending, uris)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
        }
    ) {
        HistoryImportContent(
            state = state,
            onPick = { source ->
                pending = source
                // Last.fm is a single CSV; Spotify, ListenBrainz and YouTube Music exports come as several files (or zip).
                if (source == HistorySource.LASTFM) single.launch(PICKER_TYPES) else multiple.launch(PICKER_TYPES)
            },
            onFinish = viewModel::reset,
            modifier = Modifier.navigationBarsPadding()
        )
        Spacer(Modifier.height(16.dp))
    }
}
