package io.github.raghavsatyadev.openplayer.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** Settings for looking up missing song genres online. */
class OnlineGenrePreferences(
    private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)

    val wifiOnly: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, true)

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun setWifiOnly(value: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()
    }

    val changes: Flow<Unit> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(Unit)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val enabledFlow: Flow<Boolean> = changes
        .map { enabled }
        .onStart { emit(enabled) }
        .distinctUntilChanged()

    val wifiOnlyFlow: Flow<Boolean> = changes
        .map { wifiOnly }
        .onStart { emit(wifiOnly) }
        .distinctUntilChanged()

    companion object {
        private const val PREFS_NAME = "online_genres"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_WIFI_ONLY = "wifi_only"

        fun isEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false)
        }
    }
}
