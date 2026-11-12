package io.github.raghavsatyadev.openplayer.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.video.LocalVideoRepository
import io.github.raghavsatyadev.openplayer.data.video.VideoMatchingMode
import io.github.raghavsatyadev.openplayer.presentation.settings.search.settingHighlight
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.VideoSettingsViewModel

/** Show matching local videos in the player, and how videos are matched to tracks. */
@Composable
fun VideoSettingsSection(
    highlightKey: String?,
    viewModel: VideoSettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
    var showFolders by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult()
        if (granted) viewModel.setShowInPlayer(true)
    }
    val icon: @Composable () -> Unit = {
        Icon(painterResource(R.drawable.rounded_movie_24), null, tint = MaterialTheme.colorScheme.secondary)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SwitchSettingItem(
            title = stringResource(R.string.setting_video_show_in_player_title),
            subtitle = stringResource(R.string.setting_video_show_in_player_subtitle),
            checked = settings.showInPlayer && hasPermission,
            onCheckedChange = { checked ->
                if (checked && !hasPermission) {
                    permissionLauncher.launch(LocalVideoRepository.videoPermission)
                } else {
                    viewModel.setShowInPlayer(checked)
                }
            },
            leadingIcon = icon,
            modifier = Modifier.settingHighlight("item_playback_videos", highlightKey)
        )
        ThemeSelectorItem(
            label = stringResource(R.string.setting_video_matching_title),
            description = stringResource(R.string.setting_video_matching_auto),
            options = mapOf(
                VideoMatchingMode.AUTO.storageKey to stringResource(R.string.setting_video_matching_auto),
                VideoMatchingMode.FILENAME.storageKey to stringResource(R.string.setting_video_matching_filename),
                VideoMatchingMode.TITLE_AND_ARTIST.storageKey to
                    stringResource(R.string.setting_video_matching_title_artist),
            ),
            selectedKey = settings.matchingMode.storageKey,
            onSelectionChanged = { viewModel.setMatchingMode(VideoMatchingMode.fromStorageKey(it)) },
            leadingIcon = icon,
            modifier = Modifier.settingHighlight("item_playback_video_matching", highlightKey)
        )
        SwitchSettingItem(
            title = stringResource(R.string.setting_video_same_folder_title),
            subtitle = stringResource(R.string.setting_video_same_folder_subtitle),
            checked = settings.sameFolderOnly,
            onCheckedChange = viewModel::setSameFolderOnly,
            leadingIcon = {
                Icon(painterResource(R.drawable.rounded_folder_24), null, tint = MaterialTheme.colorScheme.secondary)
            },
            modifier = Modifier.settingHighlight("item_playback_video_same_folder", highlightKey)
        )
        SwitchSettingItem(
            title = stringResource(R.string.setting_video_add_to_library_title),
            subtitle = stringResource(R.string.setting_video_add_to_library_subtitle),
            checked = settings.addToLibrary && hasPermission,
            onCheckedChange = { checked ->
                if (checked && !hasPermission) {
                    permissionLauncher.launch(LocalVideoRepository.videoPermission)
                } else {
                    viewModel.setAddToLibrary(checked)
                }
            },
            leadingIcon = {
                Icon(painterResource(R.drawable.rounded_library_music_24), null, tint = MaterialTheme.colorScheme.secondary)
            },
            modifier = Modifier.settingHighlight("item_playback_video_add_to_library", highlightKey)
        )
        SettingsItem(
            title = stringResource(R.string.setting_video_folders_title),
            subtitle = if (settings.includedFolders.isEmpty() && settings.excludedFolders.isEmpty()) {
                stringResource(R.string.setting_video_folders_all)
            } else {
                stringResource(
                    R.string.setting_video_folders_summary,
                    settings.includedFolders.size,
                    settings.excludedFolders.size,
                )
            },
            leadingIcon = {
                Icon(painterResource(R.drawable.rounded_folder_open_24), null, tint = MaterialTheme.colorScheme.secondary)
            },
            modifier = Modifier.settingHighlight("item_playback_video_folders", highlightKey),
            onClick = { showFolders = true },
        )
    }

    if (showFolders) {
        val folders by viewModel.videoFolders.collectAsStateWithLifecycle()
        VideoFoldersSheet(
            folders = folders,
            included = settings.includedFolders,
            excluded = settings.excludedFolders,
            onChange = viewModel::setFolders,
            onDismiss = { showFolders = false },
        )
    }
}
