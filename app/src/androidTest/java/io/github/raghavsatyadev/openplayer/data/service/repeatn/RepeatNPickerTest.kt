package io.github.raghavsatyadev.openplayer.data.service.repeatn

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.presentation.components.player.RepeatNPickerContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** The repeat-N picker with fake state: choosing a count, the stop action and light/dark pictures. */
@RunWith(AndroidJUnit4::class)
class RepeatNPickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var remaining by mutableIntStateOf(0)
    private val picked = mutableListOf<Int>()
    private var stops = 0

    private fun setPicker(dark: Boolean) {
        composeRule.setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface {
                    RepeatNPickerContent(
                        remaining = remaining,
                        onPick = { picked += it },
                        onStop = { stops++ },
                    )
                }
            }
        }
    }

    @Test
    fun pickingACountReportsIt() {
        setPicker(dark = false)
        composeRule.onNodeWithText("Stop repeating").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Repeat 5 more times").performClick()
        composeRule.onNodeWithText("Repeat").performClick()
        composeRule.runOnIdle { assertThat(picked).containsExactly(5) }
    }

    @Test
    fun stopAppearsWhileACountRuns() {
        remaining = 3
        setPicker(dark = false)
        composeRule.onNodeWithContentDescription("3 repeats left").assertExists()
        composeRule.onNodeWithText("Stop repeating").performClick()
        composeRule.runOnIdle { assertThat(stops).isEqualTo(1) }
    }

    @Test
    fun lightPicture() = picture(dark = false)

    @Test
    fun darkPicture() = picture(dark = true)

    private fun picture(dark: Boolean) {
        remaining = 4
        setPicker(dark)
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "repeat_n_${if (dark) "dark" else "light"}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
