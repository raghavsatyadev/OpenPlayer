package io.github.raghavsatyadev.openplayer.presentation.screens.historyimport

import android.net.Uri
import androidx.lifecycle.ViewModel
import io.github.raghavsatyadev.openplayer.data.history.importer.ExternalHistoryImporter
import io.github.raghavsatyadev.openplayer.data.history.importer.HistoryImportState
import io.github.raghavsatyadev.openplayer.data.history.importer.HistorySource
import io.github.raghavsatyadev.openplayer.di.AppScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Runs imports on the app scope, so closing the sheet doesn't cancel one. */
@HiltViewModel
class HistoryImportViewModel @Inject constructor(
    private val importer: ExternalHistoryImporter,
    @AppScope private val appScope: CoroutineScope
) : ViewModel() {

    val state: StateFlow<HistoryImportState> = importer.state

    fun import(source: HistorySource, uris: List<Uri>) {
        if (uris.isEmpty()) return
        appScope.launch { importer.import(source, uris) }
    }

    fun reset() = importer.reset()
}
