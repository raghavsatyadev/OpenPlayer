package io.github.raghavsatyadev.openplayer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** The app's main settings store (file `datastore/settings.preferences_pb`); Koin hands it to [UserPreferencesRepository]. */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Synchronous read of the folder-cover-art opt-in.
 *
 * [io.github.raghavsatyadev.openplayer.utils.AlbumArtUtils] resolves artwork from a Coil fetcher
 * and a ContentProvider, both off the main thread, and it cannot proceed without knowing
 * this value: resolving under a wrong assumption persists a cache entry or a "no art"
 * marker that later reads then trust. The mirror is normally pushed in long before any
 * artwork is requested, so this only runs if a very early request wins that race.
 */
fun readUseFolderAlbumArtBlocking(context: Context): Boolean = runBlocking {
    runCatching {
        context.applicationContext.dataStore.data.first()[booleanPreferencesKey("use_folder_album_art")]
    }.getOrNull() ?: false
}
