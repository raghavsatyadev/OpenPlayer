package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource

/**
 * A list of strings edited as chips: the chosen values with a remove button each, a text field
 * that adds on Done or the add button, and chips for [suggestions] (existing values from other
 * songs) that match what is typed and are not chosen yet. Values compare ignoring case.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipInputField(
    label: String,
    hint: String,
    values: List<String>,
    suggestions: List<String>,
    onValuesChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    maxSuggestions: Int = 12
) {
    var draft by remember { mutableStateOf("") }

    fun commit(text: String) {
        val value = text.trim()
        if (value.isNotEmpty() && values.none { it.equals(value, ignoreCase = true) }) {
            onValuesChange(values + value)
        }
        draft = ""
    }

    val shownSuggestions = remember(suggestions, values, draft) {
        suggestions
            .filter { s -> values.none { it.equals(s, ignoreCase = true) } }
            .filter { draft.isBlank() || it.contains(draft.trim(), ignoreCase = true) }
            .take(maxSuggestions)
    }

    Column(modifier = modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

        if (values.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                values.forEach { value ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        InputChip(
                            selected = true,
                            onClick = { onValuesChange(values - value) },
                            label = { Text(value) },
                            trailingIcon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.rounded_close_24),
                                    contentDescription = stringResource(Res.string.user_meta_cd_remove, value),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.replace("\n", "") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(hint) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit(draft) }),
                trailingIcon = {
                    AnimatedVisibility(visible = draft.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
                        IconButton(onClick = { commit(draft) }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.rounded_add_24),
                                contentDescription = stringResource(Res.string.user_meta_cd_add)
                            )
                        }
                    }
                }
            )
        }

        if (shownSuggestions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                shownSuggestions.forEach { suggestion ->
                    AssistChip(
                        onClick = { commit(suggestion) },
                        label = { Text(suggestion) }
                    )
                }
            }
        }
    }
}
