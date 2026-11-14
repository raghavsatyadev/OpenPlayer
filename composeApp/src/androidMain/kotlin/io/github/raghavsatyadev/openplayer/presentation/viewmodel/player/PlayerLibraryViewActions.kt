package io.github.raghavsatyadev.openplayer.presentation.viewmodel.player

import io.github.raghavsatyadev.openplayer.data.model.FolderSource
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.preferences.UserPreferencesRepository
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ENABLE_FOLDERS_SOURCE_SWITCHING
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.FolderNavigationStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.LibraryStateHolder
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/** Actions that stay in PlayerViewModel and are invoked by [PlayerLibraryViewActions]. */
class PlayerLibraryViewCallbacks(
    val requiresHydration: (Song) -> Boolean,
    val hydrateSongsIfNeeded: suspend (List<Song>) -> List<Song>,
)

/** Folder navigation, library view preferences and library tab order persistence extracted from PlayerViewModel. */
class PlayerLibraryViewActions(
    private val scope: CoroutineScope,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val folderNavigationStateHolder: FolderNavigationStateHolder,
    private val libraryStateHolder: LibraryStateHolder,
    private val playerUiState: MutableStateFlow<PlayerUiState>,
    private val callbacks: PlayerLibraryViewCallbacks,
) {

    fun setFoldersPlaylistView(isPlaylistView: Boolean) {
        scope.launch {
            userPreferencesRepository.setFoldersPlaylistView(isPlaylistView)
            folderNavigationStateHolder.setFoldersPlaylistViewState(
                isPlaylistView = isPlaylistView,
                updateUiState = { mutation -> playerUiState.update(mutation) }
            )
        }
    }

    fun setFoldersSource(source: FolderSource) {
        if (!ENABLE_FOLDERS_SOURCE_SWITCHING) return
        scope.launch {
            userPreferencesRepository.setFoldersSource(source)
        }
    }

    fun navigateToFolder(path: String) {
        folderNavigationStateHolder.navigateToFolder(
            path = path,
            getUiState = { playerUiState.value },
            updateUiState = { mutation -> playerUiState.update(mutation) },
            cachedSongsById = { libraryStateHolder.allSongsById.value },
            onFolderChanged = { folderPath -> hydrateCurrentFolderSongs(folderPath) }
        )
    }

    fun navigateBackFolder() {
        folderNavigationStateHolder.navigateBackFolder(
            getUiState = { playerUiState.value },
            updateUiState = { mutation -> playerUiState.update(mutation) },
            cachedSongsById = { libraryStateHolder.allSongsById.value },
            onFolderChanged = { folderPath -> hydrateCurrentFolderSongs(folderPath) }
        )
    }

    private fun hydrateCurrentFolderSongs(folderPath: String) {
        folderNavigationStateHolder.hydrateCurrentFolderSongsIfNeeded(
            scope = scope,
            folderPath = folderPath,
            getUiState = { playerUiState.value },
            updateUiState = { mutation -> playerUiState.update(mutation) },
            requiresHydration = { song -> callbacks.requiresHydration(song) },
            hydrateSongs = { songs -> callbacks.hydrateSongsIfNeeded(songs) }
        )
    }

    fun setAlbumsListView(isList: Boolean) {
        scope.launch {
            userPreferencesRepository.setAlbumsListView(isList)
        }
    }

    fun setPlaylistsGridView(isGrid: Boolean) {
        scope.launch {
            userPreferencesRepository.setPlaylistsGridView(isGrid)
        }
    }

    fun saveLibraryTabsOrder(tabs: List<String>) {
        scope.launch {
            val orderJson = Json.encodeToString(tabs)
            userPreferencesRepository.saveLibraryTabsOrder(orderJson)
        }
    }

    fun resetLibraryTabsOrder() {
        scope.launch {
            userPreferencesRepository.resetLibraryTabsOrder()
        }
    }
}
