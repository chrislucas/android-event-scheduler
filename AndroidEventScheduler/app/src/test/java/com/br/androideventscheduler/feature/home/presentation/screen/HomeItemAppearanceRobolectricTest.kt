package com.br.androideventscheduler.feature.home.presentation.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.preferences.testing.FakeUserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.EventColorOption
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.FakeClock
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.home.domain.calendar.MonthGridBuilder
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

/**
 * Spec 09, R5: a cor do item chega aos indicadores da grade e à barra da lista, e o adesivo do item à linha da lista.
 * Os pixels não são legíveis nos testes Robolectric deste projeto (`captureToImage` expira), então a cor desenhada
 * é conferida pela tag do elemento, que carrega o `colorHex` recebido do estado.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR-w411dp-h891dp")
class HomeItemAppearanceRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 11, 7)
    private val repository = FakeHomeScheduleRepository()

    private fun showHome() {
        val viewModel =
            HomeViewModel(
                scheduleRepository = repository,
                userSettingsRepository = FakeUserSettingsRepository(),
                homePreferencesRepository = FakeHomePreferencesRepository(),
                monthGridBuilder = MonthGridBuilder(),
                clock = FakeClock(),
                savedStateHandle = SavedStateHandle(),
                dispatcher = Dispatchers.Unconfined,
            )
        composeRule.setContent {
            AndroidEventSchedulerTheme {
                HomeScreen(
                    onNavigateToSearch = {},
                    onNavigateToUpsert = {},
                    onNavigateToDetail = {},
                    onNavigateToSettings = {},
                    viewModel = viewModel,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun barTag(
        id: String,
        color: EventColorOption,
    ) = "${SemanticsTags.HOME_SCHEDULE_COLOR_BAR_PREFIX}${id}_${color.hex}"

    private fun indicatorTag(
        date: LocalDate,
        color: EventColorOption,
    ) = "${SemanticsTags.HOME_DAY_INDICATOR_PREFIX}${date.toEpochDay()}_${color.hex}"

    private fun tagCount(tag: String) = composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun `given an item with color and sticker when shown then the list bar has its color and the row its emoji`() {
        repository.items.value =
            listOf(
                scheduleItem(id = "a", title = "Treino", startDate = today, startTime = LocalTime.of(7, 0))
                    .copy(colorHex = EventColorOption.CORAL.hex, stickerEmoji = "🥋"),
            )
        showHome()

        composeRule.onNodeWithTag(barTag("a", EventColorOption.CORAL), useUnmergedTree = true).assertIsDisplayed()
        composeRule
            .onNode(hasText("🥋") and hasAnyAncestor(hasTestTag(SemanticsTags.HOME_SCHEDULE_ITEM_PREFIX + "a")), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `given an item without sticker when shown then the row has no emoji`() {
        repository.items.value = listOf(scheduleItem(id = "n", startDate = today))
        showHome()

        assertEquals(
            0,
            composeRule
                .onAllNodes(
                    hasText("🥋") and hasAnyAncestor(hasTestTag(SemanticsTags.HOME_SCHEDULE_ITEM_PREFIX + "n")),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
                .size,
        )
    }

    @Test
    fun `given an item with a color when shown then the day cell indicator uses that color`() {
        repository.items.value =
            listOf(scheduleItem(id = "b", title = "Estudo", startDate = today).copy(colorHex = EventColorOption.PURPLE.hex))
        showHome()

        composeRule.onNodeWithTag(indicatorTag(today, EventColorOption.PURPLE), useUnmergedTree = true).assertIsDisplayed()
        assertEquals(0, tagCount(indicatorTag(today, EventColorOption.CORAL)))
    }

    @Test
    fun `given items with different colors on one day when shown then the cell has one indicator per color`() {
        repository.items.value =
            listOf(
                scheduleItem(id = "c1", startDate = today, startTime = LocalTime.of(8, 0)).copy(colorHex = EventColorOption.TEAL.hex),
                scheduleItem(id = "c2", startDate = today, startTime = LocalTime.of(9, 0)).copy(colorHex = EventColorOption.INDIGO.hex),
            )
        showHome()

        composeRule.onNodeWithTag(indicatorTag(today, EventColorOption.TEAL), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(indicatorTag(today, EventColorOption.INDIGO), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `given an item on another day when that day is selected then its bar color is shown in the list`() {
        val tomorrow = today.plusDays(1)
        repository.items.value =
            listOf(scheduleItem(id = "d", startDate = tomorrow).copy(colorHex = EventColorOption.OLIVE_GREEN.hex, stickerEmoji = "☕"))
        showHome()
        assertEquals(0, tagCount(barTag("d", EventColorOption.OLIVE_GREEN)))

        composeRule.onNodeWithTag(SemanticsTags.HOME_DAY_CELL_PREFIX + tomorrow.toEpochDay()).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(barTag("d", EventColorOption.OLIVE_GREEN), useUnmergedTree = true).assertIsDisplayed()
    }
}
