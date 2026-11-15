package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import io.github.raghavsatyadev.openplayer.data.model.Song

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

    /** One of [REPEAT_OFF], [REPEAT_ONE], [REPEAT_ALL]; reads [REPEAT_OFF] and drops writes when disconnected. */
    var repeatMode: Int

    /** Native shuffle flag of the player; false and ignored on write when disconnected. */
    var shuffleEnabled: Boolean

    /** Id of the queue item at [index], or null when out of range or disconnected. */
    fun mediaIdAt(index: Int): String?

    fun prepare()
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun seekTo(index: Int, positionMs: Long)

    /**
     * Replaces the queue with [song] (tagged with [playlistId]), prepares and plays it from
     * [startPositionMs] (the item's default start when null). When [resumeIfCurrent] is true and [song] is already the current item,
     * nothing is replaced and playback is resumed if paused. Dropped when disconnected.
     */
    suspend fun loadAndPlay(
        song: Song,
        playlistId: String? = null,
        startPositionMs: Long? = null,
        resumeIfCurrent: Boolean = false,
    )

    /** Tells the playback session the app-level shuffle flag changed. Dropped when disconnected. */
    fun syncShuffleState(enabled: Boolean)

    /** Asks the playback session to repeat the current song [times] more times (caller clamps the range). Dropped when disconnected. */
    fun startRepeatN(times: Int)

    /** Cancels a running repeat-N. Dropped when disconnected. */
    fun cancelRepeatN()

    fun moveQueueItem(fromIndex: Int, toIndex: Int)
    fun removeQueueItem(index: Int)
    fun clearQueue()

    companion object {
        const val NO_INDEX: Int = -1
        const val UNKNOWN_DURATION: Long = -1L

        /** Same numeric values as Media3 `Player.REPEAT_MODE_*`, which are persisted and used across the app. */
        const val REPEAT_OFF: Int = 0
        const val REPEAT_ONE: Int = 1
        const val REPEAT_ALL: Int = 2
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
