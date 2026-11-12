package io.github.raghavsatyadev.openplayer.presentation.components.player

import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.video.LocalVideoRepository
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerVideoViewModel
import kotlinx.collections.immutable.ImmutableList
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource

/**
 * The full player's cover: the matching local video when one exists and video is on, otherwise
 * [artwork] (the album carousel). Swiping the video skips tracks like the carousel does.
 */
@Composable
internal fun PlayerVideoCover(
    song: Song,
    queue: ImmutableList<Song>,
    currentIndex: Int,
    onSongSelected: (Song, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerVideoViewModel = activityPlayerVideoViewModel(),
    artwork: @Composable () -> Unit,
) {
    val matched by viewModel.matchedVideo.collectAsStateWithLifecycle()
    val showInPlayer by viewModel.showInPlayer.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
    val isOwnVideo by viewModel.isOwnVideo.collectAsStateWithLifecycle()
    val inPip by viewModel.inPictureInPicture.collectAsStateWithLifecycle()
    val showVideo = (showInPlayer || isOwnVideo) && hasPermission && matched != null
    val fullscreen by viewModel.fullscreen.collectAsStateWithLifecycle()
    val videoAspect by viewModel.videoAspect.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult()
        if (granted) viewModel.setShowInPlayer(true)
    }

    if (showVideo) {
        // The video player lives only while it can be seen.
        LifecycleStartEffect(Unit) {
            viewModel.attach()
            onStopOrDispose { viewModel.detach() }
        }
    }

    val toggleButton: @Composable () -> Unit = {
        if (!isOwnVideo) CoverButton(
            iconRes = R.drawable.rounded_movie_24,
            description = stringResource(
                if (showVideo) R.string.video_show_artwork else R.string.video_show_video
            ),
            selected = showVideo,
            onClick = {
                when {
                    showVideo -> viewModel.setShowInPlayer(false)
                    !hasPermission -> permissionLauncher.launch(LocalVideoRepository.videoPermission)
                    else -> viewModel.setShowInPlayer(true)
                }
            },
        )
    }

    Box(modifier = modifier) {
        if (showVideo) {
            val player by viewModel.player.collectAsStateWithLifecycle()
            val shape = remember { coverShape() }
            // Sized to the video, so it fills the width with no bars; zoom trims any rounding gap.
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(videoAspect ?: DEFAULT_ASPECT)
                    .clip(shape)
                    .background(Color.Black)
                    .swipeToSkip(song, queue, currentIndex, onSongSelected)
            ) {
                // In fullscreen and picture-in-picture another view draws the video.
                if (!fullscreen && !inPip) {
                    VideoView(player, Modifier.fillMaxSize(), AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
                }
                Row(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp)
                ) {
                    CoverButton(
                        iconRes = R.drawable.rounded_fullscreen_24,
                        description = stringResource(Res.string.video_fullscreen),
                        onClick = { viewModel.setFullscreen(true) },
                    )
                    toggleButton()
                }
            }
        } else {
            artwork()
            // Shown only when there is a video to turn on, or video is on but needs permission.
            if (matched != null || (showInPlayer && !hasPermission)) {
                Row(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 28.dp, bottom = 12.dp)
                ) { toggleButton() }
            }
        }
    }

    LaunchedEffect(showVideo) {
        if (!showVideo) viewModel.setFullscreen(false)
    }
}

@Composable
private fun CoverButton(
    iconRes: Int,
    description: String,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    val colors = if (selected) {
        IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        )
    }
    FilledTonalIconButton(
        onClick = onClick,
        colors = colors,
        modifier = Modifier.padding(start = 8.dp).size(40.dp),
    ) {
        Icon(painterResource(iconRes), contentDescription = description, modifier = Modifier.size(22.dp))
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun VideoView(
    player: Player?,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            (android.view.LayoutInflater.from(ctx).inflate(R.layout.view_video_player, null) as PlayerView).apply {
                this.resizeMode = resizeMode
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { it.player = player },
        onRelease = { it.player = null },
    )
}

/** A horizontal swipe past a third of the width moves to the previous or next track. */
private fun Modifier.swipeToSkip(
    song: Song,
    queue: ImmutableList<Song>,
    currentIndex: Int,
    onSongSelected: (Song, Int) -> Unit,
): Modifier = pointerInput(song.id, currentIndex, queue) {
    var total = 0f
    detectHorizontalDragGestures(
        onDragStart = { total = 0f },
        onDragEnd = {
            val threshold = size.width / 3f
            val target = when {
                total <= -threshold -> currentIndex + 1
                total >= threshold -> currentIndex - 1
                else -> null
            }
            target?.let { index -> queue.getOrNull(index)?.let { onSongSelected(it, index) } }
        },
    ) { change, dragAmount ->
        change.consume()
        total += dragAmount
    }
}

private fun coverShape() = AbsoluteSmoothCornerShape(
    cornerRadiusTL = 24.dp, smoothnessAsPercentTL = 60,
    cornerRadiusTR = 24.dp, smoothnessAsPercentTR = 60,
    cornerRadiusBL = 24.dp, smoothnessAsPercentBL = 60,
    cornerRadiusBR = 24.dp, smoothnessAsPercentBR = 60,
)

private const val DEFAULT_ASPECT = 16f / 9f
