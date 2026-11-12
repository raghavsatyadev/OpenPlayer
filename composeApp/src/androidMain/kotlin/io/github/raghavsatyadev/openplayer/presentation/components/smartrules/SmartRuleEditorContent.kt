package io.github.raghavsatyadev.openplayer.presentation.components.smartrules

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartMatchMode
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartSortField
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.pluralStringResource
import io.github.raghavsatyadev.openplayer.resources.stringResource

/**
 * The rule playlist editor with all its state passed in: name, match mode, condition rows
 * (added and removed with motion), sort and limit, and a bottom bar with the live
 * [previewCount] (null while counting) and Save.
 */
@Composable
fun SmartRuleEditorContent(
    state: SmartRuleEditorState,
    previewCount: Int?,
    onStateChange: (SmartRuleEditorState) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "name") {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onStateChange(state.copy(name = it)) },
                    label = { Text(stringResource(Res.string.smart_rules_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = "match") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.smart_rules_match_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilterChip(
                        selected = state.matchMode == SmartMatchMode.ALL,
                        onClick = { onStateChange(state.copy(matchMode = SmartMatchMode.ALL)) },
                        label = { Text(stringResource(Res.string.smart_rules_match_all)) },
                    )
                    FilterChip(
                        selected = state.matchMode == SmartMatchMode.ANY,
                        onClick = { onStateChange(state.copy(matchMode = SmartMatchMode.ANY)) },
                        label = { Text(stringResource(Res.string.smart_rules_match_any)) },
                    )
                }
            }
            item(key = "conditionsHeader") {
                Text(
                    text = stringResource(Res.string.smart_rules_conditions_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.conditions.isEmpty()) {
                item(key = "noConditions") {
                    Text(
                        text = stringResource(Res.string.smart_rules_no_conditions),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            items(state.conditions, key = { it.id }) { condition ->
                SmartRuleConditionRow(
                    condition = condition,
                    onFieldChange = { onStateChange(state.changeField(condition.id, it)) },
                    onChange = { changed -> onStateChange(state.updateCondition(condition.id) { changed }) },
                    onRemove = { onStateChange(state.removeCondition(condition.id)) },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "add") {
                FilledTonalButton(
                    onClick = { onStateChange(state.addCondition()) },
                    modifier = Modifier.animateItem(),
                ) {
                    Icon(ImageVector.vectorResource(R.drawable.rounded_add_24), contentDescription = null)
                    Text(
                        text = stringResource(Res.string.smart_rules_add_condition),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            item(key = "sort") { SortAndLimit(state, onStateChange, Modifier.animateItem()) }
        }
        PreviewBar(
            previewCount = previewCount,
            canSave = state.canSave,
            onSave = onSave,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun SortAndLimit(
    state: SmartRuleEditorState,
    onStateChange: (SmartRuleEditorState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.smart_rules_sort_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            val choices = listOf<SmartSortField?>(null) + SmartSortField.entries
            DropdownPicker(
                options = choices,
                selected = state.sort?.field,
                label = { field ->
                    if (field == null) stringResource(Res.string.smart_rules_sort_none) else smartSortFieldLabel(field)
                },
                onSelect = { onStateChange(state.setSortField(it)) },
            )
            val canReverse = state.sort != null && state.sort.field != SmartSortField.RANDOM
            AnimatedVisibility(visible = canReverse, enter = fadeIn(), exit = fadeOut()) {
                val rotation by animateFloatAsState(
                    targetValue = if (state.sort?.descending == true) 0f else 180f,
                    label = "sortDirection",
                )
                IconButton(onClick = { onStateChange(state.toggleSortDirection()) }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.rounded_arrow_downward_24),
                        contentDescription = stringResource(Res.string.smart_rules_sort_direction),
                        modifier = Modifier.rotate(rotation),
                    )
                }
            }
        }
        OutlinedTextField(
            value = state.limitText,
            onValueChange = { onStateChange(state.copy(limitText = it.filter(Char::isDigit).take(5))) },
            label = { Text(stringResource(Res.string.smart_rules_limit_label)) },
            placeholder = { Text(stringResource(Res.string.smart_rules_limit_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(160.dp),
        )
    }
}

@Composable
private fun PreviewBar(
    previewCount: Int?,
    canSave: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(
                targetState = previewCount,
                transitionSpec = {
                    (slideInVertically { it / 2 } + fadeIn()) togetherWith
                        (slideOutVertically { -it / 2 } + fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "previewCount",
            ) { count ->
                Text(
                    text = if (count == null) {
                        stringResource(Res.string.smart_rules_preview_loading)
                    } else {
                        pluralStringResource(Res.plurals.smart_rules_preview_count, count, count)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            TextButton(onClick = onCancel) { Text(stringResource(Res.string.smart_rules_cancel)) }
            Button(onClick = onSave, enabled = canSave) { Text(stringResource(Res.string.smart_rules_save)) }
        }
    }
}
