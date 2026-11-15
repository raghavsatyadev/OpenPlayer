package io.github.raghavsatyadev.openplayer.data.backup.module

import co.touchlab.kermit.Logger
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import io.github.raghavsatyadev.openplayer.data.backup.model.BackupSection
import io.github.raghavsatyadev.openplayer.data.database.MusicDao
import io.github.raghavsatyadev.openplayer.data.model.Playlist
import io.github.raghavsatyadev.openplayer.data.model.SmartPlaylistRule
import io.github.raghavsatyadev.openplayer.data.model.SortOption
import io.github.raghavsatyadev.openplayer.data.model.fromPlaylistSource
import io.github.raghavsatyadev.openplayer.data.model.isSmartPlaylistSource
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleCodec
import io.github.raghavsatyadev.openplayer.data.preferences.PlaylistPreferencesRepository
import io.github.raghavsatyadev.openplayer.data.preferences.PreferenceBackupEntry
import io.github.raghavsatyadev.openplayer.data.preferences.UserPreferencesRepository
import io.github.raghavsatyadev.openplayer.data.worker.SyncManager
import io.github.raghavsatyadev.openplayer.platform.AppDirectories
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Playlists and their ordering preferences.
 *
 * Song ids are matched against the current library on restore (see [BackupSongResolver]). A
 * restore made before the library scan has indexed the songs can't match them yet, so the songs
 * left unmatched are kept in [PENDING_RESTORE_FILE_NAME] and added to the restored playlists as
 * later library syncs find them, until every song the backup has metadata for resolves. Songs
 * it has none for aren't waited on (see [PlaylistsBackupPendingRestore.resolvableSongIds]).
 */
