package io.github.raghavsatyadev.openplayer.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.raghavsatyadev.openplayer.data.model.LibraryTabId
import io.github.raghavsatyadev.openplayer.data.model.StorageFilter
import io.github.raghavsatyadev.openplayer.presentation.components.MiniPlayerHeight
import io.github.raghavsatyadev.openplayer.ui.theme.RoundedSans
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import io.github.raghavsatyadev.openplayer.resources.*

private data class LibraryEmptySpec(
    val iconRes: DrawableResource,
    val titleRes: StringResource,
    val subtitleRes: StringResource
)

private fun libraryEmptySpec(
    tabId: LibraryTabId,
    storageFilter: StorageFilter
): LibraryEmptySpec {
    return when (tabId) {
        LibraryTabId.SONGS -> when (storageFilter) {
            StorageFilter.ALL -> LibraryEmptySpec(
                iconRes = Res.drawable.rounded_music_off_24,
                titleRes = Res.string.lib_empty_songs_all_title,
                subtitleRes = Res.string.lib_empty_songs_all_subtitle
            )
            StorageFilter.OFFLINE -> LibraryEmptySpec(
                iconRes = Res.drawable.rounded_music_off_24,
                titleRes = Res.string.lib_empty_songs_offline_title,
                subtitleRes = Res.string.lib_empty_songs_offline_subtitle
            )
            StorageFilter.ONLINE -> LibraryEmptySpec(
                iconRes = Res.drawable.rounded_music_off_24,
                titleRes = Res.string.lib_empty_songs_online_title,
                subtitleRes = Res.string.lib_empty_songs_online_subtitle
            )
        }

        LibraryTabId.ALBUMS -> when (storageFilter) {
            StorageFilter.ALL -> LibraryEmptySpec(
                iconRes = Res.drawable.rounded_album_24,
                titleRes = Res.string.lib_empty_albums_all_title,
                subtitleRes = Res.string.lib_empty_albums_all_subtitle
            )
            StorageFilter.OFFLINE -> LibraryEmptySpec(
                iconRes = Res.drawable.rounded_album_24,
                titleRes = Res.string.lib_empty_albums_offline_title,
                subtitleRes = Res.string.lib_empty_albums_offline_subtitle
            )
            StorageFilter.ONLINE -> LibraryEmptySpec(
                iconRes = Res.drawable.rounded_album_24,
                titleRes = Res.string.lib_empty_albums_online_title,
                subtitleRes = Res.string.lib_empty_albums_online_subtitle
            )
        }

        LibraryTabId.ARTISTS -> when (storageFilter) {
            StorageFilter.ALL -> LibraryEmptySpec(
                iconRes = Res.drawable.rounded_artist_24,
                titleRes = Res.string.lib_empty_artists_all_title,
                subtitleRes = Res.string.lib_empty_artists_all_subtitle
            )
            StorageFilter.OFFLINE -> LibraryEmptySpec(
                iconRes = Res.drawable.rounded_artist_24,
                titleRes = Res.string.lib_empty_artists_offline_title,
                subtitleRes = Res.string.lib_empty_artists_offline_subtitle
            )
            StorageFilter.ONLINE -> LibraryEmptySpec(
                iconRes = Res.drawable.rounded_artist_24,
                titleRes = Res.string.lib_empty_artists_online_title,
                subtitleRes = Res.string.lib_empty_artists_online_subtitle
            )
        }

        LibraryTabId.LIKED -> when (storageFilter) {
            StorageFilter.ALL -> LibraryEmptySpec(
                iconRes = Res.drawable.round_favorite_24,
                titleRes = Res.string.lib_empty_liked_all_title,
                subtitleRes = Res.string.lib_empty_liked_all_subtitle
            )
            StorageFilter.OFFLINE -> LibraryEmptySpec(
                iconRes = Res.drawable.round_favorite_24,
                titleRes = Res.string.lib_empty_liked_offline_title,
                subtitleRes = Res.string.lib_empty_liked_offline_subtitle
            )
            StorageFilter.ONLINE -> LibraryEmptySpec(
                iconRes = Res.drawable.round_favorite_24,
                titleRes = Res.string.lib_empty_liked_online_title,
                subtitleRes = Res.string.lib_empty_liked_online_subtitle
            )
        }

        LibraryTabId.FOLDERS -> LibraryEmptySpec(
            iconRes = Res.drawable.ic_folder,
            titleRes = Res.string.lib_empty_folders_title,
            subtitleRes = Res.string.lib_empty_folders_subtitle
        )

        LibraryTabId.PLAYLISTS -> LibraryEmptySpec(
            iconRes = Res.drawable.rounded_playlist_play_24,
            titleRes = Res.string.lib_empty_playlists_title,
            subtitleRes = Res.string.lib_empty_playlists_subtitle
        )

        LibraryTabId.GENRES -> LibraryEmptySpec(
            iconRes = Res.drawable.rounded_category_24,
            titleRes = Res.string.lib_empty_genres_title,
            subtitleRes = Res.string.lib_empty_genres_subtitle
        )

        LibraryTabId.VIDEOS -> LibraryEmptySpec(
            iconRes = Res.drawable.rounded_movie_24,
            titleRes = Res.string.lib_empty_videos_title,
            subtitleRes = Res.string.lib_empty_videos_subtitle
        )
    }
}

@Composable
internal fun LibraryExpressiveEmptyState(
    tabId: LibraryTabId,
    storageFilter: StorageFilter,
    bottomBarHeight: Dp,
    modifier: Modifier = Modifier
) {
    val spec = remember(tabId, storageFilter) { libraryEmptySpec(tabId, storageFilter) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = 28.dp,
                end = 28.dp,
                bottom = bottomBarHeight + MiniPlayerHeight + 24.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                tonalElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(spec.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(spec.titleRes),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = RoundedSans,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(spec.subtitleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
