package io.github.raghavsatyadev.openplayer.presentation.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.raghavsatyadev.openplayer.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The search-tap preference for the search screen and its settings row. */
@HiltViewModel
class SearchTapModeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val mode: StateFlow<SearchTapMode> = userPreferencesRepository.searchTapModeFlow
        .map(SearchTapMode::fromKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchTapMode.DEFAULT)

    fun setMode(mode: SearchTapMode) {
        viewModelScope.launch { userPreferencesRepository.setSearchTapMode(mode.key) }
    }
}
