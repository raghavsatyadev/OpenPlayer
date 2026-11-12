package io.github.raghavsatyadev.openplayer.presentation.components.player

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import io.github.raghavsatyadev.openplayer.data.video.LocalVideo
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerVideoViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Shrinks the app to a corner video window when the user leaves it while a video plays.
 * The music service keeps playing the sound, and the system's window controls use its
 * media session.
 */
class VideoPictureInPicture(
    private val activity: ComponentActivity,
    private val videoViewModel: PlayerVideoViewModel,
    private val isPlaying: Flow<Boolean>,
) {
    private val supported = activity.packageManager
        .hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    /** The video the window would show, or null when it should not open. */
    private var pipVideo: LocalVideo? = null

    fun start() {
        if (!supported) return
        activity.lifecycleScope.launch {
            combine(videoViewModel.videoOnScreen, isPlaying) { video, playing -> video?.takeIf { playing } }
                .distinctUntilChanged()
                .collect { video ->
                    pipVideo = video
                    runCatching { activity.setPictureInPictureParams(paramsFor(video)) }
                }
        }
    }

    /** Android 11 has no auto-enter, so open the window by hand when the user leaves. */
    fun onUserLeaveHint() {
        if (!supported || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
        val video = pipVideo ?: return
        runCatching { activity.enterPictureInPictureMode(paramsFor(video)) }
    }

    fun onModeChanged(inPip: Boolean) = videoViewModel.setInPictureInPicture(inPip)

    private fun paramsFor(video: LocalVideo?): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder().setAspectRatio(aspectOf(video))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(video != null)
            // Video content: let the system crossfade rather than stretch the frame.
            builder.setSeamlessResizeEnabled(false)
        }
        return builder.build()
    }

    private fun aspectOf(video: LocalVideo?): Rational {
        val w = video?.width ?: 0
        val h = video?.height ?: 0
        if (w <= 0 || h <= 0) return Rational(16, 9)
        // The system allows ratios between 1:2.39 and 2.39:1; stay just inside after rounding.
        val ratio = (w.toFloat() / h).coerceIn(MIN_RATIO, MAX_RATIO)
        return Rational((ratio * 1000).toInt(), 1000)
    }

    private companion object {
        const val MAX_RATIO = 2.38f
        const val MIN_RATIO = 1f / 2.38f
    }
}
