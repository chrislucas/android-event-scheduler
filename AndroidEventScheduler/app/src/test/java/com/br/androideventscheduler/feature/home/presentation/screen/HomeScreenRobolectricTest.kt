package com.br.androideventscheduler.feature.home.presentation.screen

import android.Manifest
import android.content.Context
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.preferences.testing.FakeUserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.StickerEmoji
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.FakeClock
import com.br.androideventscheduler.core.testing.pressBackOnLatestDialog
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.home.domain.calendar.MonthGridBuilder
import com.br.androideventscheduler.feature.home.presentation.component.previewContent
import com.br.androideventscheduler.feature.home.presentation.event.HomeUiEvent
import com.br.androideventscheduler.feature.home.presentation.state.HomeUiState
import com.br.androideventscheduler.feature.home.presentation.viewmodel.HomeViewModel
import com.br.androideventscheduler.feature.home.testing.FakeHomePreferencesRepository
import com.br.androideventscheduler.feature.home.testing.FakeHomeScheduleRepository
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** Registro que só anota os pedidos de permissão (o diálogo do sistema não existe no Robolectric). */
private class RecordingRegistry : ActivityResultRegistry() {
    val requestedPermissions = mutableListOf<String>()

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        requestedPermissions += input as String
        dispatchResult(requestCode, false)
    }
}

