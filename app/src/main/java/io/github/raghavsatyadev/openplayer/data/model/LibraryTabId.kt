package io.github.raghavsatyadev.openplayer.data.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import io.github.raghavsatyadev.openplayer.R

@Immutable
enum class LibraryTabId(
    val storageKey: String,
    @StringRes val titleRes: Int,
    val defaultSort: SortOption
) {
    SONGS("SONGS", R.string.library_tab_songs, SortOption.SongTitleAZ),
    ALBUMS("ALBUMS", R.string.library_tab_albums, SortOption.AlbumTitleAZ),
    ARTISTS("ARTIST", R.string.library_tab_artists, SortOption.ArtistNameAZ),
    PLAYLISTS("PLAYLISTS", R.string.library_tab_playlists, SortOption.PlaylistNameAZ),
    FOLDERS("FOLDERS", R.string.library_tab_folders, SortOption.FolderNameAZ),
    LIKED("LIKED", R.string.library_tab_liked, SortOption.LikedSongDateLiked),
    GENRES("GENRES", R.string.library_tab_genres, SortOption.GenreNameAZ),

    /** Local music videos. Has no sort options; the list is sorted by name. */
    VIDEOS("VIDEOS", R.string.library_tab_videos, SortOption.SongTitleAZ);

    companion object {
        /** Tabs that start hidden; the user turns them on from the tab editor. */
        val hiddenByDefault: Set<LibraryTabId> = setOf(VIDEOS)

        fun fromStorageKey(key: String): LibraryTabId =
            entries.firstOrNull { it.storageKey == key } ?: SONGS
    }
}

fun String.toLibraryTabIdOrNull(): LibraryTabId? =
    LibraryTabId.entries.firstOrNull { it.storageKey == this }