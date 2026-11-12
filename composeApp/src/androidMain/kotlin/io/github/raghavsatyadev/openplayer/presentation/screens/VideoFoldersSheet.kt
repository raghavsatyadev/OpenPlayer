package io.github.raghavsatyadev.openplayer.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.raghavsatyadev.openplayer.R
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

private const val STORAGE_ROOT = "/storage/emulated/0/"

/**
 * Pick the folders to scan for videos and the ones to skip. With nothing included, every
 * folder is scanned; a skipped folder wins over an included one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoFoldersSheet(
    folders: List<String>,
    included: Set<String>,
    excluded: Set<String>,
    onChange: (included: Set<String>, excluded: Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(stringResource(R.string.setting_video_folders_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.setting_video_folders_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            if (included.isNotEmpty() || excluded.isNotEmpty()) {
                TextButton(onClick = { onChange(emptySet(), emptySet()) }) {
                    Text(stringResource(R.string.setting_video_folders_reset))
                }
            }
        }
        if (folders.isEmpty()) {
            Text(
                stringResource(R.string.lib_empty_videos_title),
                modifier = Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(folders, key = { it }) { folder ->
                FolderRow(
                    folder = folder,
                    isIncluded = folder in included,
                    isExcluded = folder in excluded,
                    onInclude = { on ->
                        onChange(if (on) included + folder else included - folder, excluded - folder)
                    },
                    onExclude = { on ->
                        onChange(included - folder, if (on) excluded + folder else excluded - folder)
                    },
                )
            }
        }
    }
}

@Composable
private fun FolderRow(
    folder: String,
    isIncluded: Boolean,
    isExcluded: Boolean,
    onInclude: (Boolean) -> Unit,
    onExclude: (Boolean) -> Unit,
) {
    val relative = folder.removePrefix(STORAGE_ROOT)
    val depth = relative.count { it == '/' }
    val shape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTL = 18.dp, smoothnessAsPercentTL = 60,
            cornerRadiusTR = 18.dp, smoothnessAsPercentTR = 60,
            cornerRadiusBL = 18.dp, smoothnessAsPercentBL = 60,
            cornerRadiusBR = 18.dp, smoothnessAsPercentBR = 60,
        )
    }
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painterResource(
                    if (isExcluded) R.drawable.rounded_folder_off_filled_24 else R.drawable.rounded_folder_24
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    relative.substringAfterLast('/'),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (depth > 0) {
                    Text(
                        relative.substringBeforeLast('/'),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            FilledIconToggleButton(checked = isIncluded, onCheckedChange = onInclude) {
                Icon(
                    painterResource(R.drawable.rounded_check_24),
                    contentDescription = stringResource(R.string.setting_video_folder_include),
                )
            }
            FilledIconToggleButton(
                checked = isExcluded,
                onCheckedChange = onExclude,
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    checkedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(
                    painterResource(R.drawable.rounded_block_24),
                    contentDescription = stringResource(R.string.setting_video_folder_exclude),
                )
            }
        }
    }
}
