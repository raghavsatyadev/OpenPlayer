package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import android.app.Activity
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import io.github.raghavsatyadev.openplayer.data.model.Playlist
import io.github.raghavsatyadev.openplayer.presentation.components.subcomps.AutoSizingTextToFill
import io.github.raghavsatyadev.openplayer.presentation.components.subcomps.TightWrapText
import io.github.raghavsatyadev.openplayer.ui.theme.RoundedSans
import io.github.raghavsatyadev.openplayer.utils.resolvePlaylistCoverContentColor
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import io.github.raghavsatyadev.openplayer.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import androidx.compose.runtime.remember
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource

/**
 * Bottom sheet for batch operations on multiple selected playlists.
 * Designed to match the MultiSelectionBottomSheet (songs) in layout, animation and styling.
 *
 * @param selectedPlaylists List of selected playlists
 * @param onDismiss Callback when sheet is dismissed
 * @param onDeleteAll Delete all selected playlists
 * @param onExportAll Export all selected playlists as M3U files
 * @param onMergeAll Merge all selected playlists into one
 * @param onShareAll Share all selected playlists as ZIP file
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaylistMultiSelectionBottomSheet(
    selectedPlaylists: ImmutableList<Playlist>,
    onDismiss: () -> Unit,
    onDeleteAll: () -> Unit,
    onExportAll: () -> Unit,
    onMergeAll: () -> Unit,
    onShareAll: () -> Unit
) {
    val sheetState = rememberModalSheetState(skipPartiallyExpanded = true)
    val evenCornerRadius = 26.dp
    val buttonShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = evenCornerRadius, smoothnessAsPercentBR = 60,
        cornerRadiusBR = evenCornerRadius, smoothnessAsPercentTL = 60,
        cornerRadiusTL = evenCornerRadius, smoothnessAsPercentBL = 60,
        cornerRadiusBL = evenCornerRadius, smoothnessAsPercentTR = 60
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Box(
            modifier = Modifier
                .animateContentSize(animationSpec = tween(durationMillis = 200))
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val stackedImageSize = 66.dp
                    val stackedOverlap = 33.dp
                    val stackedCount = selectedPlaylists.take(4).size
                    val stackedWidth = if (stackedCount > 0) {
                        (stackedImageSize - stackedOverlap) * (stackedCount - 1) + stackedImageSize
                    } else 0.dp

                    StackedPlaylistCovers(
                        playlists = remember(selectedPlaylists) { selectedPlaylists.take(4).toImmutableList() },
                        modifier = Modifier
                            .height(74.dp)
                            .width(stackedWidth)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        // Inherits LocalContentColor, which is onSurface inside the sheet.
                        AutoSizingTextToFill(
                            text = stringResource(Res.string.multi_selection_playlists_count_upper, selectedPlaylists.size),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RoundedSans,
                            minFontSize = 16.sp,
                            maxFontSizeLimit = 24.sp,
                            maxLines = 2,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.multi_selection_selected),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = RoundedSans,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .heightIn(min = 66.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                shape = CircleShape,
                                onClick = {
                                    onDeleteAll()
                                    onDismiss()
                                }
                            ) {
                                Icon(
                                    ImageVector.vectorResource(R.drawable.rounded_delete_filled_24),
                                    contentDescription = stringResource(Res.string.cd_delete_all_songs)
                                )
                                Spacer(Modifier.width(6.dp))
                                TightWrapText(
                                    text = stringResource(Res.string.delete_action),
                                    modifier = Modifier.padding(end = 4.dp),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 2,
                                    lineHeight = 22.sp,
                                )
                            }

                            FilledTonalButton(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .heightIn(min = 66.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                shape = CircleShape,
                                onClick = {
                                    onExportAll()
                                    onDismiss()
                                }
                            ) {
                                Icon(
                                    ImageVector.vectorResource(R.drawable.rounded_file_download_24),
                                    contentDescription = stringResource(Res.string.cd_export_all)
                                )
                                Spacer(Modifier.width(6.dp))
                                TightWrapText(
                                    text = stringResource(Res.string.action_export),
                                    modifier = Modifier.padding(end = 4.dp),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 2,
                                    lineHeight = 22.sp,
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .heightIn(min = 66.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                shape = CircleShape,
                                onClick = {
                                    onMergeAll()
                                    onDismiss()
                                }
                            ) {
                                Icon(
                                    ImageVector.vectorResource(R.drawable.rounded_merge_24),
                                    contentDescription = stringResource(Res.string.cd_merge_all)
                                )
                                Spacer(Modifier.width(6.dp))
                                TightWrapText(
                                    text = stringResource(Res.string.action_merge),
                                    modifier = Modifier.padding(end = 4.dp),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 2,
                                    lineHeight = 22.sp,
                                )
                            }

                            FilledTonalButton(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .heightIn(min = 66.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                shape = CircleShape,
                                onClick = {
                                    onShareAll()
                                    onDismiss()
                                }
                            ) {
                                Icon(
                                    ImageVector.vectorResource(R.drawable.rounded_share_filled_24),
                                    contentDescription = stringResource(Res.string.cd_share_all)
                                )
                                Spacer(Modifier.width(6.dp))
                                TightWrapText(
                                    text = stringResource(Res.string.action_share),
                                    modifier = Modifier.padding(end = 4.dp),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 2,
                                    lineHeight = 22.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Displays stacked playlist cover images with overlap effect for the bottom sheet header.
 * Mirrors the StackedAlbumArts component from MultiSelectionBottomSheet.
 */
