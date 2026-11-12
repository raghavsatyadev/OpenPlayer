package io.github.raghavsatyadev.openplayer.data.service.tile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import io.github.raghavsatyadev.openplayer.MainActivity
import io.github.raghavsatyadev.openplayer.MainActivityIntentContract
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.model.MusicFolder
import io.github.raghavsatyadev.openplayer.data.preferences.PlaylistPreferencesRepository
import io.github.raghavsatyadev.openplayer.data.preferences.UserPreferencesRepository
import io.github.raghavsatyadev.openplayer.data.repository.MusicRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Quick Settings tile that resumes the most recently played playlist.
 * Reads the last playlist ID from DataStore and fires ACTION_OPEN_PLAYLIST to MainActivity.
 * Works whether the app is open or not.
 *
 * Uses coroutines instead of runBlocking to avoid blocking the binder thread
 * (which could cause ANR when the user opens the Quick Settings panel).
 */
@RequiresApi(Build.VERSION_CODES.N)
class LastPlaylistTileService : TileService(), KoinComponent {

    companion object {
        private const val FOLDER_PLAYLIST_PREFIX = "folder_playlist:"
        private const val REQUEST_CODE_LAST_PLAYLIST = 1002
    }

    private val prefsRepo: UserPreferencesRepository by inject()
    private val playlistRepo: PlaylistPreferencesRepository by inject()
    private val musicRepo: MusicRepository by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        serviceScope.launch(Dispatchers.IO) {
            val lastPlaylistId = resolveLaunchablePlaylistId()
            withContext(Dispatchers.Main) {
                qsTile?.apply {
                    state = if (lastPlaylistId != null) Tile.STATE_INACTIVE
                            else Tile.STATE_UNAVAILABLE
                    updateTile()
                }
            }
        }
    }

    override fun onClick() {
        serviceScope.launch(Dispatchers.IO) {
            val playlistId = resolveLaunchablePlaylistId()
            withContext(Dispatchers.Main) {
                if (playlistId == null) {
                    Toast.makeText(
                        this@LastPlaylistTileService,
                        R.string.tile_last_playlist_unavailable,
                        Toast.LENGTH_SHORT
                    ).show()
                    qsTile?.apply {
                        state = Tile.STATE_UNAVAILABLE
                        updateTile()
                    }
                    return@withContext
                }

                val intent = Intent(this@LastPlaylistTileService, MainActivity::class.java).apply {
                    setPackage(packageName)
                    action = MainActivityIntentContract.ACTION_OPEN_PLAYLIST
                    putExtra(MainActivityIntentContract.EXTRA_PLAYLIST_ID, playlistId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivityAndCollapseCompat(
                    intent = intent,
                    requestCode = REQUEST_CODE_LAST_PLAYLIST
                )
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun resolveLaunchablePlaylistId(): String? {
        val storedPlaylistId = prefsRepo.lastPlaylistIdFlow.first() ?: return null
        val isValid = if (storedPlaylistId.startsWith(FOLDER_PLAYLIST_PREFIX)) {
            hasExistingFolderPlaylist(storedPlaylistId)
        } else {
            playlistRepo.getPlaylistsOnce().any { playlist -> playlist.id == storedPlaylistId }
        }

        if (isValid) {
            return storedPlaylistId
        }

        prefsRepo.clearLastPlaylist()
        return null
    }

    private suspend fun hasExistingFolderPlaylist(playlistId: String): Boolean {
        val folderPath = Uri.decode(playlistId.removePrefix(FOLDER_PLAYLIST_PREFIX))
        return findFolder(folderPath, musicRepo.getMusicFolders().first()) != null
    }

    private fun findFolder(targetPath: String, folders: List<MusicFolder>): MusicFolder? {
        val queue = ArrayDeque(folders)
        while (queue.isNotEmpty()) {
            val folder = queue.removeFirst()
            if (folder.path == targetPath) {
                return folder
            }
            folder.subFolders.forEach(queue::addLast)
        }
        return null
    }
}
