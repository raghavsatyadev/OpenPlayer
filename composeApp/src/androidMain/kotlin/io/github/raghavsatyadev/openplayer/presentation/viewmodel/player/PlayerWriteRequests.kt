package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import io.github.raghavsatyadev.openplayer.data.media.CoverArtUpdate
import io.github.raghavsatyadev.openplayer.data.media.CustomMetadataChanges
import io.github.raghavsatyadev.openplayer.data.model.Lyrics
import io.github.raghavsatyadev.openplayer.data.model.Song
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal data class PendingMetadataEdit(
    val song: Song,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val composer: String,
    val genre: String,
    val lyrics: String,
    val trackNumber: Int,
    val discNumber: Int?,
    val replayGainTrackGainDb: String?,
    val replayGainAlbumGainDb: String?,
    val coverArtUpdate: CoverArtUpdate?,
    val customMetadataChanges: CustomMetadataChanges
)

internal data class PendingBatchMetadataEdit(
    val songs: List<Song>,
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val composer: String?,
    val genre: String?,
    val lyrics: String?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val replayGainTrackGainDb: String?,
    val replayGainAlbumGainDb: String?,
    val coverArtUpdate: CoverArtUpdate?
)

internal data class PendingLyricsSave(
    val song: Song,
    val lyrics: Lyrics,
    val preferSynced: Boolean
)

/**
 * Shared MediaStore write-permission request flow and the edits waiting on its result, extracted from
 * PlayerViewModel. The editors park their pending work here; [PlayerWritePermissions] retries it on the result.
 */
class PlayerWriteRequests {
    private val _writePermissionRequest = MutableSharedFlow<android.content.IntentSender>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val writePermissionRequest: SharedFlow<android.content.IntentSender> = _writePermissionRequest.asSharedFlow()

    internal suspend fun emit(intentSender: android.content.IntentSender) = _writePermissionRequest.emit(intentSender)

    internal val subscriptionCount get() = _writePermissionRequest.subscriptionCount

    internal var pendingMetadataEdit: PendingMetadataEdit? = null
    internal var pendingBatchMetadataEdit: PendingBatchMetadataEdit? = null
    internal var pendingLyricsSave: PendingLyricsSave? = null
    internal var pendingBatchGenreEdit: Pair<List<Song>, String>? = null
    /** A write request for songs left cut off by an interrupted save is waiting for its result. */
    internal var pendingInterruptedSaveRestore = false
    /** Cut-off songs already asked about, so a denied request is not repeated while this ViewModel lives. */
    internal val askedInterruptedSaveRestoreUris = mutableSetOf<String>()
    /** Counts answered write requests, so a waiting restore prompt can tell one has finished. */
    internal val answeredWriteRequests = MutableStateFlow(0)

    internal fun otherWriteRequestPending(): Boolean =
        pendingMetadataEdit != null || pendingBatchMetadataEdit != null ||
            pendingBatchGenreEdit != null || pendingLyricsSave != null
}
