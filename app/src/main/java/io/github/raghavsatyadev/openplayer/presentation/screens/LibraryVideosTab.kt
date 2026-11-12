package io.github.raghavsatyadev.openplayer.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.model.LibraryTabId
import io.github.raghavsatyadev.openplayer.data.model.StorageFilter
import io.github.raghavsatyadev.openplayer.data.video.LocalVideoRepository
import io.github.raghavsatyadev.openplayer.presentation.components.MiniPlayerHeight
import io.github.raghavsatyadev.openplayer.presentation.components.SmartImage
import coil.size.Size
import coil.compose.AsyncImagePainter
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.VideoRow
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.VideosLibraryViewModel
import io.github.raghavsatyadev.openplayer.ui.theme.RoundedSans
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

/** Local videos and the tracks they match. Tapping a matched video plays its track with video on. */
@Composable
internal fun LibraryVideosTab(
    playerViewModel: PlayerViewModel,
    bottomBarHeight: Dp,
    viewModel: VideosLibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.onPermissionResult() }

    when {
        !state.isLoaded -> Box(Modifier.fillMaxSize())

        !state.hasPermission -> VideoPermissionPrompt(
            bottomBarHeight = bottomBarHeight,
            onAllow = { permissionLauncher.launch(LocalVideoRepository.videoPermission) },
        )

        state.rows.isEmpty() -> LibraryExpressiveEmptyState(
            tabId = LibraryTabId.VIDEOS,
            storageFilter = StorageFilter.ALL,
            bottomBarHeight = bottomBarHeight,
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 4.dp,
                bottom = bottomBarHeight + MiniPlayerHeight + ListExtraBottomGap,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(state.rows, key = { it.video.id }) { row ->
                VideoListItem(
                    row = row,
                    onClick = {
                        if (row.song != null) viewModel.showVideoInPlayer()
                        playerViewModel.showAndPlaySong(row.playable, state.playables, "Videos")
                    },
                )
            }
        }
    }
}

@Composable
private fun VideoListItem(row: VideoRow, onClick: () -> Unit) {
    val shape = remember { smoothShape(20.dp) }
    val matched = row.song != null
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = remember { smoothShape(14.dp) },
                color = if (matched) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                modifier = Modifier.size(52.dp),
            ) {
                // Same cover as the Songs tab: the matched track's, else the video's own library row.
                val artUri = row.playable.albumArtUriString?.takeIf { it.isNotBlank() }
                var artFailed by remember(artUri) { mutableStateOf(false) }
                if (artUri != null && !artFailed) {
                    SmartImage(
                        model = artUri,
                        contentDescription = null,
                        targetSize = Size(VIDEO_ART_SIZE_PX, VIDEO_ART_SIZE_PX),
                        modifier = Modifier.fillMaxSize(),
                        onState = { if (it is AsyncImagePainter.State.Error) artFailed = true },
                    )
                } else Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_movie_24),
                        contentDescription = null,
                        tint = if (matched) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = row.video.displayName.substringBeforeLast('.'),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                row.song?.let { song ->
                    Text(
                        text = stringResource(R.string.video_track_line, song.title, song.displayArtist()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = formatVideoDuration(row.video.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun VideoPermissionPrompt(bottomBarHeight: Dp, onAllow: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 28.dp, end = 28.dp, bottom = bottomBarHeight + MiniPlayerHeight + 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.video_permission_title),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = RoundedSans,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.video_permission_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onAllow) { Text(stringResource(R.string.video_permission_button)) }
        }
    }
}

private fun io.github.raghavsatyadev.openplayer.data.model.Song.displayArtist(): String =
    artists.firstOrNull { it.isPrimary }?.name ?: artists.firstOrNull()?.name ?: artist

private fun formatVideoDuration(ms: Long): String {
    if (ms <= 0) return ""
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private const val VIDEO_ART_SIZE_PX = 160

private fun smoothShape(radius: Dp) = AbsoluteSmoothCornerShape(
    cornerRadiusTL = radius, smoothnessAsPercentTL = 60,
    cornerRadiusTR = radius, smoothnessAsPercentTR = 60,
    cornerRadiusBL = radius, smoothnessAsPercentBL = 60,
    cornerRadiusBR = radius, smoothnessAsPercentBR = 60,
)
