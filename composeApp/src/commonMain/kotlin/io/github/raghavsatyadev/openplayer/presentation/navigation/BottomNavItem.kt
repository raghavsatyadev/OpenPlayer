package io.github.raghavsatyadev.openplayer.presentation.navigation

import androidx.compose.runtime.Immutable
import io.github.raghavsatyadev.openplayer.resources.Res
import io.github.raghavsatyadev.openplayer.resources.home_24_rounded_filled
import io.github.raghavsatyadev.openplayer.resources.round_library_music_24
import io.github.raghavsatyadev.openplayer.resources.rounded_home_24
import io.github.raghavsatyadev.openplayer.resources.rounded_library_music_24
import io.github.raghavsatyadev.openplayer.resources.rounded_search_24
import io.github.raghavsatyadev.openplayer.resources.search
import io.github.raghavsatyadev.openplayer.resources.tab_home
import io.github.raghavsatyadev.openplayer.resources.tab_library
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

@Immutable
data class BottomNavItem(
    /** Shown as the tab label and content description, so it follows the app language. */
    val labelResId: StringResource,
    val iconResId: DrawableResource,
    val selectedIconResId: DrawableResource? = null,
    val screen: Screen
)

/** Top-level destinations shared by the bottom navigation bar and the navigation rail. */
internal val MainNavItems: ImmutableList<BottomNavItem> = persistentListOf(
    BottomNavItem(Res.string.tab_home, Res.drawable.rounded_home_24, Res.drawable.home_24_rounded_filled, Screen.Home),
    BottomNavItem(Res.string.search, Res.drawable.rounded_search_24, Res.drawable.rounded_search_24, Screen.Search),
    BottomNavItem(Res.string.tab_library, Res.drawable.rounded_library_music_24, Res.drawable.round_library_music_24, Screen.Library)
)
