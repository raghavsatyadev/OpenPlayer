package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.raghavsatyadev.openplayer.data.library.DuplicateFinder
import io.github.raghavsatyadev.openplayer.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DuplicateSongsUiState(
    val isLoading: Boolean = true,
    val groups: List<DuplicateFinder.DuplicateGroup> = emptyList(),
) {
    val totalDuplicates: Int get() = groups.sumOf { it.songs.size }
}

class DuplicateSongsViewModel(
    private val musicRepository: MusicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuplicateSongsUiState())
    val uiState: StateFlow<DuplicateSongsUiState> = _uiState.asStateFlow()

    init {
        scan()
    }

    fun scan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val groups = withContext(Dispatchers.Default) {
                DuplicateFinder.findDuplicates(musicRepository.getAllSongsOnce())
            }
            _uiState.update { it.copy(isLoading = false, groups = groups) }
        }
    }
}