class PlaylistsModuleHandler(
    private val appDirectories: AppDirectories,
    private val playlistPreferencesRepository: PlaylistPreferencesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val musicDao: MusicDao,
    private val songResolver: BackupSongResolver,
    private val gson: Gson,
    private val syncManagerProvider: () -> SyncManager,
    private val appScope: CoroutineScope
) : BackupModuleHandler {

    private val pendingRestoreMutex = Mutex()
    private val pendingRestoreFile: File
        get() = File(appDirectories.filesDir, PENDING_RESTORE_FILE_NAME)

    init {
        appScope.launch { retryPendingWhenIdle() }
    }

    override val section = BackupSection.PLAYLISTS

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val allPlaylists = playlistPreferencesRepository.getPlaylistsOnce()

        val playlists = allPlaylists.filter { isBackedUpPlaylistSource(it.source) }

        val cloudSongIds = buildCloudSongIdSet()

        val filteredPlaylists = playlists.map { playlist ->
            playlist.copy(songIds = playlist.songIds.filter { id -> id !in cloudSongIds })
        }
        val songMetadata = songResolver.metadataFor(filteredPlaylists.flatMap { it.songIds })

        val coverImages = mutableMapOf<String, String>()
        filteredPlaylists.forEach { playlist ->
            val uri = playlist.coverImageUri ?: return@forEach
            PlaylistsBackupArtwork.readFileAsBase64(uri)?.let { coverImages[playlist.id] = it }
        }

        val payload = PlaylistsBackupPayload(
            playlists = filteredPlaylists,
            playlistSongOrderModes = playlistPreferencesRepository.playlistSongOrderModesFlow.first(),
            playlistsSortOption = playlistPreferencesRepository.playlistsSortOptionFlow.first(),
            songMetadata = songMetadata.ifEmpty { null },
            coverImages = coverImages.ifEmpty { null }
        )
        gson.toJson(payload)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        val playlists = playlistPreferencesRepository.getPlaylistsOnce()
            .count { isBackedUpPlaylistSource(it.source) }
        val orderModes = playlistPreferencesRepository.playlistSongOrderModesFlow.first()
        val sortOption = playlistPreferencesRepository.playlistsSortOptionFlow.first()
        playlists + orderModes.size + if (sortOption.isNotBlank()) 1 else 0
    }

    override suspend fun snapshot(): String = withContext(Dispatchers.IO) {
        val payload = PlaylistsBackupPayload(
            playlists = playlistPreferencesRepository.getPlaylistsOnce(),
            playlistSongOrderModes = playlistPreferencesRepository.playlistSongOrderModesFlow.first(),
            playlistsSortOption = playlistPreferencesRepository.playlistsSortOptionFlow.first()
        )
        gson.toJson(payload)
    }

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val element = JsonParser.parseString(payload)
        if (element.isJsonArray) {
            restoreLegacyPreferenceEntries(payload)
            return@withContext
        }

        val parsed = runCatching {
            gson.fromJson(payload, PlaylistsBackupPayload::class.java)
        }.getOrElse { e ->
            throw IllegalStateException("Playlists payload could not be parsed: ${e.message}", e)
        } ?: throw IllegalStateException("Playlists payload could not be parsed: empty JSON document")

        val backupPlaylists = parsed.playlists.orEmpty()
        val songMetadata = parsed.songMetadata
        val coverImages = parsed.coverImages

        // Held from the library snapshot through the playlist write, so a retry after a sync that
        // finishes meanwhile runs either before this restore or after its playlists are saved.
        // In between, it would act on the old playlists and drop the new pending restore.
        pendingRestoreMutex.withLock {
            val resolution = if (songMetadata != null && songMetadata.isNotEmpty()) {
                resolvePlaylists(backupPlaylists, songMetadata)
            } else {
                PlaylistsResolution(backupPlaylists, resolvedSongIds = emptyMap())
            }
            val pending = songMetadata?.let {
                PlaylistsBackupPendingRestore.pendingRestoreOf(backupPlaylists, it, resolution.unresolvedSongIds)
            }
            // An earlier restore's pending file left in place would fill these playlists in
            // from the other backup.
            if (!writePendingRestore(pending)) writePendingRestore(null)

            val finalPlaylists = if (coverImages != null && coverImages.isNotEmpty()) {
                PlaylistsBackupArtwork.restoreCoverImages(resolution.playlists, coverImages, appDirectories.filesDir)
            } else {
                resolution.playlists
            }

            playlistPreferencesRepository.replaceAllPlaylists(finalPlaylists)
        }
        playlistPreferencesRepository.setPlaylistSongOrderModes(parsed.playlistSongOrderModes.orEmpty())
        playlistPreferencesRepository.setPlaylistsSortOption(
            parsed.playlistsSortOption ?: SortOption.PlaylistNameAZ.storageKey
        )
        userPreferencesRepository.clearLegacyUserPlaylists()
    }

    override suspend fun rollback(snapshot: String) = restore(snapshot)

    /**
     * Retries the pending restore on every distinct idle sync state: the first one at startup,
     * and each one a sync leaves behind. A running -> idle edge isn't required, because a short
     * sync can finish before its running state is ever observed.
     *
     * A retry that fails keeps the pending file for the next idle state, which counts songs the
     * failed one already added as the backup's copies instead of adding them again. Its
     * exception isn't let out: that would end this collector, and crash the app, since
     * [appScope] has no exception handler.
     */
    private suspend fun retryPendingWhenIdle() {
        syncManagerProvider().syncProgress
            .distinctUntilChanged()
            .filter { !it.isRunning }
            .collect {
                try {
                    resolvePendingPlaylists()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.withTag(TAG).e(e) { "Pending playlist restore failed; retrying after the next sync" }
                }
            }
    }

    /**
     * Saves [pending] for [resolvePendingPlaylists], or drops the pending restore when it's null.
     * Callers hold [pendingRestoreMutex].
     *
     * The file is written beside the pending one and then moved over it, so a process killed or
     * a disk filling up midway leaves the earlier pending restore whole instead of cut off.
     *
     * @return false when [pending] couldn't be saved; the earlier pending restore, if any, is
     * left as it was.
     */
    private fun writePendingRestore(pending: PendingPlaylistRestore?): Boolean =
        PlaylistsBackupPendingRestore.writePendingRestore(
            pending = pending,
            filesDir = appDirectories.filesDir,
            pendingFile = pendingRestoreFile,
            gson = gson
        )

    /**
     * Re-matches the pending restore's unresolved songs against the library and adds the ones it
     * has gained to their restored playlists. Songs already in a playlist are left alone, so
     * songs the user removed, moved or added since the restore stay that way. The pending file
     * is dropped once every song that can resolve has, or the playlists missing songs are gone.
     */
    private suspend fun resolvePendingPlaylists() = pendingRestoreMutex.withLock {
        val pendingFile = pendingRestoreFile
        if (!pendingFile.exists()) return@withLock

        val json = try {
            pendingFile.readText()
        } catch (e: Exception) {
            Logger.withTag(TAG).e(e) { "Failed to read pending playlist restore file" }
            return@withLock
        }
        // A file this can't parse is dropped, and so is one from before unresolvedSongIds was
        // recorded, which holds the whole backup payload instead: it doesn't say which songs its
        // restore left out, so filling them in could bring back songs the user removed since.
        val pending = runCatching { gson.fromJson(json, PendingPlaylistRestore::class.java) }.getOrNull()
        val songMetadata = pending?.songMetadata.orEmpty()
        val unresolvedSongIds = PlaylistsBackupPendingRestore.resolvableSongIds(pending?.unresolvedSongIds.orEmpty(), songMetadata)
        if (pending == null || unresolvedSongIds.isEmpty()) {
            pendingFile.delete()
            Logger.withTag(TAG).i { "Pending playlist restore: dropped, nothing it can still fill in" }
            return@withLock
        }

        val backupPlaylists = pending.playlists.orEmpty()
        val resolution = resolvePlaylists(backupPlaylists, songMetadata)
        val newlyResolved = unresolvedSongIds.filterTo(HashSet()) { resolution.resolvedSongIds[it] != null }

        val currentById = playlistPreferencesRepository.getPlaylistsOnce().associateBy { it.id }
        // Playlists deleted since the restore are left out, and so are smart playlists with a
        // rule, which rebuild their songs from it.
        val waiting = backupPlaylists.filter { backup ->
            val current = currentById[backup.id]
            current != null && SmartPlaylistRule.fromPlaylistSource(current.source) == null &&
                !SmartRuleCodec.isRuleSource(current.source)
        }
        var grownCount = 0
        if (newlyResolved.isNotEmpty()) {
            waiting.forEach { backup ->
                val current = currentById.getValue(backup.id)
                val songIds = PlaylistsBackupPendingRestore.insertNewlyResolvedSongs(
                    current.songIds, backup.songIds, newlyResolved, resolution.resolvedSongIds
                )
                // Only this playlist's songs are written: its name, cover and lastModified, and
                // the other playlists, keep any edit made since they were read.
                if (songIds != current.songIds &&
                    playlistPreferencesRepository.replacePlaylistSongs(current.id, songIds)
                ) {
                    grownCount++
                }
            }
        }

        val stillPending = PlaylistsBackupPendingRestore.pendingRestoreOf(waiting, songMetadata, unresolvedSongIds - newlyResolved)
        if (grownCount > 0) {
            Logger.withTag(TAG).i { "Pending playlist restore: $grownCount playlists grew, " +
                    "${stillPending?.unresolvedSongIds.orEmpty().size} songs still unresolved" }
        }
        if (stillPending == null) {
            pendingFile.delete()
            Logger.withTag(TAG).i { "Pending playlist restore: nothing left to fill in" }
        } else if (stillPending != pending) {
            // When this fails, the next retry reads the earlier file again, and counts the songs
            // this one added as the backup's copies instead of adding them twice.
            writePendingRestore(stillPending)
        }
    }

    /**
     * Resolves backup song IDs to current device song IDs (see [BackupSongResolver]). Songs
     * without a confident match are left out of the playlist, since keeping their backup ids
     * would risk false matches; [resolvePendingPlaylists] retries them after the next sync.
     */
    private suspend fun resolvePlaylists(
        playlists: List<Playlist>,
        songMetadata: Map<String, BackupSongMetadata>
    ): PlaylistsResolution {
        val library = songResolver.library()

        val resolutionCache = mutableMapOf<String, String?>()
        var totalSongs = 0
        var resolvedCount = 0
        var unresolvedCount = 0

        playlists.forEach { playlist ->
            playlist.songIds.forEach { songId ->
                if (songId !in resolutionCache) {
                    totalSongs++
                    val resolved = library.resolve(songId, songMetadata[songId])
                    resolutionCache[songId] = resolved
                    if (resolved != null) resolvedCount++ else unresolvedCount++
                }
            }
        }

        if (unresolvedCount > 0) {
            Logger.withTag(TAG).w { "Playlist restore: $resolvedCount/$totalSongs songs resolved, $unresolvedCount unresolved" }
        }

        val resolvedPlaylists = playlists.map { playlist ->
            playlist.copy(songIds = playlist.songIds.mapNotNull { songId -> resolutionCache[songId] })
        }
        return PlaylistsResolution(resolvedPlaylists, resolutionCache)
    }

    private data class PlaylistsResolution(
        val playlists: List<Playlist>,
        /** This library's id for each backup song id, or null when no song matches it yet. */
        val resolvedSongIds: Map<String, String?>
    ) {
        val unresolvedSongIds: Set<String>
            get() = resolvedSongIds.filterValues { it == null }.keys
    }

    private suspend fun buildCloudSongIdSet(): Set<String> {
        val cloudIds = mutableSetOf<String>()
        musicDao.getAllNavidromeSongIds().mapTo(cloudIds) { it.toString() }
        musicDao.getAllJellyfinSongIds().mapTo(cloudIds) { it.toString() }
        return cloudIds
    }

    private suspend fun restoreLegacyPreferenceEntries(payload: String) {
        val type = TypeToken.getParameterized(List::class.java, PreferenceBackupEntry::class.java).type
        val entries: List<PreferenceBackupEntry> = gson.fromJson(payload, type)

        val playlists = entries.firstOrNull { it.key == LEGACY_USER_PLAYLISTS_KEY }
            ?.stringValue
            ?.let { raw ->
                runCatching {
                    val playlistType = TypeToken.getParameterized(List::class.java, Playlist::class.java).type
                    gson.fromJson<List<Playlist>>(raw, playlistType)
                }.getOrDefault(emptyList())
            }
            .orEmpty()

        val playlistSongOrderModes = entries.firstOrNull { it.key == LEGACY_PLAYLIST_ORDER_MODES_KEY }
            ?.stringValue
            ?.let { raw ->
                runCatching {
                    val mapType = TypeToken.getParameterized(
                        Map::class.java,
                        String::class.java,
                        String::class.java
                    ).type
                    gson.fromJson<Map<String, String>>(raw, mapType)
                }.getOrDefault(emptyMap())
            }
            .orEmpty()

        val playlistsSortOption = entries.firstOrNull { it.key == LEGACY_PLAYLIST_SORT_OPTION_KEY }
            ?.stringValue
            ?: SortOption.PlaylistNameAZ.storageKey

        pendingRestoreMutex.withLock {
            // These playlists replace the ones an earlier restore was still filling in.
            writePendingRestore(null)
            playlistPreferencesRepository.replaceAllPlaylists(playlists)
        }
        playlistPreferencesRepository.setPlaylistSongOrderModes(playlistSongOrderModes)
        playlistPreferencesRepository.setPlaylistsSortOption(playlistsSortOption)
        userPreferencesRepository.clearLegacyUserPlaylists()
    }

    private data class PlaylistsBackupPayload(
        val playlists: List<Playlist>? = null,
        val playlistSongOrderModes: Map<String, String>? = null,
        val playlistsSortOption: String? = null,
        /** Song metadata for cross-device matching. Key = songId from backup. Null in legacy/snapshot payloads. */
        val songMetadata: Map<String, BackupSongMetadata>? = null,
        /** Base64-encoded cover images. Key = playlist ID. Null if no custom covers. */
        val coverImages: Map<String, String>? = null
    )

    companion object {
        private const val TAG = "PlaylistsModuleHandler"

        /** Playlist sources that are backed up. Cloud-sourced playlists are excluded. */
        private fun isBackedUpPlaylistSource(source: String): Boolean =
            source == "LOCAL" || isSmartPlaylistSource(source)

        const val LEGACY_USER_PLAYLISTS_KEY = "user_playlists_json_v1"
        const val LEGACY_PLAYLIST_ORDER_MODES_KEY = "playlist_song_order_modes"
        const val LEGACY_PLAYLIST_SORT_OPTION_KEY = "playlists_sort_option"
        val PLAYLIST_KEYS = setOf(
            LEGACY_USER_PLAYLISTS_KEY,
            LEGACY_PLAYLIST_ORDER_MODES_KEY,
            LEGACY_PLAYLIST_SORT_OPTION_KEY
        )
    }
}
