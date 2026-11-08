package com.br.androideventscheduler.feature.alarm.presentation.screen

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.alarm.presentation.event.FullScreenAlarmUiEvent
import com.br.androideventscheduler.feature.alarm.presentation.state.FullScreenAlarmUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR")
class FullScreenAlarmScreenRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val events = mutableListOf<FullScreenAlarmUiEvent>()

    private val content =
        FullScreenAlarmUiState.Content(
            title = "Aula de Kotlin",
            timeText = "22:00 – 23:30",
            location = "Sala 3",
            notes = "Levar o notebook",
            colorHex = 0xFF5C6BC0,
            minutesUntilStart = 15,
        )

    private fun setContent(uiState: FullScreenAlarmUiState) {
        composeRule.setContent {
            AndroidEventSchedulerTheme { FullScreenAlarmContent(uiState = uiState, onEvent = { events += it }) }
        }
    }

    private fun tagExists(tag: String): Boolean = composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun topOf(tag: String): Float =
        composeRule
            .onNodeWithTag(tag)
            .fetchSemanticsNode()
            .positionInRoot.y

    @Test
    fun `given content when rendered then every field shows its text under its tag`() {
        setContent(content)

        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_BADGE).assertIsDisplayed().assertTextContains("Evento em 15 minutos")
        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_TITLE).assertTextContains("Aula de Kotlin")
        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_TIME).assertTextContains("22:00 – 23:30")
        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_LOCATION).assertTextContains("Sala 3")
        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_NOTES).assertTextContains("Levar o notebook")
    }

    @Test
    fun `given one minute left when rendered then the badge uses the singular`() {
        setContent(content.copy(minutesUntilStart = 1))

        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_BADGE).assertTextContains("Evento em 1 minuto")
    }

    @Test
    fun `given event that already started when rendered then the badge says now`() {
        setContent(content.copy(minutesUntilStart = 0))

        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_BADGE).assertTextContains("Evento agora")
    }

    @Test
    fun `given no location and no notes when rendered then those rows are absent`() {
        setContent(content.copy(location = null, notes = null))

        assertTrue(!tagExists(SemanticsTags.ALARM_FULLSCREEN_LOCATION))
        assertTrue(!tagExists(SemanticsTags.ALARM_FULLSCREEN_NOTES))
        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_TIME).assertIsDisplayed()
    }

    @Test
    fun `given content when rendered then the info rows are read with their labels`() {
        setContent(content)

        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_TIME).assertContentDescriptionEquals("Horário: 22:00 – 23:30")
        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_LOCATION).assertContentDescriptionEquals("Local: Sala 3")
        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_NOTES).assertContentDescriptionEquals("Notas: Levar o notebook")
    }

    @Test
    fun `given content when rendered then chips are radio buttons with the default selected and descriptions`() {
        setContent(content.copy(selectedSnoozeMinutes = 10))

        val isRadioButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        listOf(5, 10, 15).forEach { minutes ->
            composeRule
                .onNodeWithTag(SemanticsTags.ALARM_SNOOZE_CHIP_PREFIX + minutes)
                .assert(isRadioButton)
                .assertContentDescriptionEquals(
                    context.resources.getQuantityString(R.plurals.alarm_snooze_chip_description, minutes, minutes),
                ).assertHeightIsAtLeast(48.dp)
        }
        composeRule.onNodeWithTag(SemanticsTags.ALARM_SNOOZE_CHIP_PREFIX + 10).assertIsSelected()
        composeRule.onNodeWithTag(SemanticsTags.ALARM_SNOOZE_CHIP_PREFIX + 5).assertIsNotSelected()
        composeRule.onNodeWithTag(SemanticsTags.ALARM_SNOOZE_CHIP_PREFIX + 15).assertIsNotSelected()
    }

    @Test
    fun `given a chip when clicked then emits the selected duration`() {
        setContent(content)

        composeRule.onNodeWithTag(SemanticsTags.ALARM_SNOOZE_CHIP_PREFIX + 15).performClick()

        assertEquals(listOf(FullScreenAlarmUiEvent.OnSnoozeDurationSelected(15)), events)
    }

    @Test
    fun `given content when rendered then the buttons have the accessibility descriptions of the selected duration`() {
        setContent(content.copy(selectedSnoozeMinutes = 5))

        composeRule
            .onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_SNOOZE_BUTTON)
            .assertContentDescriptionEquals("Adiar lembrete do evento por 5 minutos")
            .assertHeightIsAtLeast(56.dp)
        composeRule
            .onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_DISMISS_BUTTON)
            .assertContentDescriptionEquals("Dispensar lembrete do evento")
            .assertHeightIsAtLeast(56.dp)
    }

    @Test
    fun `given buttons when clicked then emit snooze and dismiss events`() {
        setContent(content)

        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_SNOOZE_BUTTON).performScrollTo().performClick()
        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_DISMISS_BUTTON).performScrollTo().performClick()

        assertEquals(listOf(FullScreenAlarmUiEvent.OnSnoozeClicked, FullScreenAlarmUiEvent.OnDismissClicked), events)
    }

    @Test
    fun `given processing when rendered then chips and buttons are disabled`() {
        setContent(content.copy(isProcessing = true))

        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_SNOOZE_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_DISMISS_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(SemanticsTags.ALARM_SNOOZE_CHIP_PREFIX + 5).assertIsNotEnabled()
    }

    @Test
    fun `given idle content when rendered then chips and buttons are enabled`() {
        setContent(content)

        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_SNOOZE_BUTTON).assertIsEnabled()
        composeRule.onNodeWithTag(SemanticsTags.ALARM_SNOOZE_CHIP_PREFIX + 5).assertIsEnabled()
    }

    @Test
    fun `given error message when rendered then it is shown and absent otherwise`() {
        setContent(content.copy(errorMessageRes = R.string.alarm_snooze_error))

        composeRule
            .onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_ERROR)
            .assertIsDisplayed()
            .assertTextContains(context.getString(R.string.alarm_snooze_error))
    }

    @Test
    fun `given no error when rendered then the error text is absent`() {
        setContent(content)

        assertTrue(!tagExists(SemanticsTags.ALARM_FULLSCREEN_ERROR))
    }

    @Test
    fun `given loading state when rendered then only the progress indicator is shown`() {
        setContent(FullScreenAlarmUiState.Loading)

        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_LOADING).assertIsDisplayed()
        assertTrue(!tagExists(SemanticsTags.ALARM_FULLSCREEN_SNOOZE_BUTTON))
    }

    @Test
    fun `given content when rendered then reading order is badge title time location notes chips and buttons`() {
        setContent(content)

        val orderedTags =
            listOf(
                SemanticsTags.ALARM_FULLSCREEN_BADGE,
                SemanticsTags.ALARM_FULLSCREEN_TITLE,
                SemanticsTags.ALARM_FULLSCREEN_TIME,
                SemanticsTags.ALARM_FULLSCREEN_LOCATION,
                SemanticsTags.ALARM_FULLSCREEN_NOTES,
                SemanticsTags.ALARM_SNOOZE_CHIPS,
                SemanticsTags.ALARM_FULLSCREEN_SNOOZE_BUTTON,
                SemanticsTags.ALARM_FULLSCREEN_DISMISS_BUTTON,
            )
        val tops = orderedTags.map(::topOf)
        assertEquals(tops.sorted(), tops)
        assertEquals(tops.size, tops.toSet().size)
    }

    @Test
    @Config(qualifiers = "pt-rBR-w800dp-h360dp-land")
    fun `given landscape when rendered then info and actions are side by side and all visible`() {
        setContent(content)

        val infoLeft =
            composeRule
                .onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_TITLE)
                .fetchSemanticsNode()
                .boundsInRoot.left
        val actionsLeft =
            composeRule
                .onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_SNOOZE_BUTTON)
                .fetchSemanticsNode()
                .boundsInRoot.left
        assertTrue("actions should be to the right of the info", actionsLeft > infoLeft)
        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_SNOOZE_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_DISMISS_BUTTON).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp", fontScale = 2f)
    fun `given large font when rendered then the buttons stay reachable by scrolling`() {
        setContent(content)

        composeRule
            .onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_SNOOZE_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertEquals(listOf(FullScreenAlarmUiEvent.OnSnoozeClicked), events)
    }
}
