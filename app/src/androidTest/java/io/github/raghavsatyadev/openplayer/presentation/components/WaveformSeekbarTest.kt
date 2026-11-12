package io.github.raghavsatyadev.openplayer.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.raghavsatyadev.openplayer.presentation.components.player.ChapterLabel
import io.github.raghavsatyadev.openplayer.presentation.components.player.SeekbarChapter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** The waveform seekbar with a made-up waveform and two chapter markers, in light and dark. */
@RunWith(AndroidJUnit4::class)
class WaveformSeekbarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val waveform = FloatArray(160) { i ->
        (0.25f + 0.75f * abs(sin(i * 2 * PI / 53).toFloat()) * (0.4f + 0.6f * i / 160f)).coerceIn(0f, 1f)
    }
    private val chapters = listOf(SeekbarChapter(30_000L, "Verse"), SeekbarChapter(120_000L, "Chorus"))
    private val durationMs = 180_000L
    private val position = mutableLongStateOf(50_000L)

    private fun setSeekbar(dark: Boolean) {
        composeRule.setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        WavySliderExpressive(
                            value = { position.longValue.toFloat() / durationMs },
                            onValueChange = {},
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            thumbColor = MaterialTheme.colorScheme.primary,
                            waveform = waveform,
                            chapterMarkers = chapters.map { it.timestampMs.toFloat() / durationMs },
                            markerColor = MaterialTheme.colorScheme.tertiary,
                        )
                        ChapterLabel(
                            chapters = chapters,
                            positionState = position,
                            textColor = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun showsCurrentChapterLabel() {
        setSeekbar(dark = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Verse").assertExists()
        composeRule.runOnIdle { position.longValue = 150_000L }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Chorus").assertExists()
    }

    @Test
    fun lightPicture() = picture(dark = false)

    @Test
    fun darkPicture() = picture(dark = true)

    private fun picture(dark: Boolean) {
        setSeekbar(dark)
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "waveform_seekbar_${if (dark) "dark" else "light"}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
