package io.github.raghavsatyadev.openplayer.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.repository.SongUserMeta
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The rating, moods and tags editor with a made-up state: stars set and clear the rating, chips
 * are added from suggestions or typed, and Save hands back the result. Also saves a light and a
 * dark picture of it to the app files folder.
 */
@RunWith(AndroidJUnit4::class)
class SongUserMetaEditorTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var saved: SongUserMeta? = null

    private fun setEditor(dark: Boolean, initial: SongUserMeta) {
        saved = null
        composeRule.setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxWidth()) {
                    SongUserMetaEditorContent(
                        songTitle = "Midnight City",
                        initial = initial,
                        moodSuggestions = listOf("Calm", "Happy", "Focus"),
                        tagSuggestions = listOf("gym", "road trip"),
                        onSave = { saved = it },
                        onCancel = {}
                    )
                }
            }
        }
    }

    @Test
    fun starsMoodsAndTagsAreEditedAndSaved() {
        setEditor(dark = false, initial = SongUserMeta("7", rating = 2, moods = listOf("Calm")))

        composeRule.onNodeWithContentDescription("Set rating to 4 stars").performClick()
        composeRule.onNodeWithText("4 of 5 stars").assertExists()

        composeRule.onNodeWithText("Happy").performClick()
        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("night")
        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Save").performClick()
        composeRule.runOnIdle {
            val result = saved!!
            assertThat(result.songId).isEqualTo("7")
            assertThat(result.rating).isEqualTo(4)
            assertThat(result.moods).containsExactly("Calm", "Happy").inOrder()
            assertThat(result.tags).containsExactly("night")
        }
    }

    @Test
    fun tappingTheCurrentStarClearsTheRating() {
        setEditor(dark = false, initial = SongUserMeta("7", rating = 3))

        composeRule.onNodeWithContentDescription("Set rating to 3 stars").performClick()
        composeRule.onNodeWithText("Not rated").assertExists()
    }

    @Test
    fun lightPicture() = picture(dark = false)

    @Test
    fun darkPicture() = picture(dark = true)

    private fun picture(dark: Boolean) {
        setEditor(
            dark,
            SongUserMeta("7", rating = 4, moods = listOf("Calm", "Focus"), tags = listOf("gym"))
        )
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "song_user_meta_${if (dark) "dark" else "light"}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
