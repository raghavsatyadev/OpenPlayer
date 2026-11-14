package io.github.raghavsatyadev.openplayer.presentation.model

import io.github.raghavsatyadev.openplayer.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

enum class SettingsCategory(
    val id: String,
    val titleRes: StringResource,
    val subtitleRes: StringResource,
    val iconRes: DrawableResource,
) {
    LIBRARY(
        id = "library",
        titleRes = Res.string.settings_category_library_title,
        subtitleRes = Res.string.settings_category_library_subtitle,
        iconRes = Res.drawable.rounded_library_music_filled_24
    ),
    APPEARANCE(
        id = "appearance",
        titleRes = Res.string.settings_category_appearance_title,
        subtitleRes = Res.string.settings_category_appearance_subtitle,
        iconRes = Res.drawable.rounded_palette_filled_24
    ),
    PLAYBACK(
        id = "playback",
        titleRes = Res.string.settings_category_playback_title,
        subtitleRes = Res.string.settings_category_playback_subtitle,
        iconRes = Res.drawable.rounded_music_note_24
    ),
    BEHAVIOR(
        id = "behavior",
        titleRes = Res.string.settings_category_behavior_title,
        subtitleRes = Res.string.settings_category_behavior_subtitle,
        iconRes = Res.drawable.rounded_touch_app_24
    ),
    BACKUP_RESTORE(
        id = "backup_restore",
        titleRes = Res.string.settings_category_backup_title,
        subtitleRes = Res.string.settings_category_backup_subtitle,
        iconRes = Res.drawable.rounded_upload_file_24
    ),
    DEVELOPER(
        id = "developer",
        titleRes = Res.string.settings_category_developer_title,
        subtitleRes = Res.string.settings_category_developer_subtitle,
        iconRes = Res.drawable.rounded_developer_mode_filled_24
    ),
    EQUALIZER(
        id = "equalizer",
        titleRes = Res.string.settings_category_equalizer_title,
        subtitleRes = Res.string.settings_category_equalizer_subtitle,
        iconRes = Res.drawable.rounded_graphic_eq_24
    ),
    DEVICE_CAPABILITIES(
        id = "device_capabilities",
        titleRes = Res.string.settings_category_device_capabilities_title,
        subtitleRes = Res.string.settings_category_device_capabilities_subtitle,
        iconRes = Res.drawable.rounded_developer_board_filled_24
    ),
    ABOUT(
        id = "about",
        titleRes = Res.string.settings_category_about_title,
        subtitleRes = Res.string.settings_category_about_subtitle,
        iconRes = Res.drawable.rounded_info_filled_24
    );

    companion object {
        fun fromId(id: String): SettingsCategory? = entries.find { it.id == id }
    }
}
