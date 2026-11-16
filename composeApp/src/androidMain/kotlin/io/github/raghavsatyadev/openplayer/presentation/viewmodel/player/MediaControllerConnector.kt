package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import io.github.raghavsatyadev.openplayer.data.media.MediaControllerFactory
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlaybackStateHolder

/**
 * Owns the Media3 [MediaController] connection of one PlayerViewModel: the async build, the
 * controller reference, the Player listener and the release. Everything else talks to the
 * controller through [createPlaybackController] or, until its callers are migrated, [rawController].
 */
class MediaControllerConnector(
    private val context: Context,
    private val sessionToken: SessionToken,
    private val factory: MediaControllerFactory,
    private val playbackStateHolder: PlaybackStateHolder,
) {
    private var mediaController: MediaController? = null
    private var adapter: Media3PlaybackEventAdapter? = null
    private var playerListener: Player.Listener? = null
    private var future: ListenableFuture<MediaController>? = null

    /** True once the async connection delivered a controller (even if it later disconnected). */
    val isAttached: Boolean get() = mediaController != null

    /** [PlaybackController] over whichever controller is current; [resolvedMediaItem] builds the playable item for a song. */
    fun createPlaybackController(
        resolvedMediaItem: suspend (Song, String?) -> MediaItem,
    ): PlaybackController = MediaControllerPlaybackController(
        controller = { mediaController },
        resolvedMediaItem = resolvedMediaItem,
    )

    /**
     * Starts the async connection. The session's custom commands reach [listener] from now on
     * ([PlaybackEventListener.onSessionShuffleCommand]); player events only after [attachPlayerEvents].
     */
    fun connect(listener: PlaybackEventListener) {
        val newAdapter = Media3PlaybackEventAdapter(listener)
        adapter = newAdapter
        future = factory.create(context, sessionToken, newAdapter.controllerListener)
    }

    /**
     * Runs on the main thread once [connect]'s future completes: stores the controller, hands it to
     * [PlaybackStateHolder], then calls [onConnected]. Any exception, from the future or from
     * [onConnected], goes to [onError].
     */
    fun whenConnected(onConnected: () -> Unit, onError: (Exception) -> Unit) {
        val pending = checkNotNull(future) { "connect() must be called first" }
        pending.addListener({
            try {
                val controller = pending.get()
                mediaController = controller
                playbackStateHolder.setMediaController(controller)
                onConnected()
            } catch (e: Exception) {
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Starts delivering player events to the listener given to [connect] (replacing a previous
     * attachment). Call after the initial state sync so it is not interleaved with events.
     */
    fun attachPlayerEvents() {
        val controller = mediaController ?: return
        val listener = adapter?.playerListener ?: return
        playerListener?.let(controller::removeListener)
        playerListener = listener
        controller.addListener(listener)
    }

    /** Same order as before: remove listener, clear holder, release controller, drop reference, cancel the future. */
    fun release() {
        val controllerToRelease = mediaController
        playerListener?.let { listener ->
            controllerToRelease?.removeListener(listener)
            playerListener = null
        }
        playbackStateHolder.clearMediaController(controllerToRelease)
        controllerToRelease?.release()
        mediaController = null
        future?.cancel(true)
    }

    /**
     * TEMPORARY: the raw Media3 controller for code that still needs MediaItem/Timeline/Tracks or
     * session custom commands. Remove as those callers move onto [PlaybackController].
     */
    fun rawController(): MediaController? = mediaController
}