@Composable
private fun StackedPlaylistCovers(
    playlists: ImmutableList<Playlist>,
    modifier: Modifier = Modifier
) {
    val imageSize = 66.dp
    val overlap = 33.dp
    val borderWidth = 3.dp
    val borderColor = MaterialTheme.colorScheme.surfaceContainerLow

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        playlists.forEachIndexed { index, playlist ->
            val offsetX = index * (imageSize.value - overlap.value)

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.dp.roundToPx(), 0) }
                    .zIndex((playlists.size - index).toFloat())
                    .size(imageSize)
                    .background(borderColor, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(borderWidth)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (playlist.coverImageUri != null) {
                        AsyncImage(
                            model = playlist.coverImageUri,
                            contentDescription = playlist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else if (playlist.coverColorArgb != null) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color(playlist.coverColorArgb)),
                            contentAlignment = Alignment.Center
                        ) {
                            val contentColor = resolvePlaylistCoverContentColor(
                                playlist.coverColorArgb,
                                MaterialTheme.colorScheme
                            )
                            
                            if (playlist.coverIconName != null) {
                                Icon(
                                    imageVector = getPlaylistIconByName(playlist.coverIconName) ?: ImageVector.vectorResource(R.drawable.rounded_music_note_24),
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = playlist.name.firstOrNull()?.toString()?.uppercase() ?: "P",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = playlist.name.firstOrNull()?.toString()?.uppercase() ?: "P",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getPlaylistIconByName(name: String?): androidx.compose.ui.graphics.vector.ImageVector? {
    return when (name) {
        "MusicNote" -> ImageVector.vectorResource(R.drawable.rounded_music_note_24)
        "Headphones" -> ImageVector.vectorResource(R.drawable.rounded_headphones_filled_24)
        "Album" -> ImageVector.vectorResource(R.drawable.rounded_album_filled_24)
        "Mic" -> ImageVector.vectorResource(R.drawable.rounded_mic_external_on_filled_24)
        "Speaker" -> ImageVector.vectorResource(R.drawable.rounded_speaker_filled_24)
        "Favorite" -> ImageVector.vectorResource(R.drawable.rounded_favorite_filled_24)
        "Piano" -> ImageVector.vectorResource(R.drawable.rounded_piano_24)
        "Queue" -> ImageVector.vectorResource(R.drawable.rounded_queue_music_24)
        else -> ImageVector.vectorResource(R.drawable.rounded_music_note_24)
    }
}
