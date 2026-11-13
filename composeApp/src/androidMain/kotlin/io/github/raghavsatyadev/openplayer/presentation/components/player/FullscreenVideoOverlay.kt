package io.github.raghavsatyadev.openplayer.presentation.components.player

import android.content.pm.ActivityInfo
import io.github.raghavsatyadev.openplayer.presentation.utils.AppBackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.AspectRatioFrameLayout
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerVideoViewModel
import io.github.raghavsatyadev.openplayer.utils.PhoneOrientationPolicy
import kotlinx.coroutines.delay
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource

/**
 * The video over the whole app: immersive, edge to edge, turned to landscape for wide videos, with
 * playback controls that hide after a few seconds and come back on a tap. Back leaves it.
 *
 * It sits at the activity root, not in the player sheet, so turning the screen never removes it.
 */
@Composable
fun FullscreenVideoOverlay(viewModel: PlayerVideoViewModel = activityPlayerVideoViewModel()) {
    val fullscreen by viewModel.fullscreen.collectAsStateWithLifecycle()
    val inPip by viewModel.inPictureInPicture.collectAsStateWithLifecycle()
    val video by viewModel.videoOnScreen.collectAsStateWithLifecycle()
    if (!fullscreen || inPip || video == null) return

    val aspect by viewModel.videoAspect.collectAsStateWithLifecycle()
    val landscape = (aspect ?: 1f) >= 1f
    ImmersiveWindow(landscape)
    AppBackHandler { viewModel.setFullscreen(false) }
    LifecycleStartEffect(Unit) {
        viewModel.attach()
        onStopOrDispose { viewModel.detach() }
    }

    val player by viewModel.player.collectAsStateWithLifecycle()
    var controlsVisible by remember { mutableStateOf(true) }
    var lastTouch by remember { mutableLongStateOf(0L) }
    LaunchedEffect(controlsVisible, lastTouch) {
        if (controlsVisible) {
            delay(CONTROLS_HIDE_MS)
            controlsVisible = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                controlsVisible = !controlsVisible
                lastTouch = System.currentTimeMillis()
            }
    ) {
        VideoView(player, Modifier.fillMaxSize(), AspectRatioFrameLayout.RESIZE_MODE_FIT)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            FullscreenControls(
                viewModel = viewModel,
                onInteract = { lastTouch = System.currentTimeMillis() },
                onExit = { viewModel.setFullscreen(false) },
            )
        }
    }
}

@Composable
private fun FullscreenControls(
    viewModel: PlayerVideoViewModel,
    onInteract: () -> Unit,
    onExit: () -> Unit,
) {
    val state by viewModel.playerState.collectAsStateWithLifecycle()
    val song = state.currentSong
    val audio = viewModel.audioPlayer

    var position by remember { mutableLongStateOf(audio?.currentPosition ?: 0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(audio) {
        while (true) {
            if (!dragging) position = audio?.currentPosition ?: 0L
            delay(POSITION_TICK_MS)
        }
    }
    val duration = (audio?.duration?.takeIf { it > 0 } ?: state.totalDuration).coerceAtLeast(1L)

    val scrim = Brush.verticalGradient(
        0f to Color.Black.copy(alpha = 0.6f),
        0.3f to Color.Transparent,
        0.6f to Color.Transparent,
        1f to Color.Black.copy(alpha = 0.7f),
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(scrim)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = song?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song?.displayArtist.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OverlayIcon(R.drawable.rounded_fullscreen_exit_24, stringResource(Res.string.video_exit_fullscreen), onExit)
        }

        Row(
            Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OverlayIcon(R.drawable.rounded_skip_previous_24, stringResource(Res.string.video_previous)) {
                onInteract(); audio?.seekToPrevious()
            }
            FilledIconButton(
                onClick = {
                    onInteract()
                    audio?.let { if (it.isPlaying) it.pause() else it.play() }
                },
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.9f),
                    contentColor = Color.Black,
                ),
            ) {
                Icon(
                    painterResource(if (state.isPlaying) R.drawable.rounded_pause_24 else R.drawable.rounded_play_arrow_24),
                    contentDescription = stringResource(if (state.isPlaying) R.string.video_pause else R.string.video_play),
                    modifier = Modifier.size(36.dp),
                )
            }
            OverlayIcon(R.drawable.rounded_skip_next_24, stringResource(Res.string.video_next)) {
                onInteract(); audio?.seekToNext()
            }
        }

        Column(Modifier.fillMaxWidth().align(Alignment.BottomStart)) {
            Slider(
                value = if (dragging) dragValue else (position.toFloat() / duration).coerceIn(0f, 1f),
                onValueChange = {
                    onInteract()
                    dragging = true
                    dragValue = it
                },
                onValueChangeFinished = {
                    val target = (dragValue * duration).toLong()
                    audio?.seekTo(target)
                    position = target
                    dragging = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
            )
            Row(Modifier.fillMaxWidth()) {
                val shown = if (dragging) (dragValue * duration).toLong() else position
                Text(formatTime(shown), color = Color.White, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text(formatTime(duration), color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun OverlayIcon(iconRes: Int, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.35f),
            contentColor = Color.White,
        ),
    ) {
        Icon(painterResource(iconRes), contentDescription = description, modifier = Modifier.size(30.dp))
    }
}

/** Hides the system bars and turns the screen for as long as this is shown. */
@Composable
private fun ImmersiveWindow(landscape: Boolean) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity, landscape) {
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        PhoneOrientationPolicy.override = if (landscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        PhoneOrientationPolicy.apply(activity)
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            PhoneOrientationPolicy.override = null
            PhoneOrientationPolicy.apply(activity)
        }
    }
}

private val io.github.raghavsatyadev.openplayer.data.model.Song.displayArtist: String
    get() = artists.firstOrNull { it.isPrimary }?.name ?: artists.firstOrNull()?.name ?: artist

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private const val CONTROLS_HIDE_MS = 3_500L
private const val POSITION_TICK_MS = 250L
