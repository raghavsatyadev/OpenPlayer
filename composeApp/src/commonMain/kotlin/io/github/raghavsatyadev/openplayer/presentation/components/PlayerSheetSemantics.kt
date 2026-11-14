package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerSheetState

/**
 * Test tags of the player sheet card, exposed to UiAutomator as its resource id. They stay the same
 * in every language, so the baseline profile benchmarks wait on them instead of the translated
 * [playerSheetContentDescription]. PlayerSheetAnimationBenchmarks repeats these values.
 */
internal const val PLAYER_SHEET_COLLAPSED_TEST_TAG = "player_sheet_collapsed"
internal const val PLAYER_SHEET_EXPANDED_TEST_TAG = "player_sheet_expanded"

internal fun playerSheetTestTag(state: PlayerSheetState): String =
    when (state) {
        PlayerSheetState.COLLAPSED -> PLAYER_SHEET_COLLAPSED_TEST_TAG
        PlayerSheetState.EXPANDED -> PLAYER_SHEET_EXPANDED_TEST_TAG
    }

/**
 * TalkBack description of the player sheet card, naming [songTitle] when there is one.
 *
 * Resolved through Compose resources so it follows the in-app language.
 */
@Composable
internal fun playerSheetContentDescription(
    state: PlayerSheetState,
    songTitle: String?
): String {
    val title = songTitle?.takeIf { it.isNotBlank() }
    val descriptionRes = playerSheetContentDescriptionRes(state, hasTitle = title != null)
    return if (title != null) {
        stringResource(descriptionRes, title)
    } else {
        stringResource(descriptionRes)
    }
}

/** The variants with a title take it as their only format argument. */
internal fun playerSheetContentDescriptionRes(state: PlayerSheetState, hasTitle: Boolean): StringResource =
    when (state) {
        PlayerSheetState.COLLAPSED ->
            if (hasTitle) Res.string.cd_player_sheet_collapsed else Res.string.cd_player_sheet_collapsed_no_track
        PlayerSheetState.EXPANDED ->
            if (hasTitle) Res.string.cd_player_sheet_expanded else Res.string.cd_player_sheet_expanded_no_track
    }
