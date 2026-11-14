package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

/** Engine state of a [PlaybackController], independent of the platform player. */
enum class PlaybackEngineState { IDLE, BUFFERING, READY, ENDED }

/**
 * Platform-neutral view of the player a screen drives: transport, position and queue edits.
 * Android backs it with the Media3 `MediaController`; a desktop player implements it directly.
 *
 * Every member is safe to call while no player is connected: reads return the idle defaults
 * documented below and commands are dropped.
 */
interface PlaybackController {
    /** False until the platform player is attached and ready for commands. */
    val isConnected: Boolean
    val isPlaying: Boolean
    val playWhenReady: Boolean
    val engineState: PlaybackEngineState

    /** Id of the item the player is on, or null when the queue is empty. */
    val currentMediaId: String?

    /** Index of the current item in the queue, or [NO_INDEX]. */
    val currentIndex: Int
    val queueSize: Int
    val positionMs: Long

    /** Duration of the current item, or [UNKNOWN_DURATION]. */
    val durationMs: Long

    fun prepare()
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun seekTo(index: Int, positionMs: Long)

    fun moveQueueItem(fromIndex: Int, toIndex: Int)
    fun removeQueueItem(index: Int)
    fun clearQueue()

    companion object {
        const val NO_INDEX: Int = -1
        const val UNKNOWN_DURATION: Long = -1L
    }
}

/** Removes every queue item except the one playing; does nothing when nothing is current. */
fun PlaybackController.removeAllExceptCurrent(): Boolean {
    val current = currentIndex
    if (current == PlaybackController.NO_INDEX) return false
    for (index in queueSize - 1 downTo 0) {
        if (index != current) removeQueueItem(index)
    }
    return true
}
