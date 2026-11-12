package io.github.raghavsatyadev.openplayer.presentation.screens

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.presentation.settings.search.settingHighlight
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.search.SearchTapMode
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.search.SearchTapModeViewModel

/** Choice of what tapping a song in the search results does. */
@Composable
fun SearchTapModeSetting(highlightKey: String?) {
    val viewModel: SearchTapModeViewModel = hiltViewModel()
    val mode by viewModel.mode.collectAsState()
    ThemeSelectorItem(
        label = stringResource(R.string.setcat_search_tap_mode_label),
        description = stringResource(R.string.setcat_search_tap_mode_desc),
        options = SearchTapMode.entries.associate { it.key to stringResource(it.labelRes) },
        selectedKey = mode.key,
        onSelectionChanged = { viewModel.setMode(SearchTapMode.fromKey(it)) },
        leadingIcon = {
            Icon(painterResource(R.drawable.rounded_manage_search_24), null, tint = MaterialTheme.colorScheme.secondary)
        },
        modifier = Modifier.settingHighlight("item_behavior_search_tap_mode", highlightKey)
    )
}