@RunWith(RobolectricTestRunner::class)
// Tela de celular típico (411×891 dp): a grade ocupa ~300 dp e a agenda precisa caber abaixo dela.
@Config(qualifiers = "pt-rBR-w411dp-h891dp")
class HomeScreenRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val events = mutableListOf<HomeUiEvent>()

    private val repository = FakeHomeScheduleRepository()
    private val registry = RecordingRegistry()
    private val homePreferences = FakeHomePreferencesRepository()

    // FakeClock: 2026-11-07. Dispatcher Unconfined: o estado derivado reage na hora, sem drenar looper.
    private fun createViewModel(items: List<ScheduleItem> = emptyList()): HomeViewModel {
        repository.items.value = items
        return HomeViewModel(
            scheduleRepository = repository,
            userSettingsRepository = FakeUserSettingsRepository(),
            homePreferencesRepository = homePreferences,
            monthGridBuilder = MonthGridBuilder(),
            clock = FakeClock(),
            savedStateHandle = SavedStateHandle(),
            dispatcher = Dispatchers.Unconfined,
        )
    }

    private fun setStatelessContent(uiState: HomeUiState) {
        composeRule.setContent {
            AndroidEventSchedulerTheme { HomeContent(uiState = uiState, onEvent = { events += it }) }
        }
    }

    private fun setScreen(
        viewModel: HomeViewModel,
        onSearch: () -> Unit = {},
        onUpsert: (Long) -> Unit = {},
        onDetail: (String) -> Unit = {},
        onSettings: () -> Unit = {},
    ) {
        val owner =
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry: ActivityResultRegistry = registry
            }
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                AndroidEventSchedulerTheme {
                    HomeScreen(
                        onNavigateToSearch = onSearch,
                        onNavigateToUpsert = onUpsert,
                        onNavigateToDetail = onDetail,
                        onNavigateToSettings = onSettings,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }

    private fun cellTag(date: LocalDate) = SemanticsTags.HOME_DAY_CELL_PREFIX + date.toEpochDay()

    @Test
    fun `given the screen opens when composed then it asks for the notification permission`() {
        setScreen(createViewModel())
        composeRule.waitForIdle()

        assertEquals(listOf(Manifest.permission.POST_NOTIFICATIONS), registry.requestedPermissions)
    }

    @Test
    fun `given the current month when rendered then the title shows month and year`() {
        setScreen(createViewModel())

        composeRule.onNodeWithTag(SemanticsTags.HOME_MONTH_HEADER).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.HOME_DAY_HEADER).assertTextEquals("7 SÁB.")
        composeRule.onNodeWithTag(SemanticsTags.HOME_CALENDAR_GRID).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.HOME_MONTH_HEADER).assertTextEquals("novembro 2026")
    }

    @Test
    fun `given the title when clicked then the month picker sheet opens with the visible month selected`() {
        setScreen(createViewModel())

        composeRule.onNodeWithTag(SemanticsTags.HOME_MONTH_HEADER).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_MONTH_PICKER_SHEET).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.HOME_PICKER_YEAR).assertTextEquals("2026")
        composeRule
            .onNodeWithTag(SemanticsTags.HOME_MONTH_OPTION_PREFIX + 11)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()
            .assertTextEquals("NOV.")
            .assertContentDescriptionEquals(context.getString(R.string.home_month_option_description, "novembro", 2026))
        composeRule.onNodeWithTag(SemanticsTags.HOME_MONTH_OPTION_PREFIX + 1).assertIsNotSelected().assertTextEquals("JAN.")
        composeRule.onNodeWithTag(SemanticsTags.HOME_MONTH_OPTION_PREFIX + 12).assertTextEquals("DEZ.")
    }

    @Test
    fun `given the open picker when a month is chosen then the sheet closes and the title updates`() {
        setScreen(createViewModel())
        composeRule.onNodeWithTag(SemanticsTags.HOME_MONTH_HEADER).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_MONTH_OPTION_PREFIX + 3).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_MONTH_PICKER_SHEET).assertDoesNotExist()
        composeRule.onNodeWithTag(SemanticsTags.HOME_MONTH_HEADER).assertTextEquals("março 2026")
    }

    @Test
    fun `given the open picker when the year arrows are clicked then the year changes`() {
        setScreen(createViewModel())
        composeRule.onNodeWithTag(SemanticsTags.HOME_MONTH_HEADER).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_PICKER_NEXT_YEAR_BUTTON).performClick()
        composeRule.onNodeWithTag(SemanticsTags.HOME_PICKER_NEXT_YEAR_BUTTON).performClick()
        composeRule.onNodeWithTag(SemanticsTags.HOME_PICKER_PREVIOUS_YEAR_BUTTON).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_PICKER_YEAR).assertTextEquals("2027")
        composeRule.onNodeWithTag(SemanticsTags.HOME_MONTH_OPTION_PREFIX + 11).assertIsNotSelected()
    }

    @Test
    fun `given a schedule on the selected day when its item is clicked then the detail navigation is requested`() {
        var openedId: String? = null
        val item = scheduleItem(id = "abc", title = "Dentista", startDate = LocalDate.of(2026, 11, 7))
        setScreen(createViewModel(listOf(item)), onDetail = { openedId = it })

        composeRule.onNodeWithTag(SemanticsTags.HOME_SCHEDULE_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.HOME_SCHEDULE_ITEM_PREFIX + "abc").performClick()
        composeRule.waitForIdle()

        assertEquals("abc", openedId)
    }

    @Test
    fun `given a schedule item when rendered then it shows title interval and calendar label`() {
        val item =
            scheduleItem(
                id = "abc",
                title = "Jantar",
                startDate = LocalDate.of(2026, 11, 7),
                startTime = LocalTime.of(22, 0),
                endTime = LocalTime.of(23, 30),
            )
        setScreen(createViewModel(listOf(item)))

        composeRule.onNodeWithTag(SemanticsTags.HOME_SCHEDULE_ITEM_PREFIX + "abc").assertIsDisplayed()
        composeRule.onNodeWithText("Jantar").assertIsDisplayed()
        composeRule.onNodeWithText("22:00 – 23:30 · christoffer.luccas@gmail.com").assertIsDisplayed()
    }

    @Test
    fun `given a day without schedules when rendered then the empty state is shown`() {
        setScreen(createViewModel())

        composeRule.onNodeWithTag(SemanticsTags.HOME_EMPTY_DAY).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.home_empty_day)).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.HOME_SCHEDULE_ITEM_PREFIX + "abc").assertDoesNotExist()
    }

    @Test
    fun `given cells with schedules when rendered then descriptions carry the date and count and selection is exposed`() {
        val content = previewContent()
        setStatelessContent(content)
        val date5 = LocalDate.of(2026, 11, 5)
        val date10 = LocalDate.of(2026, 11, 10)
        val date11 = LocalDate.of(2026, 11, 11)

        composeRule.onNodeWithTag(cellTag(date5)).assertContentDescriptionEquals("5 de novembro, 1 compromisso").assertIsSelected()
        composeRule.onNodeWithTag(cellTag(date10)).assertContentDescriptionEquals("10 de novembro, 2 compromissos").assertIsNotSelected()
        composeRule.onNodeWithTag(cellTag(date11)).assertContentDescriptionEquals("11 de novembro, sem compromissos")
        composeRule
            .onNodeWithTag(cellTag(date10))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun `given a cell when clicked then the day clicked event carries its date`() {
        setStatelessContent(previewContent())

        composeRule.onNodeWithTag(cellTag(LocalDate.of(2026, 12, 2))).performClick()

        assertEquals(HomeUiEvent.OnDayClicked(LocalDate.of(2026, 12, 2)), events.last())
    }

    @Test
    fun `given a neighbor month day when clicked with the real view model then the title switches to that month`() {
        setScreen(createViewModel())

        composeRule.onNodeWithTag(cellTag(LocalDate.of(2026, 12, 2))).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_MONTH_HEADER).assertTextEquals("dezembro 2026")
        composeRule.onNodeWithTag(SemanticsTags.HOME_DAY_HEADER).assertTextEquals("2 QUA.")
    }

    @Test
    fun `given the month buttons when clicked then previous and next events are sent`() {
        setStatelessContent(previewContent())

        composeRule.onNodeWithTag(SemanticsTags.HOME_PREVIOUS_MONTH_BUTTON).performClick()
        composeRule.onNodeWithTag(SemanticsTags.HOME_NEXT_MONTH_BUTTON).performClick()

        assertEquals(listOf(HomeUiEvent.OnPreviousMonthClicked, HomeUiEvent.OnNextMonthClicked), events)
        composeRule
            .onNodeWithTag(SemanticsTags.HOME_PREVIOUS_MONTH_BUTTON)
            .assertContentDescriptionEquals(context.getString(R.string.home_previous_month))
    }

    @Test
    fun `given the grid when swiped horizontally then the next or previous month event is sent`() {
        setStatelessContent(previewContent())

        composeRule.onNodeWithTag(SemanticsTags.HOME_CALENDAR_GRID).performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag(SemanticsTags.HOME_CALENDAR_GRID).performTouchInput { swipeRight() }

        assertEquals(listOf(HomeUiEvent.OnNextMonthClicked, HomeUiEvent.OnPreviousMonthClicked), events)
    }

    @Test
    fun `given the search button when clicked then the search navigation is requested`() {
        var searchCalls = 0
        setScreen(createViewModel(), onSearch = { searchCalls++ })

        composeRule
            .onNodeWithTag(SemanticsTags.HOME_SEARCH_BUTTON)
            .assertContentDescriptionEquals(context.getString(R.string.home_search_description))
            .performClick()
        composeRule.waitForIdle()

        assertEquals(1, searchCalls)
    }

    @Test
    fun `given another day selected when the quick add bar is clicked then upsert opens with that day`() {
        var upsertDay: Long? = null
        setScreen(createViewModel(), onUpsert = { upsertDay = it })

        composeRule.onNodeWithTag(cellTag(LocalDate.of(2026, 11, 20))).performClick()
        composeRule.onNodeWithTag(SemanticsTags.HOME_QUICK_ADD_BAR).assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals(LocalDate.of(2026, 11, 20).toEpochDay(), upsertDay)
    }

    @Test
    fun `given the loading state when rendered then progress shows and the month buttons are hidden`() {
        setStatelessContent(HomeUiState.Loading)

        composeRule.onNodeWithTag(SemanticsTags.HOME_LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.HOME_PREVIOUS_MONTH_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(SemanticsTags.HOME_QUICK_ADD_BAR).assertIsDisplayed()
    }

    @Test
    fun `given the error state when retry is clicked then the retry event is sent`() {
        setStatelessContent(HomeUiState.Error(R.string.home_error_generic))

        composeRule.onNodeWithTag(SemanticsTags.HOME_ERROR_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.HOME_RETRY_BUTTON).performClick()

        assertEquals(HomeUiEvent.OnRetryClicked, events.last())
    }

    @Test
    fun `given the month picker visible in the state when dismissed then the dismissed event is sent`() {
        setStatelessContent(previewContent(isMonthPickerVisible = true))
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_MONTH_PICKER_SHEET).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.HOME_MONTH_OPTION_PREFIX + 2).performClick()

        assertEquals(HomeUiEvent.OnMonthPicked(YearMonth.of(2026, 2)), events.last())
    }

    // region adesivo do dia (spec 09)

    @Test
    fun `given the day header when rendered then the sticker button sits beside it with the default description`() {
        setScreen(createViewModel())
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_DAY_HEADER).assertIsDisplayed()
        composeRule
            .onNodeWithTag(SemanticsTags.HOME_DAY_STICKER_BUTTON)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Adesivo do dia")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun `given the sticker button when choosing an emoji then it is saved for the day and the description names it`() {
        setScreen(createViewModel())
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_DAY_STICKER_BUTTON).performClick()
        composeRule.onNodeWithTag(SemanticsTags.STICKER_PICKER_DIALOG).assertExists()
        composeRule.onNodeWithTag(SemanticsTags.STICKER_OPTION_PREFIX + StickerEmoji.PARTY.name).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(SemanticsTags.STICKER_PICKER_DIALOG).assertCountEquals(0)
        composeRule
            .onNodeWithTag(SemanticsTags.HOME_DAY_STICKER_BUTTON)
            .assertContentDescriptionEquals("Adesivo do dia: 🎉. Toque para alterar")
        assertEquals(mapOf(LocalDate.of(2026, 11, 7).toEpochDay() to "🎉"), homePreferences.daySticker.value)
    }

    @Test
    fun `given a sticker on one day when selecting another day then each day shows its own sticker`() {
        val today = LocalDate.of(2026, 11, 7)
        homePreferences.daySticker.value = mapOf(today.toEpochDay() to "🎉")
        setScreen(createViewModel())
        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag(SemanticsTags.HOME_DAY_STICKER_BUTTON)
            .assertContentDescriptionEquals("Adesivo do dia: 🎉. Toque para alterar")

        composeRule.onNodeWithTag(cellTag(today.plusDays(1))).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(SemanticsTags.HOME_DAY_STICKER_BUTTON).assertContentDescriptionEquals("Adesivo do dia")

        composeRule.onNodeWithTag(cellTag(today)).performClick()
        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag(SemanticsTags.HOME_DAY_STICKER_BUTTON)
            .assertContentDescriptionEquals("Adesivo do dia: 🎉. Toque para alterar")
    }

    @Test
    fun `given a day sticker when removing it from the dialog then the button goes back to the default description`() {
        homePreferences.daySticker.value = mapOf(LocalDate.of(2026, 11, 7).toEpochDay() to "☕")
        setScreen(createViewModel())
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_DAY_STICKER_BUTTON).performClick()
        composeRule.onNodeWithTag(SemanticsTags.STICKER_OPTION_PREFIX + StickerEmoji.COFFEE.name).assertIsSelected()
        composeRule.onNodeWithTag(SemanticsTags.STICKER_CLEAR_BUTTON).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_DAY_STICKER_BUTTON).assertContentDescriptionEquals("Adesivo do dia")
        assertEquals(emptyMap<Long, String>(), homePreferences.daySticker.value)
    }

    @Test
    fun `given the picker visible in the state when dismissed then the dismissed event is sent`() {
        setStatelessContent(previewContent(daySticker = "🎉", isDayStickerPickerVisible = true))
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SemanticsTags.STICKER_OPTION_PREFIX + StickerEmoji.PARTY.name).assertIsSelected()
        composeRule.runOnUiThread { pressBackOnLatestDialog() }
        composeRule.waitForIdle()

        assertEquals(HomeUiEvent.OnDayStickerPickerDismissed, events.last())
    }

    // endregion

    // region toque duplo (spec 18)

    @Test
    fun `given a cell when double clicked then the upsert navigation is requested with that day`() {
        val upserts = mutableListOf<Long>()
        val target = LocalDate.of(2026, 11, 10)
        setScreen(createViewModel(), onUpsert = { upserts += it })

        composeRule.onNodeWithTag(cellTag(target)).performTouchInput { doubleClick() }
        composeRule.waitForIdle()

        assertEquals(listOf(target.toEpochDay()), upserts)
        composeRule.onNodeWithTag(cellTag(target)).assertIsSelected()
    }

    @Test
    fun `given a cell when clicked once then it is selected right away and no creation is requested`() {
        val upserts = mutableListOf<Long>()
        val target = LocalDate.of(2026, 11, 10)
        setScreen(createViewModel(), onUpsert = { upserts += it })

        composeRule.onNodeWithTag(cellTag(target)).performClick()

        // Sem avançar o relógio: a seleção não espera o intervalo de toque duplo (R2.1).
        composeRule.onNodeWithTag(cellTag(target)).assertIsSelected()
        composeRule.waitForIdle()
        assertEquals(emptyList<Long>(), upserts)
    }

    @Test
    fun `given two clicks on different cells when done quickly then no creation is requested`() {
        val upserts = mutableListOf<Long>()
        val first = LocalDate.of(2026, 11, 10)
        val second = LocalDate.of(2026, 11, 11)
        setScreen(createViewModel(), onUpsert = { upserts += it })

        composeRule.onNodeWithTag(cellTag(first)).performClick()
        composeRule.onNodeWithTag(cellTag(second)).performClick()
        composeRule.waitForIdle()

        assertEquals(emptyList<Long>(), upserts)
        composeRule.onNodeWithTag(cellTag(second)).assertIsSelected()
    }

    @Test
    fun `given two horizontal swipes starting and ending on the same cell when done then no creation is requested`() {
        val upserts = mutableListOf<Long>()
        val target = LocalDate.of(2026, 11, 10)
        setScreen(createViewModel(), onUpsert = { upserts += it })

        composeRule.onNodeWithTag(cellTag(target)).performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag(cellTag(target)).performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assertEquals(emptyList<Long>(), upserts)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `given the add accessibility action on a cell when performed then the upsert navigation is requested with that day`() {
        val upserts = mutableListOf<Long>()
        val target = LocalDate.of(2026, 11, 5)
        setScreen(createViewModel(), onUpsert = { upserts += it })

        composeRule
            .onNodeWithTag(cellTag(target))
            .performCustomAccessibilityActionWithLabel("Adicionar agendamento em 5 de novembro")
        composeRule.waitForIdle()

        assertEquals(listOf(target.toEpochDay()), upserts)
    }

    @Test
    fun `given a neighbor month day when double clicked then the month switches and creation is requested`() {
        val upserts = mutableListOf<Long>()
        val target = LocalDate.of(2026, 12, 2)
        setScreen(createViewModel(), onUpsert = { upserts += it })

        composeRule.onNodeWithTag(cellTag(target)).performTouchInput { doubleClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SemanticsTags.HOME_MONTH_HEADER).assertTextEquals("dezembro 2026")
        assertEquals(listOf(target.toEpochDay()), upserts)
    }

    @Test
    fun `given a day without schedules when rendered then the double tap hint is shown`() {
        setScreen(createViewModel())

        composeRule.onNodeWithTag(SemanticsTags.HOME_EMPTY_DAY_HINT).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.HOME_EMPTY_DAY_HINT).assertTextEquals(context.getString(R.string.home_empty_day_hint))
    }

    // endregion
}
