package io.github.raghavsatyadev.openplayer.presentation.components.parametric

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricBand
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ParametricEqUiState
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ParametricEqViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ParametricMessage
import java.util.Locale

class ParametricEqActions(
    val onPreampChange: (Float) -> Unit,
    val onAddBand: () -> Unit,
    val onBandChange: (Int, ParametricBand) -> Unit,
    val onRemoveBand: (Int) -> Unit,
    val onClear: () -> Unit,
    val onImportClick: () -> Unit,
    val onExportClick: () -> Unit,
)

/** Graphic / Parametric switch. Only one of the two equalisers is active at a time. */
@Composable
fun EqualizerModeSwitch(
    parametric: Boolean,
    parametricAvailable: Boolean = true,
    onParametricChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !parametric,
                onClick = { onParametricChange(false) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) { Text(stringResource(R.string.parametric_eq_mode_graphic)) }
            SegmentedButton(
                selected = parametric,
                onClick = { onParametricChange(true) },
                enabled = parametricAvailable,
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) { Text(stringResource(R.string.parametric_eq_mode_parametric)) }
        }
        AnimatedVisibility(!parametricAvailable) {
            Text(stringResource(R.string.parametric_eq_unavailable), color = MaterialTheme.colorScheme.error)
        }
        Text(
            stringResource(R.string.parametric_eq_mode_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Hooks the editor to the view model, the file pickers and the status text. */
@Composable
fun ParametricEqSection(viewModel: ParametricEqViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importFrom(uri)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) viewModel.exportTo(uri)
    }
    val fileName = stringResource(R.string.parametric_eq_export_file_name)
    val actions = remember(viewModel, importLauncher, exportLauncher, fileName) {
        ParametricEqActions(
            onPreampChange = viewModel::setPreamp,
            onAddBand = viewModel::addBand,
            onBandChange = viewModel::updateBand,
            onRemoveBand = viewModel::removeBand,
            onClear = viewModel::clearBands,
            onImportClick = { importLauncher.launch(arrayOf("text/plain", "*/*")) },
            onExportClick = { exportLauncher.launch(fileName) },
        )
    }
    ParametricEqEditor(state, actions, modifier)
}

/** Stateless parametric editor: response curve, preamp, band list, add / import / export. */
@Composable
fun ParametricEqEditor(
    state: ParametricEqUiState,
    actions: ParametricEqActions,
    modifier: Modifier = Modifier,
) {
    val profile = state.profile
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp).animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.parametric_eq_title), style = MaterialTheme.typography.titleLarge)
        AnimatedVisibility(!state.available) {
            Text(stringResource(R.string.parametric_eq_unavailable), color = MaterialTheme.colorScheme.error)
        }
        ParametricResponseCurve(profile, stringResource(R.string.parametric_eq_curve_cd))
        LabeledSlider(
            label = stringResource(R.string.parametric_eq_preamp),
            valueText = stringResource(R.string.parametric_eq_db_value, String.format(Locale.US, "%+.1f", profile.preampDb)),
            value = profile.preampDb,
            valueRange = -24f..12f,
            onValueChange = { actions.onPreampChange(Math.round(it * 10f) / 10f) },
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.parametric_eq_bands_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            AnimatedVisibility(profile.bands.isNotEmpty()) {
                TextButton(onClick = actions.onClear) { Text(stringResource(R.string.parametric_eq_clear)) }
            }
        }
        AnimatedVisibility(profile.bands.isEmpty(), enter = fadeIn(), exit = fadeOut()) {
            Text(
                stringResource(R.string.parametric_eq_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        profile.bands.forEachIndexed { index, band ->
            val visible = remember { MutableTransitionState(false).apply { targetState = true } }
            AnimatedVisibility(
                visibleState = visible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                ParametricBandRow(
                    number = index + 1,
                    band = band,
                    onChange = { actions.onBandChange(index, it) },
                    onRemove = { actions.onRemoveBand(index) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = actions.onAddBand) {
                Icon(ImageVector.vectorResource(R.drawable.rounded_add_24), null)
                Text(stringResource(R.string.parametric_eq_add_band), Modifier.padding(start = 6.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = actions.onImportClick) {
                Icon(ImageVector.vectorResource(R.drawable.rounded_upload_file_24), null)
                Text(stringResource(R.string.parametric_eq_import), Modifier.padding(start = 6.dp))
            }
            OutlinedButton(onClick = actions.onExportClick, enabled = profile.bands.isNotEmpty()) {
                Icon(ImageVector.vectorResource(R.drawable.rounded_file_download_24), null)
                Text(stringResource(R.string.parametric_eq_export), Modifier.padding(start = 6.dp))
            }
        }
        AnimatedVisibility(state.message != null) {
            val msg = state.message
            val failed = msg != null && msg != ParametricMessage.EXPORTED
            Text(
                text = stringResource(
                    when (msg) {
                        ParametricMessage.IMPORT_NO_FILTERS -> R.string.parametric_eq_message_no_filters
                        ParametricMessage.IMPORT_FAILED -> R.string.parametric_eq_message_import_failed
                        ParametricMessage.EXPORT_FAILED -> R.string.parametric_eq_message_export_failed
                        else -> R.string.parametric_eq_message_exported
                    },
                ),
                color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
