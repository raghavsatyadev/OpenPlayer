package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.raghavsatyadev.openplayer.data.preferences.VideoPreferences
import io.github.raghavsatyadev.openplayer.data.preferences.VideoSettings
import io.github.raghavsatyadev.openplayer.data.video.LocalVideoRepository
import io.github.raghavsatyadev.openplayer.data.video.VideoMatchingMode
import io.github.raghavsatyadev.openplayer.data.worker.SyncManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Settings > Playback > Music videos. */
class VideoSettingsViewModel(
    private val videoPreferences: VideoPreferences,
    private val videoRepository: LocalVideoRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    val settings: StateFlow<VideoSettings> = videoRepository.settings
    val hasPermission: StateFlow<Boolean> = videoRepository.hasPermission

    fun setShowInPlayer(show: Boolean) {
        viewModelScope.launch { videoPreferences.setShowInPlayer(show) }
    }

    fun setMatchingMode(mode: VideoMatchingMode) {
        viewModelScope.launch { videoPreferences.setMatchingMode(mode) }
    }

    fun setSameFolderOnly(sameFolderOnly: Boolean) {
        viewModelScope.launch { videoPreferences.setSameFolderOnly(sameFolderOnly) }
    }

    val videoFolders: StateFlow<List<String>> = videoRepository.videoFolders

    fun setFolders(included: Set<String>, excluded: Set<String>) {
        viewModelScope.launch {
            videoPreferences.setFolders(included, excluded)
            if (settings.value.addToLibrary) syncManager.forceRefresh()
        }
    }

    /** Adds or removes the allowed videos as library songs; the sync does the work. */
    fun setAddToLibrary(add: Boolean) {
        viewModelScope.launch {
            videoPreferences.setAddToLibrary(add)
            syncManager.forceRefresh()
        }
    }

    fun onPermissionResult() = videoRepository.requestRefresh()
}
