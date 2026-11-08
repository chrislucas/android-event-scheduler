package com.br.androideventscheduler.feature.home.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.testing.FakeUserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.FakeClock
import com.br.androideventscheduler.core.testing.MainDispatcherRule
import com.br.androideventscheduler.feature.home.domain.calendar.MonthGridBuilder
import com.br.androideventscheduler.feature.home.presentation.effect.HomeUiEffect
import com.br.androideventscheduler.feature.home.presentation.event.HomeUiEvent
import com.br.androideventscheduler.feature.home.presentation.state.HomeUiState
import com.br.androideventscheduler.feature.home.testing.FakeHomePreferencesRepository
import com.br.androideventscheduler.feature.home.testing.FakeHomeScheduleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    // FakeClock: 2026-11-07 (sábado) às 09:00 em São Paulo.
    private val clock = FakeClock()
    private val today = LocalDate.of(2026, 11, 7)
    private val repository = FakeHomeScheduleRepository()
    private val settingsRepository = FakeUserSettingsRepository(UserSettings(activeAccountEmail = "bia@exemplo.com"))
    private val homePreferences = FakeHomePreferencesRepository()

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        HomeViewModel(
            scheduleRepository = repository,
            userSettingsRepository = settingsRepository,
            homePreferencesRepository = homePreferences,
            monthGridBuilder = MonthGridBuilder(),
            clock = clock,
            savedStateHandle = savedStateHandle,
            dispatcher = mainRule.dispatcher,
        )

    private fun HomeViewModel.content(): HomeUiState.Content = uiState.value as HomeUiState.Content

    private fun HomeViewModel.cell(date: LocalDate) = content().cells.first { it.date == date }

    private fun item(
        id: String,
        date: LocalDate = today,
        color: Long = 0xFF000001,
        time: LocalTime = LocalTime.of(10, 0),
        endDate: LocalDate = date,
        allDay: Boolean = false,
        recurrence: RecurrencePattern = RecurrencePattern.None,
        source: CalendarSource = CalendarSource.PERSONAL_ACCOUNT,
    ): ScheduleItem =
        scheduleItem(id = id, startDate = date, endDate = endDate, startTime = time, isAllDay = allDay, recurrencePattern = recurrence)
            .copy(colorHex = color, calendarSource = source)

    @Test
    fun `given a new screen when created then it is loading until the first data arrives`() =
        runTest {
            val viewModel = createViewModel()

            assertEquals(HomeUiState.Loading, viewModel.uiState.value)
            runCurrent()

            assertTrue(viewModel.uiState.value is HomeUiState.Content)
        }

    @Test
    fun `given the injected clock when created then month and selected day are today and the grid has 42 cells`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            val content = viewModel.content()
            assertEquals(YearMonth.of(2026, 11), content.visibleMonth)
            assertEquals(today, content.selectedDate)
            assertEquals("bia@exemplo.com", content.accountEmail)
            assertEquals(42, content.cells.size)
            assertEquals(listOf(today), content.cells.filter { it.isToday }.map { it.date })
            assertEquals(listOf(today), content.cells.filter { it.isSelected }.map { it.date })
            assertFalse(content.isMonthPickerVisible)
        }

    @Test
    fun `given the visible month when building cells then neighbor month days are flagged as outside`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            val inside = viewModel.content().cells.filter { it.isInVisibleMonth }
            assertEquals(30, inside.size)
            assertFalse(viewModel.cell(LocalDate.of(2026, 12, 1)).isInVisibleMonth)
        }

    @Test
    fun `given a day from the next month when clicked then the visible month changes to it`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.onEvent(HomeUiEvent.OnDayClicked(LocalDate.of(2026, 12, 3)))
            runCurrent()

            assertEquals(YearMonth.of(2026, 12), viewModel.content().visibleMonth)
            assertEquals(LocalDate.of(2026, 12, 3), viewModel.content().selectedDate)
        }

    @Test
    fun `given a day inside the visible month when clicked then only the selection changes`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.onEvent(HomeUiEvent.OnDayClicked(LocalDate.of(2026, 11, 20)))
            runCurrent()

            assertEquals(YearMonth.of(2026, 11), viewModel.content().visibleMonth)
            assertTrue(viewModel.cell(LocalDate.of(2026, 11, 20)).isSelected)
            assertFalse(viewModel.cell(today).isSelected)
            assertTrue(viewModel.cell(today).isToday)
        }

    @Test
    fun `given a weekly recurring item when the grid is built then every occurrence in view has an indicator`() =
        runTest {
            val weekly = item("weekly", date = LocalDate.of(2026, 10, 7), recurrence = RecurrencePattern.Weekly(setOf(DayOfWeek.WEDNESDAY)))
            repository.items.value = listOf(weekly)
            val viewModel = createViewModel()
            runCurrent()

            val withSchedules =
                viewModel
                    .content()
                    .cells
                    .filter { it.scheduleCount > 0 }
                    .map { it.date }
            assertEquals(
                listOf(4, 11, 18, 25).map { LocalDate.of(2026, 11, it) } + listOf(2, 9).map { LocalDate.of(2026, 12, it) },
                withSchedules,
            )
        }

    @Test
    fun `given more than three items on a day when building then only three distinct colors in time order are shown`() =
        runTest {
            repository.items.value =
                listOf(
                    item("late", color = 4, time = LocalTime.of(18, 0)),
                    item("a", color = 1, time = LocalTime.of(8, 0)),
                    item("b", color = 2, time = LocalTime.of(9, 0)),
                    item("c", color = 3, time = LocalTime.of(10, 0)),
                    item("same", color = 1, time = LocalTime.of(11, 0)),
                )
            val viewModel = createViewModel()
            runCurrent()

            val cell = viewModel.cell(today)
            assertEquals(listOf(1L, 2L, 3L), cell.indicatorColors)
            assertEquals(5, cell.scheduleCount)
        }

    @Test
    fun `given a multi day item when building then every covered day is marked and others are not`() =
        runTest {
            repository.items.value = listOf(item("trip", date = LocalDate.of(2026, 11, 9), endDate = LocalDate.of(2026, 11, 11)))
            val viewModel = createViewModel()
            runCurrent()

            val marked =
                viewModel
                    .content()
                    .cells
                    .filter { it.scheduleCount == 1 }
                    .map { it.date.dayOfMonth }
            assertEquals(listOf(9, 10, 11), marked)
        }

    @Test
    fun `given an item crossing midnight when selecting each day then it appears on both days`() =
        runTest {
            val night = item("night", date = today, endDate = today.plusDays(1), time = LocalTime.of(22, 0))
            repository.items.value = listOf(night)
            val viewModel = createViewModel()
            runCurrent()
            assertEquals(listOf("night"), viewModel.content().daySchedules.map { it.item.id })

            viewModel.onEvent(HomeUiEvent.OnDayClicked(today.plusDays(1)))
            runCurrent()

            assertEquals(listOf("night"), viewModel.content().daySchedules.map { it.item.id })
        }

    @Test
    fun `given items on the selected day when listing then all day comes first and the rest by start time`() =
        runTest {
            repository.items.value =
                listOf(
                    item("evening", time = LocalTime.of(20, 0)),
                    item("morning", time = LocalTime.of(8, 0)),
                    item("all-day", time = LocalTime.of(23, 0), allDay = true),
                    item("other-day", date = today.plusDays(1)),
                )
            val viewModel = createViewModel()
            runCurrent()

            assertEquals(listOf("all-day", "morning", "evening"), viewModel.content().daySchedules.map { it.item.id })
        }

    @Test
    fun `given the database changes when an item is inserted and removed then grid and list update reactively`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()
            assertTrue(viewModel.content().daySchedules.isEmpty())

            repository.items.value = listOf(item("new"))
            runCurrent()
            assertEquals(listOf("new"), viewModel.content().daySchedules.map { it.item.id })
            assertEquals(1, viewModel.cell(today).scheduleCount)

            repository.items.value = emptyList()
            runCurrent()
            assertTrue(viewModel.content().daySchedules.isEmpty())
            assertEquals(0, viewModel.cell(today).scheduleCount)
        }

    @Test
    fun `given the account email changes when settings update then the content carries the new label`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            settingsRepository.settings.value = UserSettings(activeAccountEmail = "carla@exemplo.com")
            runCurrent()

            assertEquals("carla@exemplo.com", viewModel.content().accountEmail)
        }

    @Test
    fun `given the 31st selected when picking a 30 day month then the day is clamped to 30`() =
        runTest {
            val viewModel = createViewModel(SavedStateHandle(mapOf("home_selected_epoch_day" to LocalDate.of(2026, 10, 31).toEpochDay())))
            runCurrent()
            assertEquals(YearMonth.of(2026, 10), viewModel.content().visibleMonth)

            viewModel.onEvent(HomeUiEvent.OnMonthPicked(YearMonth.of(2026, 9)))
            runCurrent()

            assertEquals(LocalDate.of(2026, 9, 30), viewModel.content().selectedDate)
        }

    @Test
    fun `given a picked month other than the current when picking then the same day of month is selected`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.onEvent(HomeUiEvent.OnMonthPicked(YearMonth.of(2027, 2)))
            runCurrent()

            assertEquals(LocalDate.of(2027, 2, 7), viewModel.content().selectedDate)
        }

    @Test
    fun `given another month visible when picking the current month then today is selected`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()
            viewModel.onEvent(HomeUiEvent.OnDayClicked(LocalDate.of(2026, 12, 25)))
            runCurrent()

            viewModel.onEvent(HomeUiEvent.OnMonthPicked(YearMonth.of(2026, 11)))
            runCurrent()

            assertEquals(today, viewModel.content().selectedDate)
        }

    @Test
    fun `given the month picker when opened changed and dismissed then visibility and year follow the events`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.onEvent(HomeUiEvent.OnMonthHeaderClicked)
            runCurrent()
            assertTrue(viewModel.content().isMonthPickerVisible)
            assertEquals(2026, viewModel.content().monthPickerYear)

            viewModel.onEvent(HomeUiEvent.OnMonthPickerYearChanged(+1))
            viewModel.onEvent(HomeUiEvent.OnMonthPickerYearChanged(+1))
            viewModel.onEvent(HomeUiEvent.OnMonthPickerYearChanged(-1))
            runCurrent()
            assertEquals(2027, viewModel.content().monthPickerYear)
            assertEquals(YearMonth.of(2026, 11), viewModel.content().visibleMonth)

            viewModel.onEvent(HomeUiEvent.OnMonthPickerDismissed)
            runCurrent()
            assertFalse(viewModel.content().isMonthPickerVisible)
        }

    @Test
    fun `given the month picker open when a month is picked then it closes and the visible month changes`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()
            viewModel.onEvent(HomeUiEvent.OnMonthHeaderClicked)
            runCurrent()

            viewModel.onEvent(HomeUiEvent.OnMonthPicked(YearMonth.of(2026, 3)))
            runCurrent()

            assertFalse(viewModel.content().isMonthPickerVisible)
            assertEquals(YearMonth.of(2026, 3), viewModel.content().visibleMonth)
        }

    @Test
    fun `given previous and next events when navigating months then the visible month moves and the selection follows`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.onEvent(HomeUiEvent.OnNextMonthClicked)
            runCurrent()
            assertEquals(YearMonth.of(2026, 12), viewModel.content().visibleMonth)
            assertEquals(LocalDate.of(2026, 12, 7), viewModel.content().selectedDate)

            viewModel.onEvent(HomeUiEvent.OnPreviousMonthClicked)
            viewModel.onEvent(HomeUiEvent.OnPreviousMonthClicked)
            runCurrent()
            assertEquals(YearMonth.of(2026, 10), viewModel.content().visibleMonth)
            assertEquals(LocalDate.of(2026, 10, 7), viewModel.content().selectedDate)
        }

    @Test
    fun `given a month change when the new data has not arrived then the previous content stays instead of loading`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.onEvent(HomeUiEvent.OnNextMonthClicked)

            assertTrue(viewModel.uiState.value is HomeUiState.Content)
            runCurrent()
            assertEquals(YearMonth.of(2026, 12), viewModel.content().visibleMonth)
        }

    @Test
    fun `given a saved state when the view model is recreated then month and selected day are restored`() =
        runTest {
            val savedStateHandle = SavedStateHandle()
            val first = createViewModel(savedStateHandle)
            runCurrent()
            first.onEvent(HomeUiEvent.OnDayClicked(LocalDate.of(2027, 1, 15)))
            runCurrent()

            val restored = createViewModel(savedStateHandle)
            runCurrent()

            assertEquals(YearMonth.of(2027, 1), restored.content().visibleMonth)
            assertEquals(LocalDate.of(2027, 1, 15), restored.content().selectedDate)
        }

    @Test
    fun `given the repository fails when observing then the state is error and retry recovers`() =
        runTest {
            repository.failing = true
            val viewModel = createViewModel()
            runCurrent()
            assertEquals(HomeUiState.Error(R.string.home_error_generic), viewModel.uiState.value)

            repository.failing = false
            viewModel.onEvent(HomeUiEvent.OnRetryClicked)
            runCurrent()

            assertTrue(viewModel.uiState.value is HomeUiState.Content)
        }

    @Test
    fun `given navigation events when handled then the matching effects are emitted with the selected date`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()
            val effects = mutableListOf<HomeUiEffect>()
            val collectJob = launch { viewModel.uiEffect.toList(effects) }

            viewModel.onEvent(HomeUiEvent.OnSearchClicked)
            viewModel.onEvent(HomeUiEvent.OnScheduleClicked("42"))
            viewModel.onEvent(HomeUiEvent.OnDayClicked(LocalDate.of(2026, 11, 20)))
            viewModel.onEvent(HomeUiEvent.OnQuickAddClicked)
            runCurrent()

            assertEquals(
                listOf(
                    HomeUiEffect.NavigateToSearch,
                    HomeUiEffect.NavigateToDetail("42"),
                    HomeUiEffect.NavigateToUpsert(LocalDate.of(2026, 11, 20).toEpochDay()),
                ),
                effects,
            )
            collectJob.cancel()
        }

    @Test
    fun `given a family item when family is hidden then month cell and day list drop it and showing it again restores it`() =
        runTest {
            repository.items.value =
                listOf(item("mine"), item("family", source = CalendarSource.FAMILY, time = LocalTime.of(11, 0)))
            val viewModel = createViewModel()
            runCurrent()
            assertEquals(listOf("mine", "family"), viewModel.content().daySchedules.map { it.item.id })

            viewModel.onEvent(HomeUiEvent.OnCalendarSourceToggled(CalendarSource.FAMILY))
            runCurrent()

            assertEquals(setOf(CalendarSource.PERSONAL_ACCOUNT, CalendarSource.MY_REMINDERS), viewModel.content().enabledSources)
            assertEquals(listOf("mine"), viewModel.content().daySchedules.map { it.item.id })
            assertEquals(1, viewModel.cell(today).scheduleCount)

            viewModel.onEvent(HomeUiEvent.OnCalendarSourceToggled(CalendarSource.FAMILY))
            runCurrent()

            assertEquals(listOf("mine", "family"), viewModel.content().daySchedules.map { it.item.id })
            assertEquals(2, viewModel.cell(today).scheduleCount)
        }

    @Test
    fun `given every calendar hidden when building then grid and list are empty without error`() =
        runTest {
            repository.items.value = listOf(item("mine"), item("family", source = CalendarSource.FAMILY))
            homePreferences.enabled.value = emptySet()
            val viewModel = createViewModel()
            runCurrent()

            val content = viewModel.content()
            assertTrue(content.daySchedules.isEmpty())
            assertTrue(content.cells.all { it.scheduleCount == 0 && it.indicatorColors.isEmpty() })
            assertTrue(content.enabledSources.isEmpty())
        }

    @Test
    fun `given the persisted filter when created then the hidden calendar stays hidden`() =
        runTest {
            repository.items.value = listOf(item("reminder", source = CalendarSource.MY_REMINDERS))
            homePreferences.enabled.value = setOf(CalendarSource.PERSONAL_ACCOUNT)
            val viewModel = createViewModel()
            runCurrent()

            assertTrue(viewModel.content().daySchedules.isEmpty())
        }

    @Test
    fun `given the account email changes in settings when observing then the label updates without restart`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            settingsRepository.settings.value = UserSettings(activeAccountEmail = "novo@exemplo.com")
            runCurrent()

            assertEquals("novo@exemplo.com", viewModel.content().accountEmail)
        }

    @Test
    fun `given the drawer closed when the menu is clicked and then dismissed then the state follows`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()
            assertFalse(viewModel.content().isDrawerOpen)

            viewModel.onEvent(HomeUiEvent.OnMenuClicked)
            runCurrent()
            assertTrue(viewModel.content().isDrawerOpen)

            viewModel.onEvent(HomeUiEvent.OnDrawerDismissed)
            runCurrent()
            assertFalse(viewModel.content().isDrawerOpen)
        }

    @Test
    fun `given the drawer saved as open when the view model is recreated then it is restored open`() =
        runTest {
            val savedStateHandle = SavedStateHandle()
            val first = createViewModel(savedStateHandle)
            runCurrent()
            first.onEvent(HomeUiEvent.OnMenuClicked)
            runCurrent()

            val second = createViewModel(savedStateHandle)
            runCurrent()

            assertTrue(second.content().isDrawerOpen)
        }

    @Test
    fun `given the drawer open when settings is clicked then it closes and navigates to settings`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()
            val effects = mutableListOf<HomeUiEffect>()
            val collectJob = launch { viewModel.uiEffect.toList(effects) }
            viewModel.onEvent(HomeUiEvent.OnMenuClicked)
            runCurrent()

            viewModel.onEvent(HomeUiEvent.OnSettingsClicked)
            runCurrent()

            assertFalse(viewModel.content().isDrawerOpen)
            assertEquals(listOf<HomeUiEffect>(HomeUiEffect.NavigateToSettings), effects)
            collectJob.cancel()
        }
}
