package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.preferences.SeekbarStyle
import io.github.raghavsatyadev.openplayer.data.preferences.UserPreferencesRepository
import io.github.raghavsatyadev.openplayer.data.waveform.WaveformRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Seekbar style preference and the waveform of the current song. */
class SeekbarViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val waveformRepository: WaveformRepository,
) : ViewModel() {

    val style: StateFlow<String> = userPreferencesRepository.seekbarStyleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SeekbarStyle.WAVY)

    fun setStyle(style: String) {
        viewModelScope.launch { userPreferencesRepository.setSeekbarStyle(style) }
    }

    fun waveformFor(song: Song): Flow<FloatArray?> = waveformRepository.waveformFor(song)
}
