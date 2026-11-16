package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

/** Why the current item of a [PlaybackController] changed. */
enum class ItemTransitionReason { AUTO, REPEAT, SEEK, PLAYLIST_CHANGED }

/**
 * Platform-neutral player events. Every callback arrives on the main thread; all have empty
 * defaults so implementers override only what they need. Android feeds it from Media3 callbacks.
 */
interface PlaybackEventListener {
    fun onPlayerError() {}
    fun onIsPlayingChanged(isPlaying: Boolean) {}
    fun onPlayWhenReadyChanged(playWhenReady: Boolean) {}

    /** [mediaId] is the new current item's id, or null when the queue became empty. */
    fun onCurrentItemChanged(mediaId: String?, reason: ItemTransitionReason) {}
    fun onEngineStateChanged(state: PlaybackEngineState) {}
    fun onTracksChanged() {}
    fun onMetadataChanged() {}

    /** The player's own shuffle flag changed. */
    fun onNativeShuffleChanged(enabled: Boolean) {}

    /** [repeatMode] is one of the `PlaybackController.REPEAT_*` values. */
    fun onRepeatModeChanged(repeatMode: Int) {}

    /** The queue changed; [structural] is true when items were added, removed or replaced (not just refreshed). */
    fun onQueueChanged(structural: Boolean) {}

    /** The playback session asked to set the app-level shuffle flag to [enabled]. */
    fun onSessionShuffleCommand(enabled: Boolean) {}
}
