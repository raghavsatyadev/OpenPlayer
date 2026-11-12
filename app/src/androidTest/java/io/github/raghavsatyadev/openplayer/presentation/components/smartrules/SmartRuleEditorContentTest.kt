package io.github.raghavsatyadev.openplayer.presentation.components.smartrules

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartCondition
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartMatchMode
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleField
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleOperator
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartSort
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartSortField
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The rule editor with a made-up state: rows are added and removed, the preview and Save follow
 * the state. Also saves a light and a dark picture of it to the app's files folder.
 */
@RunWith(AndroidJUnit4::class)
class SmartRuleEditorContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var state by mutableStateOf(
        SmartRuleEditorState(
            name = "Gym favourites",
            matchMode = SmartMatchMode.ALL,
            conditions = listOf(
                SmartCondition(1, SmartRuleField.TAG, SmartRuleOperator.HAS, text = "gym"),
                SmartCondition(2, SmartRuleField.RATING, SmartRuleOperator.AT_LEAST, number = 4),
                SmartCondition(3, SmartRuleField.LAST_PLAYED, SmartRuleOperator.NOT_WITHIN_DAYS, number = 30),
            ),
            sort = SmartSort(SmartSortField.PLAY_COUNT, descending = true),
            limitText = "50",
        )
    )
    private var previewCount by mutableStateOf<Int?>(42)
    private var saved = 0
    private var cancelled = 0

    private fun setEditor(dark: Boolean) {
        composeRule.setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface {
                    SmartRuleEditorContent(
                        state = state,
                        previewCount = previewCount,
                        onStateChange = { state = it },
                        onSave = { saved++ },
                        onCancel = { cancelled++ },
                    )
                }
            }
        }
    }

    private fun removeButtons() = composeRule.onAllNodesWithContentDescription("Remove condition")

    @Test
    fun showsTheStateAndFollowsThePreview() {
        setEditor(dark = false)
        composeRule.onNodeWithText("42 songs match").assertExists()
        composeRule.onNodeWithText("Gym favourites").assertExists()
        composeRule.onNodeWithText("Gym favourites").assertExists()

        previewCount = 1
        composeRule.waitForIdle()
        composeRule.onNodeWithText("1 song matches").assertExists()

        previewCount = null
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Counting songs").assertExists()
    }


    fun addAndRemoveConditionRows() {
        // One row keeps every control on screen, so none is left out of the lazy list.
        state = state.copy(conditions = state.conditions.take(1))
        setEditor(dark = false)
        composeRule.onNodeWithText("Add condition").performClick()
        composeRule.waitForIdle()
        assertThat(state.conditions).hasSize(2)
        // The new, empty text row isn't complete, so Save waits for its value.
        composeRule.onNodeWithText("Save").assertIsNotEnabled()

        removeButtons()[1].performClick()
        composeRule.waitForIdle()
        assertThat(state.conditions.map { it.id }).containsExactly(1L)
        composeRule.onNodeWithText("Save").assertIsEnabled()

        removeButtons()[0].performClick()
        composeRule.waitForIdle()
        assertThat(state.conditions).isEmpty()
        composeRule.onNodeWithText("No conditions yet: every song matches.").assertExists()
    }

    @Test
    fun saveAndCancelCallBack() {
        setEditor(dark = false)
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.runOnIdle {
            assertThat(saved).isEqualTo(1)
            assertThat(cancelled).isEqualTo(1)
            assertThat(state.toDefinition().limit).isEqualTo(50)
            assertThat(state.toDefinition().conditions).hasSize(3)
        }
    }

    @Test
    fun saveIsDisabledWithoutANameOrConditions() {
        state = state.copy(name = "")
        setEditor(dark = false)
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
        state = state.copy(name = "x", conditions = emptyList())
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
        composeRule.onNodeWithText("No conditions yet: every song matches.").assertExists()
    }

    @Test
    fun lightPicture() = picture(dark = false)

    @Test
    fun darkPicture() = picture(dark = true)

    private fun picture(dark: Boolean) {
        setEditor(dark)
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "smart_rules_${if (dark) "dark" else "light"}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
