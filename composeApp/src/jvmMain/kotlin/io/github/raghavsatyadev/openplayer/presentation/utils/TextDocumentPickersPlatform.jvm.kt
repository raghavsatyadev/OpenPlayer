package io.github.raghavsatyadev.openplayer.presentation.utils

import androidx.compose.runtime.Composable

private val NoOpPickers = object : TextDocumentPickers {
    override fun launchOpen() = Unit
    override fun launchCreate(suggestedName: String) = Unit
}

@Composable
actual fun rememberTextDocumentPickers(
    onOpened: (uri: String) -> Unit,
    onCreated: (uri: String) -> Unit,
): TextDocumentPickers = NoOpPickers
