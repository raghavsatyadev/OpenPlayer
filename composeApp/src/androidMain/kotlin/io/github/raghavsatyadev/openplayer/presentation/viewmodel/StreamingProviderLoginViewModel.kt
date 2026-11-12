package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import io.github.raghavsatyadev.openplayer.data.jellyfin.JellyfinRepository
import io.github.raghavsatyadev.openplayer.data.navidrome.NavidromeRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Login state of the streaming providers for screens that only show it, such as Home's
 * provider sheet. The dashboard ViewModels call the server and may start a full library sync
 * as soon as they are created, which Home must not do on every launch.
 */
class StreamingProviderLoginViewModel(
    navidromeRepository: NavidromeRepository,
    jellyfinRepository: JellyfinRepository,
) : ViewModel() {
    val isNavidromeLoggedIn: StateFlow<Boolean> = navidromeRepository.isLoggedInFlow
    val isJellyfinLoggedIn: StateFlow<Boolean> = jellyfinRepository.isLoggedInFlow
}
