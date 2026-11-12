package io.github.raghavsatyadev.openplayer.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
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
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.HistoryPicksState
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SeedPickMode
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ThemeStateHolder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The Home history sections with a made-up state: each shows its songs, and the chips switch what
 * a section shows. Also saves a light and a dark picture of them to the app's files folder.
 */
@RunWith(AndroidJUnit4::class)
class HistoryPickSectionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun song(id: String) = Song.emptySong().copy(id = id, title = "Song $id", artist = "Artist $id")

    private val seed = song("seed")
    private var state by mutableStateOf(
        HistoryPicksState(
            timeOfDay = listOf(song("t1"), song("t2")),
            seed = seed,
            seedPicks = linkedMapOf(
                SeedPickMode.ALONGSIDE to listOf(song("a1"), song("a2"), song("a3")),
                SeedPickMode.ERA to listOf(song("e1"), song("e2"), song("e3")),
            ),
            selectedMode = SeedPickMode.ALONGSIDE,
            lostMemories = linkedMapOf(2024 to listOf(song("l1")), 2023 to listOf(song("l2"))),
            selectedYear = 2024,
        )
    )
    private val played = mutableListOf<String>()

    private fun setSections(dark: Boolean) {
        val themeStateHolder = mockk<ThemeStateHolder>(relaxed = true) {
            every { getAlbumColorSchemeFlow(any(), any()) } returns MutableStateFlow(null)
        }
        val actions = HistoryPickActions(
            play = { _, start, _ -> played += start.id },
            onSongLongClick = {},
            selectYear = { state = state.copy(selectedYear = it) },
            selectMode = { state = state.copy(selectedMode = it) },
        )
        composeRule.setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface {
                    LazyColumn(Modifier.fillMaxSize()) {
                        historyPickSections(state, actions, themeStateHolder, currentSongId = null)
                    }
                }
            }
        }
    }

    @Test
    fun chipsSwitchTheSongsAndTapsPlayThem() {
        setSections(dark = false)
        composeRule.onNodeWithText("Song t1").assertExists()
        composeRule.onNodeWithText("Song a1").assertExists()
        composeRule.onNodeWithText("Song l1").assertExists()

        composeRule.onNodeWithText("Same era").performClick()
        composeRule.onNodeWithText("2023").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Song e1").assertExists()
        composeRule.onNodeWithText("Song a1").assertDoesNotExist()
        composeRule.onNodeWithText("Song l2").assertExists()

        composeRule.onNodeWithText("Song e2").performClick()
        composeRule.runOnIdle { assertThat(played).containsExactly("e2") }
    }

    @Test
    fun lightPicture() = picture(dark = false)

    @Test
    fun darkPicture() = picture(dark = true)

    private fun picture(dark: Boolean) {
        setSections(dark)
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "history_picks_${if (dark) "dark" else "light"}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
