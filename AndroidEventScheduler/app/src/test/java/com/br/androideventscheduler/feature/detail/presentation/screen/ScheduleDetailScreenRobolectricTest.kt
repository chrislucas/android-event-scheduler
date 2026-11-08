package com.br.androideventscheduler.feature.detail.presentation.screen

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.detail.presentation.event.ScheduleDetailUiEvent
import com.br.androideventscheduler.feature.detail.presentation.state.ScheduleDetailUi
import com.br.androideventscheduler.feature.detail.presentation.state.ScheduleDetailUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR")
class ScheduleDetailScreenRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val events = mutableListOf<ScheduleDetailUiEvent>()

    private val fullSchedule =
        ScheduleDetailUi(
            id = "item-1",
            title = "Aula de Kotlin",
            stickerEmoji = "📚",
            colorHex = 0xFF5C6BC0,
            dateRangeText = "qui., 5 de nov., 22:00 → sex., 6 de nov., 23:30",
            calendarLabel = "ana@exemplo.com",
            calendarColorHex = 0xFF29B6F6,
            reminderText = "10 minutos antes",
            recurrenceText = "Não repetir",
            location = "Sala 3",
            notes = "Levar o notebook",
        )

    private fun setContent(uiState: ScheduleDetailUiState) {
        composeRule.setContent {
            AndroidEventSchedulerTheme { ScheduleDetailContent(uiState = uiState, onEvent = { events += it }) }
        }
    }

    private fun tagExists(tag: String): Boolean = composeRule.onAllNodesWithTagCount(tag) > 0

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTagCount(tag: String): Int =
        onAllNodes(
            androidx.compose.ui.test
                .hasTestTag(tag),
        ).fetchSemanticsNodes().size

    @Test
    fun `given content when rendered then every field shows its text under its tag`() {
        setContent(ScheduleDetailUiState.Content(fullSchedule))

        composeRule.onNodeWithTag(SemanticsTags.DETAIL_TITLE).assertIsDisplayed().assertTextContains("Aula de Kotlin")
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_DATE_RANGE).assertTextContains(fullSchedule.dateRangeText)
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_ACCOUNT_EMAIL).assertTextContains("ana@exemplo.com")
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_REMINDER_LABEL).assertTextContains("10 minutos antes")
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_RECURRENCE_LABEL).assertTextContains("Não repetir")
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_LOCATION).assertTextContains("Sala 3")
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_NOTES).assertTextContains("Levar o notebook")
    }

    @Test
    fun `given content when rendered then title is a heading`() {
        setContent(ScheduleDetailUiState.Content(fullSchedule))

        composeRule
            .onNodeWithTag(SemanticsTags.DETAIL_TITLE)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun `given content when rendered then each row is one node described as label colon text`() {
        setContent(ScheduleDetailUiState.Content(fullSchedule))

        composeRule.onNodeWithTag(SemanticsTags.DETAIL_REMINDER_LABEL).assertContentDescriptionEquals("Alerta: 10 minutos antes")
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_DATE_RANGE).assertContentDescriptionEquals("Data: ${fullSchedule.dateRangeText}")
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_ACCOUNT_EMAIL).assertContentDescriptionEquals("Calendário: ana@exemplo.com")
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_RECURRENCE_LABEL).assertContentDescriptionEquals("Repetição: Não repetir")
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_LOCATION).assertContentDescriptionEquals("Local: Sala 3")
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_NOTES).assertContentDescriptionEquals("Notas: Levar o notebook")
    }

    @Test
    fun `given missing location and notes when rendered then those rows do not exist`() {
        setContent(ScheduleDetailUiState.Content(fullSchedule.copy(location = null, notes = null)))

        composeRule.onNodeWithTag(SemanticsTags.DETAIL_LOCATION).assertDoesNotExist()
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_NOTES).assertDoesNotExist()
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_RECURRENCE_LABEL).assertIsDisplayed()
    }

    @Test
    fun `given content when rendered then edit button is a button with description and a large touch target`() {
        setContent(ScheduleDetailUiState.Content(fullSchedule))

        composeRule
            .onNodeWithTag(SemanticsTags.DETAIL_EDIT_BUTTON)
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertContentDescriptionEquals(context.getString(R.string.detail_edit_description))
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun `given content when edit is clicked then edit event is sent`() {
        setContent(ScheduleDetailUiState.Content(fullSchedule))

        composeRule.onNodeWithTag(SemanticsTags.DETAIL_EDIT_BUTTON).performClick()

        assertEquals(listOf<ScheduleDetailUiEvent>(ScheduleDetailUiEvent.OnEditClicked), events)
    }

    @Test
    fun `given any state when back arrow is clicked then back event is sent`() {
        setContent(ScheduleDetailUiState.Content(fullSchedule))

        composeRule
            .onNodeWithTag(SemanticsTags.DETAIL_BACK_BUTTON)
            .assertContentDescriptionEquals(context.getString(R.string.navigate_back))
            .performClick()

        assertEquals(listOf<ScheduleDetailUiEvent>(ScheduleDetailUiEvent.OnBackClicked), events)
    }

    @Test
    fun `given loading state when rendered then progress is shown and content and actions are absent`() {
        setContent(ScheduleDetailUiState.Loading)

        composeRule.onNodeWithTag(SemanticsTags.DETAIL_LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_TITLE).assertDoesNotExist()
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_EDIT_BUTTON).assertDoesNotExist()
    }

    @Test
    fun `given not found state when rendered then the message is shown without actions`() {
        setContent(ScheduleDetailUiState.NotFound)

        composeRule
            .onNodeWithTag(SemanticsTags.DETAIL_NOT_FOUND)
            .assertIsDisplayed()
            .assertTextContains(context.getString(R.string.detail_not_found))
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_EDIT_BUTTON).assertDoesNotExist()
    }
}
