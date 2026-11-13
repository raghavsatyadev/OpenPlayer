package io.github.raghavsatyadev.openplayer.data.backup.module

import co.touchlab.kermit.Logger
import io.github.raghavsatyadev.openplayer.data.model.Playlist
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
internal object PlaylistsBackupArtwork {
    private const val TAG = "PlaylistsBackupArtwork"

    fun readFileAsBase64(path: String): String? {
        return try {
            val file = File(path)
            if (!file.exists() || file.length() == 0L) return null
            val bytes = file.readBytes()
            Base64.Default.encode(bytes)
        } catch (e: Exception) {
            Logger.withTag(TAG).w(e) { "Failed to read cover image: $path" }
            null
        }
    }

    fun restoreCoverImages(
        playlists: List<Playlist>,
        coverImages: Map<String, String>,
        filesDir: File,
    ): List<Playlist> {
        return playlists.map { playlist ->
            val base64 = coverImages[playlist.id] ?: return@map playlist
            try {
                val bytes = Base64.Default.decode(base64)
                val fileName = "playlist_cover_${playlist.id}.jpg"
                val file = File(filesDir, fileName)
                file.writeBytes(bytes)
                playlist.copy(coverImageUri = file.absolutePath)
            } catch (e: Exception) {
                Logger.withTag(TAG).w(e) { "Failed to restore cover image for playlist ${playlist.id}" }
                playlist.copy(coverImageUri = null)
            }
        }
    }
}
