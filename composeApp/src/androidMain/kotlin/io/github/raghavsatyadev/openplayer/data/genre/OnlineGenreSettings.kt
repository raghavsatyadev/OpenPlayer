package io.github.raghavsatyadev.openplayer.data.genre

import io.github.raghavsatyadev.openplayer.data.database.GenreFillDao
import io.github.raghavsatyadev.openplayer.data.preferences.OnlineGenrePreferences

/** What the "Find missing genres online" switches do besides saving the setting. */
class OnlineGenreSettings(
    private val preferences: OnlineGenrePreferences,
    private val dao: GenreFillDao,
    private val lookupScheduler: GenreLookupScheduler,
) {
    val enabled get() = preferences.enabled
    val wifiOnly get() = preferences.wifiOnly
    val enabledFlow = preferences.enabledFlow
    val wifiOnlyFlow = preferences.wifiOnlyFlow

    suspend fun setEnabled(enabled: Boolean) {
        val wasEnabled = preferences.enabled
        preferences.setEnabled(enabled)
        when {
            enabled && !wasEnabled -> {
                // Turned back on: give albums that found nothing before another chance.
                dao.clearCachedMisses()
                lookupScheduler.enqueue(preferences.wifiOnly, replace = true)
            }
            !enabled -> lookupScheduler.cancel()
        }
    }

    fun setWifiOnly(wifiOnly: Boolean) {
        preferences.setWifiOnly(wifiOnly)
        // The network requirement is fixed when work is queued, so queue it again.
        if (preferences.enabled) {
            lookupScheduler.enqueue(wifiOnly, replace = true)
        }
    }
}
