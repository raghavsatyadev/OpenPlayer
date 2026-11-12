package io.github.raghavsatyadev.openplayer.presentation.components.smartrules

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartCondition
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleField
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleOperator
import io.github.raghavsatyadev.openplayer.data.repository.SongUserMeta
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource

/** One editable condition: field and operator pickers, the value inputs it needs, and remove. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartRuleConditionRow(
    condition: SmartCondition,
    onFieldChange: (SmartRuleField) -> Unit,
    onChange: (SmartCondition) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 10.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                // Wraps the operator to a second line when both labels don't fit, so no chevron is clipped.
                FlowRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DropdownPicker(
                        options = SmartRuleField.entries,
                        selected = condition.field,
                        label = { smartRuleFieldLabel(it) },
                        onSelect = onFieldChange,
                    )
                    DropdownPicker(
                        options = condition.field.operators,
                        selected = condition.operator,
                        label = { smartRuleOperatorLabel(it) },
                        onSelect = { onChange(condition.copy(operator = it)) },
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.rounded_close_24),
                        contentDescription = stringResource(Res.string.smart_rules_remove_condition),
                    )
                }
            }
            AnimatedContent(
                targetState = condition.field.valueKind(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "conditionValue",
            ) { kind ->
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (kind) {
                        ValueKind.TEXT -> OutlinedTextField(
                            value = condition.text,
                            onValueChange = { onChange(condition.copy(text = it)) },
                            label = { Text(stringResource(Res.string.smart_rules_value_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ValueKind.RANGE -> {
                            NumberField(
                                value = condition.min,
                                label = stringResource(Res.string.smart_rules_range_from),
                                onValueChange = { onChange(condition.copy(min = it)) },
                                modifier = Modifier.weight(1f),
                            )
                            NumberField(
                                value = condition.max,
                                label = stringResource(Res.string.smart_rules_range_to),
                                onValueChange = { onChange(condition.copy(max = it)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        ValueKind.NUMBER -> NumberField(
                            value = condition.number,
                            label = smartRuleFieldLabel(condition.field),
                            onValueChange = {
                                val limit = if (condition.field == SmartRuleField.RATING) {
                                    SongUserMeta.MAX_RATING.toLong()
                                } else {
                                    Long.MAX_VALUE
                                }
                                onChange(condition.copy(number = it.coerceAtMost(limit)))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ValueKind.NONE -> Unit
                    }
                }
            }
        }
    }
}

private enum class ValueKind { TEXT, RANGE, NUMBER, NONE }

private fun SmartRuleField.valueKind(): ValueKind = when {
    usesText -> ValueKind.TEXT
    usesRange -> ValueKind.RANGE
    usesNumber -> ValueKind.NUMBER
    else -> ValueKind.NONE
}

@Composable
private fun NumberField(
    value: Long,
    label: String,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(value.toString()) }
    // Follow outside changes (like a clamped rating) without rewriting what is being typed.
    LaunchedEffect(value) {
        if (text.toLongOrNull() != value) text = value.toString()
    }
    OutlinedTextField(
        value = text,
        onValueChange = { typed ->
            text = typed.filter { it.isDigit() }.take(9)
            onValueChange(text.toLongOrNull() ?: 0L)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

/** A button showing the current choice that opens a menu of [options]. */
@Composable
internal fun <T> DropdownPicker(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box(modifier) {
        TextButton(onClick = { expanded = true }) {
            Text(label(selected), maxLines = 1, style = MaterialTheme.typography.labelLarge)
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.rounded_keyboard_arrow_down_24),
                contentDescription = null,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}
