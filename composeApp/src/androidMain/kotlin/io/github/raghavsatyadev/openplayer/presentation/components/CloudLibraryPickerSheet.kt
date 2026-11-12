package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.ui.theme.RoundedSans
import kotlinx.collections.immutable.ImmutableList
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource

/**
 * One selectable entry in a cloud library picker. Non-selectable entries
 * (e.g. Jellyfin movie/show views) are rendered grayed out.
 */
@Immutable
data class CloudLibraryPickerItem(
    val id: String,
    val name: String,
    val selectable: Boolean
)

/**
 * Confirm-based library picker shown right after a successful cloud server login
 * when the server exposes more than one music library. Selection is local until
 * the user confirms; dismissing means "sync everything".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudLibraryPickerSheet(
    items: ImmutableList<CloudLibraryPickerItem>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectableIds = remember(items) {
        items.filter { it.selectable }.map { it.id }.toSet()
    }
    var selectedIds by remember(items) { mutableStateOf(selectableIds) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(Res.string.auth_select_libraries_title),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = RoundedSans,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.auth_select_libraries_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = RoundedSans,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            CloudLibrarySelectorChoice(
                icon = ImageVector.vectorResource(R.drawable.rounded_select_all_24),
                title = stringResource(Res.string.dash_libraries_all),
                subtitle = stringResource(Res.string.dash_libraries_all_subtitle),
                checked = selectedIds.size == selectableIds.size,
                enabled = true,
                onClick = { selectedIds = selectableIds }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            items.forEach { item ->
                if (item.selectable) {
                    CloudLibrarySelectorChoice(
                        icon = ImageVector.vectorResource(R.drawable.rounded_library_music_filled_24),
                        title = item.name,
                        subtitle = stringResource(Res.string.dash_libraries_folder_subtitle),
                        checked = item.id in selectedIds,
                        enabled = true,
                        onClick = {
                            selectedIds = if (item.id in selectedIds) {
                                selectedIds - item.id
                            } else {
                                selectedIds + item.id
                            }
                        }
                    )
                } else {
                    CloudLibrarySelectorChoice(
                        icon = ImageVector.vectorResource(R.drawable.rounded_folder_filled_24),
                        title = item.name,
                        subtitle = stringResource(Res.string.dash_libraries_not_music),
                        checked = false,
                        enabled = false,
                        onClick = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onConfirm(selectedIds) },
                enabled = selectedIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    ImageVector.vectorResource(R.drawable.rounded_cloud_sync_filled_24),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(Res.string.auth_select_libraries_confirm),
                    fontFamily = RoundedSans
                )
            }
        }
    }
}

/**
 * Shared single-choice row used by the login-time picker and the dashboard
 * library selector sheets.
 */
@Composable
fun CloudLibrarySelectorChoice(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
        checked -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        checked -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        tonalElevation = if (checked) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (checked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(23.dp),
                    tint = when {
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        checked -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = RoundedSans,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = RoundedSans,
                    color = contentColor.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (enabled) {
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = if (checked) ImageVector.vectorResource(R.drawable.rounded_check_circle_filled_24) else ImageVector.vectorResource(R.drawable.rounded_radio_button_unchecked_filled_24),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
