package com.br.androideventscheduler.core.ui.component

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.model.StickerEmoji
import com.br.androideventscheduler.core.testing.pressBackOnLatestDialog
import com.br.androideventscheduler.core.ui.SemanticsTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR-w411dp-h891dp")
class StickerPickerDialogRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val selections = mutableListOf<String?>()
    private var dismissCalls = 0

    private fun show(selected: String? = null) {
        composeRule.setContent {
            AndroidEventSchedulerTheme {
                StickerPickerDialog(
                    selectedEmoji = selected,
                    onStickerSelected = { selections += it },
                    onDismiss = { dismissCalls++ },
                )
            }
        }
    }

    private fun option(sticker: StickerEmoji) = composeRule.onNodeWithTag(SemanticsTags.STICKER_OPTION_PREFIX + sticker.name)

    @Test
    fun `given the dialog when shown then eight radio options have their pt-BR label`() {
        show()

        composeRule.onNodeWithTag(SemanticsTags.STICKER_PICKER_DIALOG).assertExists()
        val expected =
            listOf(
                "Rosto sorridente",
                "Livros de estudo",
                "Computador e programação",
                "Foguete",
                "Estrela",
                "Comemoração",
                "Treino",
                "Pausa para café",
            )
        assertEquals(8, StickerEmoji.entries.size)
        StickerEmoji.entries.zip(expected).forEach { (sticker, label) ->
            option(sticker)
                .assertContentDescriptionEquals(label)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        }
    }

    @Test
    fun `given a selected sticker when shown then only that option is selected`() {
        show(selected = StickerEmoji.ROCKET.emoji)

        option(StickerEmoji.ROCKET).assertIsSelected()
        option(StickerEmoji.STAR).assertIsNotSelected()
    }

    @Test
    fun `given the dialog when choosing an option then the emoji is returned`() {
        show()

        option(StickerEmoji.PARTY).performClick()

        assertEquals(listOf<String?>("🎉"), selections)
    }

    @Test
    fun `given no sticker selected when shown then the clear button is not offered`() {
        show()

        composeRule.onAllNodesWithTag(SemanticsTags.STICKER_CLEAR_BUTTON).assertCountEquals(0)
    }

    @Test
    fun `given a selected sticker when clearing then null is returned`() {
        show(selected = StickerEmoji.COFFEE.emoji)

        composeRule.onNodeWithTag(SemanticsTags.STICKER_CLEAR_BUTTON).performClick()

        assertEquals(listOf<String?>(null), selections)
    }

    @Test
    fun `given the dialog when dismissed without choosing then nothing is selected`() {
        show(selected = StickerEmoji.STAR.emoji)

        composeRule.runOnUiThread { pressBackOnLatestDialog() }
        composeRule.waitForIdle()

        assertTrue(selections.isEmpty())
        assertEquals(1, dismissCalls)
    }
}
