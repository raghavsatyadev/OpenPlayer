package io.github.raghavsatyadev.openplayer.presentation.screens

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import io.github.raghavsatyadev.openplayer.data.preferences.SeekbarStyle
import io.github.raghavsatyadev.openplayer.presentation.settings.search.settingHighlight
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SeekbarViewModel
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource
import org.jetbrains.compose.resources.painterResource

/** Wavy / Waveform choice for the full player's seekbar. */
@Composable
fun SeekbarStyleSetting(highlightKey: String?) {
    val viewModel: SeekbarViewModel = koinViewModel()
    val style by viewModel.style.collectAsState()
    ThemeSelectorItem(
        label = stringResource(Res.string.setcat_seekbar_style_label),
        description = stringResource(Res.string.setcat_seekbar_style_desc),
        options = mapOf(
            SeekbarStyle.WAVY to stringResource(Res.string.setcat_seekbar_wavy),
            SeekbarStyle.WAVEFORM to stringResource(Res.string.setcat_seekbar_waveform)
        ),
        selectedKey = style,
        onSelectionChanged = { viewModel.setStyle(it) },
        leadingIcon = {
            Icon(painterResource(Res.drawable.rounded_graphic_eq_24), null, tint = MaterialTheme.colorScheme.secondary)
        },
        modifier = Modifier.settingHighlight("item_appearance_seekbar_style", highlightKey)
    )
}
