package com.br.androideventscheduler

import android.os.Looper
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.ui.SemanticsTags
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Ponta a ponta (specs 05 + 07 + 18) com a `MainActivity` e o grafo real: toque duplo num dia da grade → criação com
 * esse dia → Evento → salvar → a Home mostra o item no dia tocado. Também cobre cancelar e a guarda contra duas
 * criações empilhadas.
 */
@RunWith(RobolectricTestRunner::class)
// Tela alta: o formulário de criação e a agenda do dia ficam inteiros na área visível.
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class DoubleTapDayCreatesScheduleRobolectricTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private fun waitUntilWithMainLooper(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun hasNode(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun cellTag(date: LocalDate) = SemanticsTags.HOME_DAY_CELL_PREFIX + date.toEpochDay()

    private fun scheduleTitleOnHome(title: String) = hasText(title) and hasAnyAncestor(hasTestTag(SemanticsTags.HOME_SCHEDULE_LIST))

    private fun storedSchedules() = runBlocking { GlobalContext.get().get<ScheduleRepository>().getAll() }

    // A grade só aparece quando o estado sai de Loading.
    private fun waitForHome(target: LocalDate) = waitUntilWithMainLooper { hasNode(cellTag(target)) }

    // Dia +3 a partir de hoje; se cair em outro mês, o toque duplo também troca o mês (R3), então usa +1 no fim do mês.
    private fun targetDay(): LocalDate {
        val today = LocalDate.now()
        return today.plusDays(3).takeIf { it.month == today.month } ?: today.minusDays(3)
    }

    @Test
    fun `given the home when double tapping a day then switching to event and saving then the event is stored and shown on that day`() {
        val title = "Toque duplo ponta a ponta"
        val target = targetDay()

        ActivityScenario.launch(MainActivity::class.java).use {
            waitForHome(target)
            composeRule.onNodeWithTag(cellTag(target)).performTouchInput { doubleClick() }

            waitUntilWithMainLooper { hasNode(SemanticsTags.UPSERT_TITLE_INPUT) }
            composeRule.onNodeWithTag(SemanticsTags.UPSERT_MODE_EVENT_TAB).performClick()
            composeRule.onNodeWithTag(SemanticsTags.UPSERT_TITLE_INPUT).performTextInput(title)
            composeRule.onNodeWithTag(SemanticsTags.UPSERT_SAVE_BUTTON).performClick()

            waitUntilWithMainLooper {
                !hasNode(SemanticsTags.UPSERT_TITLE_INPUT) &&
                    composeRule.onAllNodes(scheduleTitleOnHome(title)).fetchSemanticsNodes().isNotEmpty()
            }

            val stored = storedSchedules().single()
            assertEquals(title, stored.title)
            assertEquals(ScheduleType.EVENT, stored.type)
            assertEquals(target, stored.startDate)
            composeRule.onNodeWithTag(cellTag(target)).assertIsSelected()
        }
    }

    @Test
    fun `given a double tap on a day when cancelling then nothing is stored and the day stays selected`() {
        val target = targetDay()

        ActivityScenario.launch(MainActivity::class.java).use {
            waitForHome(target)
            composeRule.onNodeWithTag(cellTag(target)).performTouchInput { doubleClick() }

            waitUntilWithMainLooper { hasNode(SemanticsTags.UPSERT_CANCEL_BUTTON) }
            composeRule.onNodeWithTag(SemanticsTags.UPSERT_CANCEL_BUTTON).performClick()

            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_QUICK_ADD_BAR) && !hasNode(SemanticsTags.UPSERT_TITLE_INPUT) }
            assertTrue(storedSchedules().isEmpty())
            composeRule.onNodeWithTag(cellTag(target)).assertIsSelected()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `given a double tap followed by quick add before the first navigation is handled when going back once then the home shows`() {
        val target = targetDay()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome(target)
            // Sem avançar quadros: os dois NavigateToUpsert chegam antes de a navegação do primeiro ser aplicada.
            composeRule.mainClock.autoAdvance = false
            composeRule.onNodeWithTag(cellTag(target)).performTouchInput { doubleClick() }
            composeRule.onNodeWithTag(SemanticsTags.HOME_QUICK_ADD_BAR).performClick()
            composeRule.mainClock.autoAdvance = true

            waitUntilWithMainLooper { hasNode(SemanticsTags.UPSERT_TITLE_INPUT) }
            scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }

            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_QUICK_ADD_BAR) && !hasNode(SemanticsTags.UPSERT_TITLE_INPUT) }
            assertTrue(storedSchedules().isEmpty())
        }
    }
}
