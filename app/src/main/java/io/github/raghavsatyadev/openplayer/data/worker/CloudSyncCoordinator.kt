package io.github.raghavsatyadev.openplayer.data.worker

import io.github.raghavsatyadev.openplayer.data.jellyfin.JellyfinRepository
import io.github.raghavsatyadev.openplayer.data.navidrome.NavidromeRepository
import javax.inject.Inject
import javax.inject.Singleton
import co.touchlab.kermit.Logger

@Singleton
class CloudSyncCoordinator @Inject constructor(
    private val navidromeRepository: NavidromeRepository,
    private val jellyfinRepository: JellyfinRepository
) {
    val needsActiveCloudSync: Boolean
        get() = navidromeRepository.isLoggedIn &&
            (System.currentTimeMillis() - navidromeRepository.lastFullSyncTime >= NavidromeRepository.SYNC_THRESHOLD_MS)

    suspend fun syncUnifiedCloudLibraries() {
        syncNavidromeData()
        syncJellyfinCache()
    }

    private suspend fun syncNavidromeData() {
        if (!navidromeRepository.isLoggedIn) return

        val lastSync = navidromeRepository.lastFullSyncTime
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastSync < NavidromeRepository.SYNC_THRESHOLD_MS) {
            Logger.withTag(TAG).d { "Skipping Navidrome server sync - last sync was recent." }
            navidromeRepository.syncUnifiedLibrarySongsFromNavidrome()
            return
        }

        Logger.withTag(TAG).i { "Syncing Navidrome data from server..." }
        try {
            val result = navidromeRepository.syncAllPlaylistsAndSongs()
            result.fold(
                onSuccess = { summary ->
                    Logger.withTag(TAG).i { "Navidrome sync complete: ${summary.playlistCount} playlists, " +
                            "${summary.syncedSongCount} songs synced (${summary.failedPlaylistCount} failed)" }
                },
                onFailure = { error ->
                    Logger.withTag(TAG).w(error) { "Navidrome server sync failed, falling back to local cache sync" }
                    navidromeRepository.syncUnifiedLibrarySongsFromNavidrome()
                }
            )
        } catch (error: Exception) {
            Logger.withTag(TAG).e(error) { "Failed to sync Navidrome data" }
        }
    }

    private suspend fun syncJellyfinCache() {
        if (!jellyfinRepository.isLoggedIn) return

        try {
            jellyfinRepository.syncUnifiedLibrarySongsFromJellyfinIfChanged()
        } catch (error: Exception) {
            Logger.withTag(TAG).e(error) { "Failed to sync Jellyfin cached data" }
        }
    }

    private companion object {
        private const val TAG = "CloudSyncCoordinator"
    }
}
