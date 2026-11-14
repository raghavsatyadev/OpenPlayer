package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import io.github.raghavsatyadev.openplayer.data.model.Lyrics
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.di.DispatcherProvider
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlaybackStateHolder
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.utils.LyricsUtils
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Actions that stay in PlayerViewModel and are invoked by [PlayerLyricsSaver]. */
class PlayerLyricsSaverCallbacks(
    val loadLyricsForCurrentSong: () -> Unit,
)

/** Saves lyrics to a sidecar .lrc file (with MediaStore write permission), extracted from PlayerViewModel. */
class PlayerLyricsSaver(
    private val scope: CoroutineScope,
    private val context: android.content.Context,
    private val dispatchers: DispatcherProvider,
    private val playbackStateHolder: PlaybackStateHolder,
    private val writeRequests: PlayerWriteRequests,
    private val toast: suspend (String) -> Unit,
    private val callbacks: PlayerLyricsSaverCallbacks,
) {

    fun saveLyricsToFile(song: Song, lyrics: Lyrics, preferSynced: Boolean) {
        val lrcContent = LyricsUtils.toLrcString(lyrics, preferSynced)
        if (lrcContent.isEmpty()) {
            scope.launch { toast(getString(Res.string.no_lyrics_to_save)) }
            return
        }

        val songFile = java.io.File(song.path)
        val lrcFile = java.io.File(songFile.parentFile, "${songFile.nameWithoutExtension}.lrc")

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && lrcFile.exists() && !lrcFile.canWrite()) {
            val uri = io.github.raghavsatyadev.openplayer.utils.MediaStorePermissionHelper.getMediaStoreUri(context, lrcFile.absolutePath)
            if (uri != null) {
                val intentSender = io.github.raghavsatyadev.openplayer.utils.MediaStorePermissionHelper.createWriteRequestIntentSender(context, listOf(uri))
                if (intentSender != null) {
                    writeRequests.pendingLyricsSave = PendingLyricsSave(song, lyrics, preferSynced)
                    scope.launch { writeRequests.emit(intentSender) }
                    return
                }
            }
        }

        performLyricsSave(song, lyrics, preferSynced)
    }

    internal fun performLyricsSave(song: Song, lyrics: Lyrics, preferSynced: Boolean) {
        scope.launch(dispatchers.io) {
            try {
                val songFile = java.io.File(song.path)
                val lrcFile = java.io.File(songFile.parentFile, "${songFile.nameWithoutExtension}.lrc")
                val lrcContent = LyricsUtils.toLrcString(lyrics, preferSynced)

                lrcFile.writeText(lrcContent, Charsets.UTF_8)
                toast(getString(Res.string.lyrics_saved_successfully))
                
                if (playbackStateHolder.stablePlayerState.value.currentSong?.id == song.id) {
                    callbacks.loadLyricsForCurrentSong()
                }
            } catch (e: Exception) {
                Logger.e(e) { "Failed to save lyrics to file" }
                toast(getString(Res.string.lyrics_save_failed))
            }
        }
    }
}
