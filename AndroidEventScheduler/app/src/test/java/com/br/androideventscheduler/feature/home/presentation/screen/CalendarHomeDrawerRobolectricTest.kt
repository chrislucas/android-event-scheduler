package com.br.androideventscheduler.feature.home.presentation.screen

import android.os.Looper
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.lifecycle.SavedStateHandle
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.preferences.testing.FakeUserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.FakeClock
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.home.domain.calendar.MonthGridBuilder
import com.br.androideventscheduler.feature.home.presentation.component.previewContent
import com.br.androideventscheduler.feature.home.presentation.effect.HomeUiEffect
import com.br.androideventscheduler.feature.home.presentation.event.HomeUiEvent
import com.br.androideventscheduler.feature.home.presentation.state.HomeUiState
import com.br.androideventscheduler.feature.home.presentation.viewmodel.HomeViewModel
import com.br.androideventscheduler.feature.home.testing.FakeHomePreferencesRepository
import com.br.androideventscheduler.feature.home.testing.FakeHomeScheduleRepository
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR-w411dp-h891dp")
class CalendarHomeDrawerRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    // FakeClock: 2026-11-07.
    private val today = LocalDate.of(2026, 11, 7)
    private val scheduleRepository = FakeHomeScheduleRepository()
    private val homePreferences = FakeHomePreferencesRepository()
    private val events = mutableListOf<HomeUiEvent>()

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        HomeViewModel(
            scheduleRepository = scheduleRepository,
            userSettingsRepository = FakeUserSettingsRepository(),
            homePreferencesRepository = homePreferences,
            monthGridBuilder = MonthGridBuilder(),
            clock = FakeClock(),
            savedStateHandle = savedStateHandle,
            dispatcher = Dispatchers.Unconfined,
        )

    /** Conteúdo stateless ligado ao ViewModel real, registrando cada evento que a UI emite. */
    private fun setContent(viewModel: HomeViewModel) {
        composeRule.setContent {
            val uiState by viewModel.uiState.collectAsState()
            AndroidEventSchedulerTheme {
                HomeContent(
                    uiState = uiState,
                    onEvent = {
                        events += it
                        viewModel.onEvent(it)
                    },
                )
            }
        }
    }

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
    }

    private fun openDrawer() {
        composeRule.onNodeWithTag(SemanticsTags.HOME_MENU_BUTTON).performClick()
        idle()
    }

    private fun filterTag(source: CalendarSource) = SemanticsTags.HOME_DRAWER_FILTER_PREFIX + source.id

    private fun tapScrim() {
        composeRule.onRoot().performTouchInput { click(Offset(width - 8f, height / 2f)) }
        idle()
    }

    @Test
    fun `given the home when the menu is clicked then the drawer opens with the three filters and settings`() {
        setContent(createViewModel())
        idle()
        composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SHEET).assertIsNotDisplayed()

        openDrawer()

        composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SHEET).assertIsDisplayed()
        CalendarSource.entries.forEach { composeRule.onNodeWithTag(filterTag(it)).assertIsDisplayed() }
        composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SETTINGS_BUTTON).assertIsDisplayed()
    }

    @Test
    fun `given the drawer when inspecting a filter row then it is a checkbox with a visible or hidden state description`() {
        homePreferences.enabled.value = setOf(CalendarSource.PERSONAL_ACCOUNT, CalendarSource.MY_REMINDERS)
        setContent(createViewModel())
        idle()
        openDrawer()

        composeRule
            .onNodeWithTag(filterTag(CalendarSource.FAMILY))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Oculto"))
        composeRule
            .onNodeWithTag(filterTag(CalendarSource.PERSONAL_ACCOUNT))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Visível"))
    }

    @Test
    fun `given the drawer when toggling a filter then the event is sent, the state description flips and the drawer stays open`() {
        scheduleRepository.items.value =
            listOf(scheduleItem(id = "fam", title = "Almoço", startDate = today).copy(calendarSource = CalendarSource.FAMILY))
        setContent(createViewModel())
        idle()
        openDrawer()

        composeRule.onNodeWithTag(filterTag(CalendarSource.FAMILY)).performClick()
        idle()

        assertTrue(HomeUiEvent.OnCalendarSourceToggled(CalendarSource.FAMILY) in events)
        composeRule
            .onNodeWithTag(filterTag(CalendarSource.FAMILY))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Oculto"))
        composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SHEET).assertIsDisplayed()
        assertEquals(setOf(CalendarSource.PERSONAL_ACCOUNT, CalendarSource.MY_REMINDERS), homePreferences.enabled.value)
    }

    @Test
    fun `given a family item when family is hidden in the drawer then it disappears from the day list`() {
        scheduleRepository.items.value =
            listOf(scheduleItem(id = "fam", title = "Almoço", startDate = today).copy(calendarSource = CalendarSource.FAMILY))
        setContent(createViewModel())
        idle()
        composeRule.onNodeWithTag(SemanticsTags.HOME_SCHEDULE_ITEM_PREFIX + "fam").assertExists()
        openDrawer()

        composeRule.onNodeWithTag(filterTag(CalendarSource.FAMILY)).performClick()
        idle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_SCHEDULE_ITEM_PREFIX + "fam").assertDoesNotExist()
        composeRule.onNodeWithTag(SemanticsTags.HOME_EMPTY_DAY).assertExists()
    }

    @Test
    fun `given the drawer open when tapping the scrim then it closes and the dismissal is reported exactly once without a loop`() {
        val viewModel = createViewModel()
        setContent(viewModel)
        idle()
        openDrawer()
        assertEquals(listOf<HomeUiEvent>(HomeUiEvent.OnMenuClicked), events)

        tapScrim()
        idle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SHEET).assertIsNotDisplayed()
        assertFalse((viewModel.uiState.value as HomeUiState.Content).isDrawerOpen)
        assertEquals(listOf(HomeUiEvent.OnMenuClicked, HomeUiEvent.OnDrawerDismissed), events)
    }

    @Test
    fun `given the drawer reopened after a scrim dismissal when the menu is clicked again then it opens again`() {
        val viewModel = createViewModel()
        setContent(viewModel)
        idle()
        openDrawer()
        tapScrim()

        openDrawer()

        composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SHEET).assertIsDisplayed()
        assertTrue((viewModel.uiState.value as HomeUiState.Content).isDrawerOpen)
        assertEquals(
            listOf(HomeUiEvent.OnMenuClicked, HomeUiEvent.OnDrawerDismissed, HomeUiEvent.OnMenuClicked),
            events,
        )
    }

    @Test
    fun `given the state saved as open when the screen is recreated then the drawer starts open`() {
        val savedStateHandle = SavedStateHandle()
        createViewModel(savedStateHandle).onEvent(HomeUiEvent.OnMenuClicked)

        setContent(createViewModel(savedStateHandle))
        idle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SHEET).assertIsDisplayed()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `given the drawer open when clicking settings then it closes and navigates to settings`() {
        val viewModel = createViewModel()
        var openedSettings = 0
        composeRule.setContent {
            val uiState by viewModel.uiState.collectAsState()
            LaunchedEffect(Unit) {
                viewModel.uiEffect.collect { if (it == HomeUiEffect.NavigateToSettings) openedSettings++ }
            }
            AndroidEventSchedulerTheme { HomeContent(uiState = uiState, onEvent = viewModel::onEvent) }
        }
        idle()
        openDrawer()

        composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SETTINGS_BUTTON).performClick()
        idle()

        assertEquals(1, openedSettings)
        composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SHEET).assertIsNotDisplayed()
    }

    @Test
    fun `given the first load when content arrives after loading then the drawer stays closed`() {
        val uiState = mutableStateOf<HomeUiState>(HomeUiState.Loading)
        composeRule.setContent {
            AndroidEventSchedulerTheme { HomeContent(uiState = uiState.value, onEvent = { events += it }) }
        }
        idle()

        uiState.value = previewContent()
        idle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SHEET).assertIsNotDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.HOME_QUICK_ADD_BAR).performClick()
        assertEquals(listOf<HomeUiEvent>(HomeUiEvent.OnQuickAddClicked), events)
    }

    @Test
    fun `given the home when swiping from the left edge then the drawer opens and the state follows exactly once`() {
        val viewModel = createViewModel()
        setContent(viewModel)
        idle()

        composeRule.onRoot().performTouchInput { swipeRight(startX = 1f, endX = width * 0.9f) }
        idle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SHEET).assertIsDisplayed()
        assertTrue((viewModel.uiState.value as HomeUiState.Content).isDrawerOpen)
        assertEquals(listOf<HomeUiEvent>(HomeUiEvent.OnMenuClicked), events)
    }

    @Test
    fun `given the drawer open when swiping it closed then the dismissal is reported exactly once`() {
        val viewModel = createViewModel()
        setContent(viewModel)
        idle()
        openDrawer()

        composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SHEET).performTouchInput { swipeLeft() }
        idle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SHEET).assertIsNotDisplayed()
        assertFalse((viewModel.uiState.value as HomeUiState.Content).isDrawerOpen)
        assertEquals(listOf(HomeUiEvent.OnMenuClicked, HomeUiEvent.OnDrawerDismissed), events)
    }
}
