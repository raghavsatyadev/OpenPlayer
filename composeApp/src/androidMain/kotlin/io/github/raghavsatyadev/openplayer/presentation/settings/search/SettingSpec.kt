package io.github.raghavsatyadev.openplayer.presentation.settings.search

import io.github.raghavsatyadev.openplayer.presentation.navigation.AppNavigator
import io.github.raghavsatyadev.openplayer.presentation.navigation.Screen
import io.github.raghavsatyadev.openplayer.presentation.model.SettingsCategory
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SettingsUiState
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SettingsViewModel
import org.jetbrains.compose.resources.StringResource

enum class SettingType {
    SWITCH,
    NAVIGABLE_CARD,
    ACTION
}

data class SettingSpec(
    val id: String,
    val itemKey: String = "setting_$id",
    val titleRes: StringResource? = null,
    val titleStatic: String? = null,
    val subtitleRes: StringResource? = null,
    val subtitleStatic: String? = null,
    val category: SettingsCategory,
    val subscreenRoute: Screen,
    val type: SettingType,
    val keywordsRes: List<StringResource> = emptyList(),
    val keywordsStatic: List<String> = emptyList(),
    val getValue: ((SettingsUiState) -> Boolean)? = null,
    val onToggle: ((SettingsViewModel, Boolean) -> Unit)? = null,
    val onAction: ((AppNavigator) -> Unit)? = null
) {
    /** The subscreen with this row highlighted (screens without highlight support ignore it). */
    fun createNavigationRoute(): Screen = subscreenRoute.withHighlight(itemKey)
}

data class SearchResultItem(
    val spec: SettingSpec,
    val score: Float,
    val matchedTitle: String,
    val matchedSubtitle: String?
)

data class SearchResultSection(
    val titleRes: StringResource,
    val items: List<SearchResultItem>
)
