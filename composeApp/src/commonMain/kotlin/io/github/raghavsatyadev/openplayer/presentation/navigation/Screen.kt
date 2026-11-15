package io.github.raghavsatyadev.openplayer.presentation.navigation

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 keys. Each destination is a serializable [NavKey] that carries its own arguments,
 * so screens and ViewModels get typed values instead of parsing route strings.
 */
@Immutable
@Serializable
sealed interface Screen : NavKey {

    /** Shows the bottom bar / rail. Only the main tabs and the cloud dashboards keep it. */
    val showsNavBar: Boolean get() = false

    /** Hides the mini player while this screen is on top. */
    val hidesMiniPlayer: Boolean get() = false

    /** Main tab index (Home 0, Search 1, Library 2), or null for every other screen. */
    val mainTabIndex: Int? get() = null

    /** Same screen with a settings row to highlight; screens without highlight support ignore it. */
    fun withHighlight(highlightKey: String?): Screen = this

    @Serializable data object Home : Screen {
        override val showsNavBar get() = true
        override val mainTabIndex get() = 0
    }

    @Serializable data object Search : Screen {
        override val showsNavBar get() = true
        override val mainTabIndex get() = 1
    }

    @Serializable data object Library : Screen {
        override val showsNavBar get() = true
        override val mainTabIndex get() = 2
    }

    @Serializable data object Settings : Screen

    @Serializable data class Accounts(val highlightKey: String? = null) : Screen {
        override fun withHighlight(highlightKey: String?) = copy(highlightKey = highlightKey)
    }

    @Serializable data class SettingsCategory(
        val categoryId: String,
        val highlightKey: String? = null,
    ) : Screen {
        override fun withHighlight(highlightKey: String?) = copy(highlightKey = highlightKey)
    }

    @Serializable data class PaletteStyle(val highlightKey: String? = null) : Screen {
        override fun withHighlight(highlightKey: String?) = copy(highlightKey = highlightKey)
    }

    @Serializable data class Experimental(val highlightKey: String? = null) : Screen {
        override fun withHighlight(highlightKey: String?) = copy(highlightKey = highlightKey)
    }

    @Serializable data class LyricsSettings(val highlightKey: String? = null) : Screen {
        override fun withHighlight(highlightKey: String?) = copy(highlightKey = highlightKey)
    }

    @Serializable data class NavBarCrRad(val highlightKey: String? = null) : Screen {
        override val hidesMiniPlayer get() = true
        override fun withHighlight(highlightKey: String?) = copy(highlightKey = highlightKey)
    }

    @Serializable data class PlaylistDetail(val playlistId: String) : Screen

    @Serializable data object DailyMixScreen : Screen
    @Serializable data object RecentlyPlayed : Screen
    @Serializable data object Stats : Screen

    @Serializable data class Recap(val year: Int) : Screen {
        override val hidesMiniPlayer get() = true
    }

    @Serializable data class Duplicates(val highlightKey: String? = null) : Screen {
        override fun withHighlight(highlightKey: String?) = copy(highlightKey = highlightKey)
    }

    @Serializable data class GenreDetail(val genreId: String) : Screen
    @Serializable data object DJSpace : Screen
    @Serializable data class AlbumDetail(val albumId: Long) : Screen
    @Serializable data class ArtistDetail(val artistId: Long) : Screen
    @Serializable data class EditTransition(val playlistId: String? = null) : Screen

    @Serializable data object About : Screen
    @Serializable data object Licenses : Screen
    @Serializable data object EasterEgg : Screen

    @Serializable data class ArtistSettings(val highlightKey: String? = null) : Screen {
        override fun withHighlight(highlightKey: String?) = copy(highlightKey = highlightKey)
    }

    @Serializable data object DelimiterConfig : Screen
    @Serializable data object WordDelimiterConfig : Screen
    @Serializable data object ExcludedDirectories : Screen

    @Serializable data class Equalizer(val highlightKey: String? = null) : Screen {
        override fun withHighlight(highlightKey: String?) = copy(highlightKey = highlightKey)
    }

    @Serializable data class DeviceCapabilities(val highlightKey: String? = null) : Screen {
        override fun withHighlight(highlightKey: String?) = copy(highlightKey = highlightKey)
    }

    @Serializable data object NavidromeDashboard : Screen {
        override val showsNavBar get() = true
    }

    @Serializable data object JellyfinDashboard : Screen {
        override val showsNavBar get() = true
    }

    @Serializable data object CloudDownloads : Screen {
        override val showsNavBar get() = true
    }

    @Serializable data object Updates : Screen
    @Serializable data object AudioBookmarks : Screen
    @Serializable data class AudioBookmarkFolder(val songId: String) : Screen
}
