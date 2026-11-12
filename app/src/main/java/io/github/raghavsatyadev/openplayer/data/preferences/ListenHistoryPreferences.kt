package io.github.raghavsatyadev.openplayer.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import io.github.raghavsatyadev.openplayer.data.history.ListenCountRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** How much of a track must play before it counts as a listen, and the one-time import flag. */
@Singleton
class ListenHistoryPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val MIN_SECONDS = intPreferencesKey("listen_count_min_seconds")
        val MIN_PERCENTAGE = intPreferencesKey("listen_count_min_percentage")
        val IMPORTED_V1 = booleanPreferencesKey("listen_history_imported_v1")
    }

    val ruleFlow: Flow<ListenCountRule> = dataStore.data
        .map { prefs ->
            ListenCountRule(
                minSeconds = prefs[Keys.MIN_SECONDS] ?: ListenCountRule.DEFAULT_MIN_SECONDS,
                minPercentage = prefs[Keys.MIN_PERCENTAGE] ?: ListenCountRule.DEFAULT_MIN_PERCENTAGE
            ).sanitized()
        }
        .distinctUntilChanged()

    suspend fun currentRule(): ListenCountRule = ruleFlow.first()

    suspend fun setMinSeconds(seconds: Int) {
        dataStore.edit { it[Keys.MIN_SECONDS] = seconds.coerceIn(ListenCountRule.SECONDS_RANGE) }
    }

    suspend fun setMinPercentage(percentage: Int) {
        dataStore.edit { it[Keys.MIN_PERCENTAGE] = percentage.coerceIn(ListenCountRule.PERCENTAGE_RANGE) }
    }

    suspend fun isImported(): Boolean = dataStore.data.first()[Keys.IMPORTED_V1] ?: false

    suspend fun markImported() {
        dataStore.edit { it[Keys.IMPORTED_V1] = true }
    }
}
