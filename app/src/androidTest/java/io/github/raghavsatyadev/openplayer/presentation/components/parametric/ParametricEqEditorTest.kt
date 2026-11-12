package io.github.raghavsatyadev.openplayer.presentation.components.parametric

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
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.AutoEqParser
import io.github.raghavsatyadev.openplayer.data.equalizer.parametric.ParametricBand
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ParametricEqUiState
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ParametricMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** The parametric editor and the mode switch with made-up state; also saves light and dark pictures. */
@RunWith(AndroidJUnit4::class)
class ParametricEqEditorTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var state by mutableStateOf(
        ParametricEqUiState(
            enabled = true,
            profile = AutoEqParser.parse(
                "Preamp: -6.2 dB\n" +
                    "Filter 1: ON LSC Fc 105 Hz Gain 5.5 dB Q 0.70\n" +
                    "Filter 2: ON PK Fc 2210 Hz Gain -3.1 dB Q 1.41\n" +
                    "Filter 3: OFF PK Fc 5000 Hz Gain 2.0 dB Q 2.00\n" +
                    "Filter 4: ON HSC Fc 10000 Hz Gain -2.0 dB Q 0.70",
            ),
        )
    )
    private var added = 0
    private var removed = -1
    private var modeParametric by mutableStateOf(true)

    private fun setEditor(dark: Boolean) {
        val actions = ParametricEqActions(
            onPreampChange = {},
            onAddBand = { added++ },
            onBandChange = { _, _ -> },
            onRemoveBand = { removed = it },
            onClear = {},
            onImportClick = {},
            onExportClick = {},
        )
        composeRule.setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface {
                    LazyColumn(Modifier.fillMaxSize()) {
                        item { EqualizerModeSwitch(modeParametric, onParametricChange = { modeParametric = it }) }
                        item { ParametricEqEditor(state, actions) }
                    }
                }
            }
        }
    }

    @Test
    fun showsBandsAndHandlesButtons() {
        setEditor(dark = false)
        composeRule.onNodeWithText("Band 1").assertExists()
        composeRule.onNodeWithText("Low shelf").assertExists()
        composeRule.onNodeWithText("Add band").performScrollTo().performClick()
        composeRule.runOnIdle { assertThat(added).isEqualTo(1) }
        composeRule.onNode(hasScrollAction()).performScrollToIndex(0)
        composeRule.onNodeWithText("Graphic").performClick()
        composeRule.runOnIdle { assertThat(modeParametric).isFalse() }
    }

    @Test
    fun showsErrorMessage() {
        setEditor(dark = false)
        composeRule.onNodeWithText("No valid filters found in that file.").assertDoesNotExist()
        state = state.copy(message = ParametricMessage.IMPORT_NO_FILTERS)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No valid filters found in that file.").assertExists()
    }

    @Test
    fun emptyProfileShowsHint() {
        state = state.copy(profile = state.profile.copy(bands = emptyList<ParametricBand>()))
        setEditor(dark = false)
        composeRule.onNodeWithText("No bands yet. Add one or import an AutoEQ file.").assertExists()
    }

    @Test
    fun lightPicture() = picture(dark = false)

    @Test
    fun darkPicture() = picture(dark = true)

    private fun picture(dark: Boolean) {
        setEditor(dark)
        composeRule.mainClock.advanceTimeBy(1000)
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "parametric_eq_${if (dark) "dark" else "light"}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
