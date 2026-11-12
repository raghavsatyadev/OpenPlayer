@file:OptIn(ExperimentalMaterial3Api::class, FlowPreview::class, ExperimentalCoroutinesApi::class)

package io.github.raghavsatyadev.openplayer.presentation.components.smartrules

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.model.Playlist
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleCodec
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SmartRulePlaylistViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Sheet to create a rule playlist, or with [editing] to change the rules of an existing one.
 * Shows nothing while [visible] is false.
 */
@Composable
fun SmartRulePlaylistSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    editing: Playlist? = null,
    viewModel: SmartRulePlaylistViewModel = koinViewModel(),
) {
    if (!visible) return

    var state by remember(editing?.id) {
        mutableStateOf(
            editing?.let { playlist ->
                SmartRuleCodec.fromSource(playlist.source)?.let { SmartRuleEditorState.from(playlist.name, it) }
            } ?: SmartRuleEditorState().addCondition()
        )
    }
    var previewCount by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(viewModel) {
        snapshotFlow { state.toDefinition() }
            .distinctUntilChanged()
            .debounce(250)
            .flatMapLatest { viewModel.previewCount(it) }
            .collect { previewCount = it }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            text = stringResource(
                if (editing == null) R.string.smart_rules_sheet_title_new else R.string.smart_rules_sheet_title_edit
            ),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        )
        SmartRuleEditorContent(
            state = state,
            previewCount = previewCount,
            onStateChange = { state = it },
            onSave = {
                viewModel.save(editing?.id, state.name, state.toDefinition(), onSaved = onDismiss)
            },
            onCancel = onDismiss,
            modifier = Modifier.fillMaxHeight(0.85f),
        )
    }
}
