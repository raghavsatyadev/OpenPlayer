package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val EXTERNAL_SONG_ID_PREFIX = "external:"

/**
 * Favorite toggling for the player, extracted from PlayerViewModel. Songs opened from outside
 * the library carry a content or file URI instead of a MediaStore id, so [resolveSongId] maps
 * them back to the library id the favorites table stores.
 */
class PlayerFavoriteActions(
    private val scope: CoroutineScope,
    private val musicRepository: MusicRepository,
    private val favoriteSongIds: StateFlow<Set<String>>,
    private val currentSong: () -> Song?,
) {
    suspend fun setFavoriteStatus(songId: String, isFavorite: Boolean) {
        musicRepository.setFavoriteStatus(songId, isFavorite)
    }

    fun toggleFavorite() {
        val song = currentSong() ?: return
        scope.launch {
            val favoriteSongId = resolveSongId(song) ?: return@launch
            val currentlyFavorite = favoriteSongIds.value.contains(favoriteSongId)
            setFavoriteStatus(favoriteSongId, !currentlyFavorite)
        }
    }

    fun toggleFavoriteSpecificSong(song: Song, removing: Boolean = false) {
        scope.launch {
            val favoriteSongId = resolveSongId(song) ?: return@launch
            val currentlyFavorite = favoriteSongIds.value.contains(favoriteSongId)
            val targetFavoriteState = if (removing) false else !currentlyFavorite
            setFavoriteStatus(favoriteSongId, targetFavoriteState)
        }
    }

    suspend fun resolveSongId(song: Song?): String? {
        song ?: return null
        if (song.id.toLongOrNull() != null) {
            return song.id
        }

        val contentUriCandidates = buildList {
            if (song.id.startsWith(EXTERNAL_SONG_ID_PREFIX)) {
                add(song.id.removePrefix(EXTERNAL_SONG_ID_PREFIX))
            }
            add(song.contentUriString)
        }.filter { it.isNotBlank() }.distinct()

        for (candidate in contentUriCandidates) {
            musicRepository.getSongIdByContentUri(candidate)?.let { return it.toString() }
            parseMediaStoreAudioId(candidate)?.let { return it.toString() }
        }

        val pathCandidates = buildList {
            add(song.path)
            contentUriCandidates.forEach { candidate ->
                parseFileUriPath(candidate)?.let(::add)
            }
        }.filter { it.isNotBlank() }.distinct()

        for (candidate in pathCandidates) {
            musicRepository.getSongByPath(candidate)?.id?.takeIf { it.toLongOrNull() != null }?.let {
                return it
            }
        }

        return null
    }

    private fun parseMediaStoreAudioId(uriString: String): Long? {
        val normalizedUri = uriString.substringBefore('?').substringBefore('#')
        if (
            !normalizedUri.startsWith("content://media/", ignoreCase = true) ||
            !normalizedUri.contains("/audio/media/", ignoreCase = true)
        ) {
            return null
        }

        return normalizedUri.substringAfterLast('/').toLongOrNull()?.takeIf { it > 0L }
    }

    private fun parseFileUriPath(uriString: String): String? {
        if (!uriString.startsWith("file:", ignoreCase = true)) return null
        val rest = uriString.substringBefore('#').substringBefore('?').substring("file:".length)
        val path = if (rest.startsWith("//")) "/" + rest.removePrefix("//").substringAfter('/', "") else rest
        val decoded = runCatching { java.net.URLDecoder.decode(path.replace("+", "%2B"), "UTF-8") }.getOrNull() ?: return null
        return decoded.takeIf { it.isNotBlank() }
    }
}
