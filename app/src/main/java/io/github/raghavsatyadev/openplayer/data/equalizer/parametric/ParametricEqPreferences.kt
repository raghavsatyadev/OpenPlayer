package io.github.raghavsatyadev.openplayer.data.equalizer.parametric

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Stores the parametric EQ switch and profile in the app's preferences DataStore (no database). */
class ParametricEqPreferences(
    private val dataStore: DataStore<Preferences>,
) {
    val enabledFlow: Flow<Boolean> = dataStore.data.map { it[ENABLED] ?: false }

    val profileFlow: Flow<ParametricEqProfile> = dataStore.data.map {
        ParametricProfileCodec.decode(it[PROFILE_JSON]) ?: ParametricEqProfile.EMPTY
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[ENABLED] = enabled }
    }

    suspend fun setProfile(profile: ParametricEqProfile) {
        dataStore.edit { it[PROFILE_JSON] = ParametricProfileCodec.encode(profile) }
    }

    companion object {
        const val KEY_ENABLED = "parametric_eq_enabled"
        const val KEY_PROFILE_JSON = "parametric_eq_profile_json"
        private val ENABLED = booleanPreferencesKey(KEY_ENABLED)
        private val PROFILE_JSON = stringPreferencesKey(KEY_PROFILE_JSON)
    }
}
