package io.github.raghavsatyadev.openplayer.data.model

import androidx.compose.runtime.Immutable
import io.github.raghavsatyadev.openplayer.resources.Res
import io.github.raghavsatyadev.openplayer.resources.*
import org.jetbrains.compose.resources.StringResource

@Immutable
enum class LibraryTabId(
    val storageKey: String,
    val titleRes: StringResource,
    val defaultSort: SortOption
) {
    SONGS("SONGS", Res.string.library_tab_songs, SortOption.SongTitleAZ),
    ALBUMS("ALBUMS", Res.string.library_tab_albums, SortOption.AlbumTitleAZ),
    ARTISTS("ARTIST", Res.string.library_tab_artists, SortOption.ArtistNameAZ),
    PLAYLISTS("PLAYLISTS", Res.string.library_tab_playlists, SortOption.PlaylistNameAZ),
    FOLDERS("FOLDERS", Res.string.library_tab_folders, SortOption.FolderNameAZ),
    LIKED("LIKED", Res.string.library_tab_liked, SortOption.LikedSongDateLiked),
    GENRES("GENRES", Res.string.library_tab_genres, SortOption.GenreNameAZ),

    /** Local music videos. Has no sort options; the list is sorted by name. */
    VIDEOS("VIDEOS", Res.string.library_tab_videos, SortOption.SongTitleAZ);

    companion object {
        /** Tabs that start hidden; the user turns them on from the tab editor. */
        val hiddenByDefault: Set<LibraryTabId> = setOf(VIDEOS)

        fun fromStorageKey(key: String): LibraryTabId =
            entries.firstOrNull { it.storageKey == key } ?: SONGS
    }
}

fun String.toLibraryTabIdOrNull(): LibraryTabId? =
    LibraryTabId.entries.firstOrNull { it.storageKey == this }