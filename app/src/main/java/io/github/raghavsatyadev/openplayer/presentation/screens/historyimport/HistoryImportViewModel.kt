package io.github.raghavsatyadev.openplayer.presentation.screens.historyimport

import android.net.Uri
import androidx.lifecycle.ViewModel
import io.github.raghavsatyadev.openplayer.data.history.importer.ExternalHistoryImporter
import io.github.raghavsatyadev.openplayer.data.history.importer.HistoryImportState
import io.github.raghavsatyadev.openplayer.data.history.importer.HistorySource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Runs imports on the app scope, so closing the sheet doesn't cancel one. */
class HistoryImportViewModel(
    private val importer: ExternalHistoryImporter,
    private val appScope: CoroutineScope
) : ViewModel() {

    val state: StateFlow<HistoryImportState> = importer.state

    fun import(source: HistorySource, uris: List<Uri>) {
        if (uris.isEmpty()) return
        appScope.launch { importer.import(source, uris) }
    }

    fun reset() = importer.reset()
}
