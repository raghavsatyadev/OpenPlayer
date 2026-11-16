package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import io.github.raghavsatyadev.openplayer.data.model.Song

/** Builds and attaches a playback queue: the start song first, then the rest in the background. */
interface PlaybackQueueLoader {
    /** Resolves [song]'s playable item now (may suspend on I/O) and returns a synchronous action that replaces the player's queue with just that item and plays it from [startPositionMs]. The action may be stored and run later. */
    suspend fun prepareStart(song: Song, playlistId: String?, startPositionMs: Long): () -> Unit

    /** Builds items for the rest of [songs] off the main thread, then, only if the player still holds exactly one item whose id is [startSongId], inserts them before/after it in batches on the main thread. Returns the start song's index in [songs] when inserted, else null. */
    suspend fun attachRest(songs: List<Song>, startSongId: String, playlistId: String?): Int?
}
