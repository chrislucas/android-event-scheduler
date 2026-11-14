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
import com.br.androideventscheduler.core.schedule.domain.model.EventColorOption
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
class ColorPickerDialogRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val selections = mutableListOf<Long>()
    private var dismissCalls = 0

    private fun show(selectedHex: Long = EventColorOption.LIGHT_BLUE.hex) {
        composeRule.setContent {
            AndroidEventSchedulerTheme {
                ColorPickerDialog(
                    selectedColorHex = selectedHex,
                    onColorSelected = { selections += it },
                    onDismiss = { dismissCalls++ },
                )
            }
        }
    }

    private fun option(color: EventColorOption) = composeRule.onNodeWithTag(SemanticsTags.COLOR_OPTION_PREFIX + color.name)

    @Test
    fun `given the dialog when shown then six radio options carry the color name`() {
        show()

        composeRule.onNodeWithTag(SemanticsTags.COLOR_PICKER_DIALOG).assertExists()
        val names = listOf("Azul claro", "Índigo", "Verde-azulado", "Verde oliva", "Roxo", "Coral")
        assertEquals(6, EventColorOption.entries.size)
        EventColorOption.entries.zip(names).forEach { (color, name) ->
            option(color)
                .assertContentDescriptionEquals(name)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        }
    }

    @Test
    fun `given a selected color when shown then only it is selected and shows the check mark`() {
        show(selectedHex = EventColorOption.PURPLE.hex)

        option(EventColorOption.PURPLE).assertIsSelected()
        option(EventColorOption.CORAL).assertIsNotSelected()
        composeRule
            .onAllNodesWithTag(
                SemanticsTags.COLOR_OPTION_CHECK_PREFIX + EventColorOption.PURPLE.name,
                useUnmergedTree = true,
            ).assertCountEquals(1)
        EventColorOption.entries.filter { it != EventColorOption.PURPLE }.forEach { other ->
            composeRule.onAllNodesWithTag(SemanticsTags.COLOR_OPTION_CHECK_PREFIX + other.name, useUnmergedTree = true).assertCountEquals(0)
        }
    }

    @Test
    fun `given a hex outside the catalog when shown then the default color is the selected one`() {
        show(selectedHex = 0xFF123456)

        option(EventColorOption.LIGHT_BLUE).assertIsSelected()
    }

    @Test
    fun `given the dialog when choosing a color then its hex is returned`() {
        show()

        option(EventColorOption.CORAL).performClick()

        assertEquals(listOf(EventColorOption.CORAL.hex), selections)
    }

    @Test
    fun `given the dialog when dismissed without choosing then nothing is selected`() {
        show()

        composeRule.runOnUiThread { pressBackOnLatestDialog() }
        composeRule.waitForIdle()

        assertTrue(selections.isEmpty())
        assertEquals(1, dismissCalls)
    }
}
