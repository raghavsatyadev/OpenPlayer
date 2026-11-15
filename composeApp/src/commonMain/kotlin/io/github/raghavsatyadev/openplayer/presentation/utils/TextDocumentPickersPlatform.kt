package io.github.raghavsatyadev.openplayer.presentation.utils

import androidx.compose.runtime.Composable

/** Launches the system "open a text file" / "save a text file" pickers. Results are Uri strings. */
interface TextDocumentPickers {
    fun launchOpen()
    fun launchCreate(suggestedName: String)
}

/** [onOpened] / [onCreated] get the chosen document's Uri string; they are not called on cancel. */
@Composable
expect fun rememberTextDocumentPickers(
    onOpened: (uri: String) -> Unit,
    onCreated: (uri: String) -> Unit,
): TextDocumentPickers
