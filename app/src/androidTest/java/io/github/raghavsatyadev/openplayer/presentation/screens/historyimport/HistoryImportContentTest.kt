package io.github.raghavsatyadev.openplayer.presentation.screens.historyimport

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.history.importer.HistoryImportState
import io.github.raghavsatyadev.openplayer.data.history.importer.HistoryImportSummary
import io.github.raghavsatyadev.openplayer.data.history.importer.HistorySource
import io.github.raghavsatyadev.openplayer.data.history.importer.UnmatchedTrack
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The import sheet's idle, working and summary states with made-up state, plus a light and a dark
 * picture of the idle and summary states saved to the app's files folder.
 */
@RunWith(AndroidJUnit4::class)
class HistoryImportContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val summary = HistoryImportSummary(
        imported = 4210,
        duplicates = 312,
        unmatched = 1893,
        belowRule = 540,
        unparseable = 3,
        unmatchedSample = listOf(
            UnmatchedTrack("Daft Punk - Digital Love", 41),
            UnmatchedTrack("Radiohead - Weird Fishes/ Arpeggi", 27),
            UnmatchedTrack("Bonobo - Kerala", 19)
        )
    )

    private var state: HistoryImportState by mutableStateOf(HistoryImportState.Idle)
    private val picked = mutableListOf<HistorySource>()
    private var finished = 0

    private fun show(dark: Boolean) {
        composeRule.setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface {
                    HistoryImportContent(
                        state = state,
                        onPick = { picked += it },
                        onFinish = { finished++ },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    @Test
    fun idleShowsFourSourcesAndTapsPickThem() {
        show(dark = false)
        composeRule.onNodeWithText("Last.fm CSV").assertExists()
        composeRule.onNodeWithText("Spotify streaming history").assertExists()
        composeRule.onNodeWithText("YouTube Music (Google Takeout)").assertExists()
        composeRule.onNodeWithText("ListenBrainz export").performClick()
        composeRule.onNodeWithText("Last.fm CSV").performClick()
        composeRule.onNodeWithText("YouTube Music (Google Takeout)").performClick()
        composeRule.runOnIdle {
            assertThat(picked).containsExactly(
                HistorySource.LISTENBRAINZ,
                HistorySource.LASTFM,
                HistorySource.YOUTUBE_MUSIC
            ).inOrder()
        }
    }

    @Test
    fun stateChangesAnimateBetweenIdleWorkingAndSummary() {
        show(dark = false)
        state = HistoryImportState.Working(HistorySource.SPOTIFY, HistoryImportState.Phase.READING, 12_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Reading files").assertExists()
        composeRule.onNodeWithText("12000 plays read").assertExists()

        state = HistoryImportState.Done(HistorySource.SPOTIFY, summary)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Imported 4210 plays").assertExists()
        composeRule.onNodeWithText("Daft Punk - Digital Love (41)").assertExists()
        composeRule.onNodeWithText("Done").performClick()
        composeRule.runOnIdle { assertThat(finished).isEqualTo(1) }
    }

    @Test
    fun lightPictures() = pictures(dark = false)

    @Test
    fun darkPictures() = pictures(dark = true)

    private fun pictures(dark: Boolean) {
        show(dark)
        composeRule.waitForIdle()
        save(if (dark) "history_import_dark.png" else "history_import_light.png")
        state = HistoryImportState.Done(HistorySource.LASTFM, summary)
        composeRule.waitForIdle()
        save(if (dark) "history_import_summary_dark.png" else "history_import_summary_light.png")
    }

    private fun save(name: String) {
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
