package io.github.raghavsatyadev.openplayer.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.raghavsatyadev.openplayer.data.video.VideoMatchingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class VideoSettings(
    /** Show a matching local video in the full player instead of the album art. */
    val showInPlayer: Boolean = false,
    val matchingMode: VideoMatchingMode = VideoMatchingMode.AUTO,
    /** Only use videos in the same folder as the audio file. */
    val sameFolderOnly: Boolean = false,
    /** Folders to scan for videos. Empty means every folder. */
    val includedFolders: Set<String> = emptySet(),
    /** Folders to skip. These win over [includedFolders]. */
    val excludedFolders: Set<String> = emptySet(),
    /** Add the videos in those folders to the song library as tracks. */
    val addToLibrary: Boolean = false,
)

/** Settings for local music videos. */
class VideoPreferences(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val SHOW_IN_PLAYER = booleanPreferencesKey("video_show_in_player")
        val MATCHING_MODE = stringPreferencesKey("video_matching_mode")
        val SAME_FOLDER_ONLY = booleanPreferencesKey("video_same_folder_only")
        val INCLUDED_FOLDERS = stringSetPreferencesKey("video_included_folders")
        val EXCLUDED_FOLDERS = stringSetPreferencesKey("video_excluded_folders")
        val ADD_TO_LIBRARY = booleanPreferencesKey("video_add_to_library")
    }

    val settingsFlow: Flow<VideoSettings> = dataStore.data
        .map { prefs ->
            VideoSettings(
                showInPlayer = prefs[Keys.SHOW_IN_PLAYER] ?: false,
                matchingMode = VideoMatchingMode.fromStorageKey(prefs[Keys.MATCHING_MODE]),
                sameFolderOnly = prefs[Keys.SAME_FOLDER_ONLY] ?: false,
                includedFolders = prefs[Keys.INCLUDED_FOLDERS] ?: emptySet(),
                excludedFolders = prefs[Keys.EXCLUDED_FOLDERS] ?: emptySet(),
                addToLibrary = prefs[Keys.ADD_TO_LIBRARY] ?: false,
            )
        }
        .distinctUntilChanged()

    suspend fun setShowInPlayer(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_IN_PLAYER] = show }
    }

    suspend fun setMatchingMode(mode: VideoMatchingMode) {
        dataStore.edit { it[Keys.MATCHING_MODE] = mode.storageKey }
    }

    suspend fun setSameFolderOnly(sameFolderOnly: Boolean) {
        dataStore.edit { it[Keys.SAME_FOLDER_ONLY] = sameFolderOnly }
    }

    suspend fun setFolders(included: Set<String>, excluded: Set<String>) {
        dataStore.edit {
            it[Keys.INCLUDED_FOLDERS] = included
            it[Keys.EXCLUDED_FOLDERS] = excluded
        }
    }

    suspend fun setAddToLibrary(add: Boolean) {
        dataStore.edit { it[Keys.ADD_TO_LIBRARY] = add }
    }
}
