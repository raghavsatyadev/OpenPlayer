package io.github.raghavsatyadev.openplayer.presentation.components.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.service.repeatn.RepeatNCounter
import kotlinx.coroutines.flow.StateFlow

/** What the repeat button needs for repeat-N, provided once above the full player. */
class RepeatNUi(
    val remaining: StateFlow<Int>,
    val onStart: (Int) -> Unit,
    val onCancel: () -> Unit,
)

val LocalRepeatN: ProvidableCompositionLocal<RepeatNUi?> = compositionLocalOf { null }

/** Small count badge that slides between values and pops in and out. */
@Composable
fun RepeatNBadge(remaining: Int, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = remaining,
        modifier = modifier,
        transitionSpec = {
            val spec = if (initialState == 0 || targetState == 0) {
                (scaleIn(tween(180)) + fadeIn(tween(180))) togetherWith
                    (scaleOut(tween(120)) + fadeOut(tween(120)))
            } else {
                (slideInVertically(tween(200)) { -it } + fadeIn(tween(200))) togetherWith
                    (slideOutVertically(tween(200)) { it } + fadeOut(tween(200)))
            }
            spec.using(SizeTransform(clip = false))
        },
        label = "repeatNBadge",
    ) { value ->
        if (value > 0) {
            val cd = stringResource(R.string.repeat_n_badge_cd, value)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                    .semantics { contentDescription = cd },
            ) {
                Text(
                    text = value.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        } else {
            Spacer(Modifier.size(0.dp))
        }
    }
}

/** The picker body: counts 2..10, plus a stop action while a count runs. Testable without a window. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RepeatNPickerContent(
    remaining: Int,
    onPick: (Int) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    initialSelection: Int = RepeatNCounter.MIN_TIMES,
) {
    var selected by remember { mutableIntStateOf(initialSelection) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.rounded_repeat_one_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.repeat_n_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    RepeatNBadge(remaining)
                }
                Text(
                    text = stringResource(R.string.repeat_n_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (n in RepeatNCounter.MIN_TIMES..RepeatNCounter.MAX_TIMES) {
                val cd = stringResource(R.string.repeat_n_option_cd, n)
                FilterChip(
                    selected = selected == n,
                    onClick = { selected = n },
                    label = {
                        Text(
                            text = n.toString(),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    },
                    // Same width for every count so the chips line up as a grid.
                    modifier = Modifier
                        .width(56.dp)
                        .semantics { contentDescription = cd },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (remaining > 0) {
                TextButton(onClick = onStop) { Text(stringResource(R.string.repeat_n_stop)) }
                Spacer(Modifier.size(8.dp))
            }
            FilledTonalButton(onClick = { onPick(selected) }) {
                Text(stringResource(R.string.repeat_n_start))
            }
        }
    }
}

/** Bottom sheet wrapper around [RepeatNPickerContent]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatNPickerSheet(
    remaining: Int,
    onPick: (Int) -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        RepeatNPickerContent(
            remaining = remaining,
            onPick = {
                onPick(it)
                onDismiss()
            },
            onStop = {
                onStop()
                onDismiss()
            },
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

/** Collects the live count from [ui] (0 when repeat-N is not provided). */
@Composable
fun rememberRepeatNRemaining(ui: RepeatNUi?): Int {
    if (ui == null) return 0
    val value by ui.remaining.collectAsStateWithLifecycle()
    return value
}
