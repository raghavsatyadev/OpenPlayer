package io.github.raghavsatyadev.openplayer.presentation.utils

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun rememberTextDocumentPickers(
    onOpened: (uri: String) -> Unit,
    onCreated: (uri: String) -> Unit,
): TextDocumentPickers {
    val currentOnOpened = rememberUpdatedState(onOpened)
    val currentOnCreated = rememberUpdatedState(onCreated)
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) currentOnOpened.value(uri.toString())
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) currentOnCreated.value(uri.toString())
    }
    return remember(importLauncher, exportLauncher) {
        object : TextDocumentPickers {
            override fun launchOpen() = importLauncher.launch(arrayOf("text/plain", "*/*"))
            override fun launchCreate(suggestedName: String) = exportLauncher.launch(suggestedName)
        }
    }
}
