package io.github.raghavsatyadev.openplayer.presentation.viewmodel

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import io.github.raghavsatyadev.openplayer.data.model.Artist
import io.github.raghavsatyadev.openplayer.data.preferences.AlbumArtQuality
import io.github.raghavsatyadev.openplayer.data.preferences.CarouselStyle
import io.github.raghavsatyadev.openplayer.data.preferences.FullPlayerLoadingTweaks
import io.github.raghavsatyadev.openplayer.data.preferences.NavBarStyle
import io.github.raghavsatyadev.openplayer.data.preferences.ThemePreference

data class FullPlayerSlice(
    val currentSongArtists: ImmutableList<Artist> = persistentListOf(),
    val lyricsSyncOffset: Int = 0,
    val albumArtQuality: AlbumArtQuality = AlbumArtQuality.MEDIUM,
    val audioMetadata: PlaybackAudioMetadata = PlaybackAudioMetadata(),
    val showPlayerFileInfo: Boolean = true,
    val immersiveLyricsEnabled: Boolean = false,
    val immersiveLyricsTimeout: Long = 4000L,
    val isImmersiveTemporarilyDisabled: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val bluetoothName: String? = null
)

data class PlayerConfigSlice(
    val navBarCornerRadius: Int = 32,
    val navBarStyle: String = NavBarStyle.DEFAULT,
    val carouselStyle: String = CarouselStyle.NO_PEEK,
    val fullPlayerLoadingTweaks: FullPlayerLoadingTweaks = FullPlayerLoadingTweaks(),
    val tapBackgroundClosesPlayer: Boolean = false,
    val useSmoothCorners: Boolean = true,
    val playerThemePreference: String = ThemePreference.ALBUM_ART,
    val miniPlayerSwipeToSkip: Boolean = false
)
