package com.br.androideventscheduler.feature.search.presentation.screen

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.search.domain.model.SearchFilter
import com.br.androideventscheduler.feature.search.presentation.event.SearchUiEvent
import com.br.androideventscheduler.feature.search.presentation.state.SearchResultUi
import com.br.androideventscheduler.feature.search.presentation.state.SearchUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR")
class SearchScreenRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val events = mutableListOf<SearchUiEvent>()

    private val result =
        SearchResultUi(
            id = "42",
            colorHex = 0xFF26A69A,
            stickerEmoji = "🚀",
            title = "Reunião",
            dateTimeText = "qui., 5 de nov., 10:00",
            typeText = "Evento",
            calendarLabel = "bia@exemplo.com",
            recurrenceText = "Diariamente",
        )

    private fun setContent(uiState: SearchUiState) {
        composeRule.setContent {
            AndroidEventSchedulerTheme { SearchContent(uiState = uiState, onEvent = { events += it }) }
        }
    }

    @Test
    fun `given the screen opens when rendered then the query field is focused`() {
        setContent(SearchUiState.Idle("", SearchFilter.ALL))
        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(SemanticsTags.SEARCH_QUERY_INPUT)
            .assertIsFocused()
            .assertContentDescriptionContains(context.getString(R.string.search_field_description))
    }

    @Test
    fun `given idle state when rendered then the hint is shown and the clear button is hidden`() {
        setContent(SearchUiState.Idle("", SearchFilter.ALL))

        composeRule.onNodeWithTag(SemanticsTags.SEARCH_HINT).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.search_hint)).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.SEARCH_CLEAR_BUTTON).assertDoesNotExist()
    }

    @Test
    fun `given filter chips when rendered then they are radio buttons reflecting the selection`() {
        setContent(SearchUiState.Idle("", SearchFilter.EVENTS))

        composeRule
            .onNodeWithTag(SemanticsTags.SEARCH_FILTER_CHIP_PREFIX + SearchFilter.EVENTS.name)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertContentDescriptionContains(context.getString(R.string.search_filter_events_description))
            .assertIsSelected()
        composeRule
            .onNodeWithTag(SemanticsTags.SEARCH_FILTER_CHIP_PREFIX + SearchFilter.ALL.name)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsNotSelected()
    }

    @Test
    fun `given a chip not selected when clicked then filter selected event is sent`() {
        setContent(SearchUiState.Idle("", SearchFilter.ALL))

        composeRule.onNodeWithTag(SemanticsTags.SEARCH_FILTER_CHIP_PREFIX + SearchFilter.REMINDERS.name).performClick()

        assertEquals(SearchUiEvent.OnFilterSelected(SearchFilter.REMINDERS), events.last())
    }

    @Test
    fun `given the user types when text changes then query changed event carries the text`() {
        setContent(SearchUiState.Idle("", SearchFilter.ALL))

        composeRule.onNodeWithTag(SemanticsTags.SEARCH_QUERY_INPUT).performTextReplacement("reu")

        assertEquals(SearchUiEvent.OnQueryChanged("reu"), events.last())
    }

    @Test
    fun `given text in the field when clear is clicked then the field empties keeps focus and clear event is sent`() {
        setContent(SearchUiState.Results("reu", SearchFilter.ALL, listOf(result)))
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SemanticsTags.SEARCH_CLEAR_BUTTON).performClick()

        assertEquals(SearchUiEvent.OnClearQueryClicked, events.last())
        composeRule.onNodeWithTag(SemanticsTags.SEARCH_QUERY_INPUT).assertTextContains("", substring = false).assertIsFocused()
        composeRule.onNodeWithTag(SemanticsTags.SEARCH_CLEAR_BUTTON).assertDoesNotExist()
    }

    @Test
    fun `given results when rendered then each item shows its fields and clicking sends the id`() {
        setContent(SearchUiState.Results("reu", SearchFilter.ALL, listOf(result)))

        composeRule.onNodeWithText("Reunião").assertIsDisplayed()
        composeRule.onNodeWithText("qui., 5 de nov., 10:00").assertIsDisplayed()
        composeRule.onNodeWithText("Evento · bia@exemplo.com").assertIsDisplayed()
        composeRule.onNodeWithText("Diariamente").assertIsDisplayed()
        composeRule.onNodeWithText("🚀").assertIsDisplayed()
        composeRule.onNodeWithText("1 resultado").assertIsDisplayed()

        composeRule.onNodeWithTag(SemanticsTags.SEARCH_RESULT_ITEM_PREFIX + "42").performClick()

        assertEquals(SearchUiEvent.OnResultClicked("42"), events.last())
    }

    @Test
    fun `given empty state when rendered then the message quotes the trimmed text`() {
        setContent(SearchUiState.Empty("  dentista ", SearchFilter.ALL))

        composeRule.onNodeWithTag(SemanticsTags.SEARCH_EMPTY_STATE).assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum resultado para “dentista”").assertIsDisplayed()
    }

    @Test
    fun `given searching state when rendered then progress shows and previous results stay`() {
        setContent(SearchUiState.Searching("reun", SearchFilter.ALL, listOf(result)))

        composeRule.onNodeWithTag(SemanticsTags.SEARCH_LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.SEARCH_RESULT_ITEM_PREFIX + "42").assertIsDisplayed()
    }

    @Test
    fun `given error state when retry is clicked then retry event is sent`() {
        setContent(SearchUiState.Error("reu", SearchFilter.ALL))

        composeRule.onNodeWithTag(SemanticsTags.SEARCH_ERROR_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.SEARCH_RETRY_BUTTON).performClick()

        assertEquals(SearchUiEvent.OnRetryClicked, events.last())
    }

    @Test
    fun `given back button when clicked then back event is sent`() {
        setContent(SearchUiState.Idle("", SearchFilter.ALL))

        composeRule.onNodeWithTag(SemanticsTags.SEARCH_BACK_BUTTON).performClick()

        assertEquals(SearchUiEvent.OnBackClicked, events.last())
    }
}
