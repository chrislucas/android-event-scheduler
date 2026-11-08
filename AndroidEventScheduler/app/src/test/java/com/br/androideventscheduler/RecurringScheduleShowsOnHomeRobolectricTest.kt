package com.br.androideventscheduler

import android.app.AlarmManager
import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.ui.SemanticsTags
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.LocalDate

/**
 * Ponta a ponta (specs 04 + 05 + 07 + 08) com a `MainActivity` e o grafo real: adição rápida → diálogo de
 * repetição ("Diariamente", 3 vezes) → salvar → a Home lista o item hoje e nos dois dias seguintes, mas não
 * no quarto dia, e o alarme da próxima ocorrência fica agendado.
 */
@RunWith(RobolectricTestRunner::class)
// Tela alta: o formulário de criação e a agenda do dia ficam inteiros na área visível.
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class RecurringScheduleShowsOnHomeRobolectricTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val alarmManager
        get() = ApplicationProvider.getApplicationContext<Context>().getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @After
    fun resetAlarms() {
        ShadowAlarmManager.reset()
    }

    private fun waitUntilWithMainLooper(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun hasNode(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun scheduleTitleOnHome(title: String) = hasText(title) and hasAnyAncestor(hasTestTag(SemanticsTags.HOME_SCHEDULE_LIST))

    private fun isTitleListed(title: String) = composeRule.onAllNodes(scheduleTitleOnHome(title)).fetchSemanticsNodes().isNotEmpty()

    /** Seleciona o dia na grade; se ele estiver no mês seguinte (fora da grade atual), avança o mês antes. */
    private fun selectDay(date: LocalDate) {
        val cellTag = SemanticsTags.HOME_DAY_CELL_PREFIX + date.toEpochDay()
        if (!hasNode(cellTag)) {
            composeRule.onNodeWithTag(SemanticsTags.HOME_NEXT_MONTH_BUTTON).performClick()
            waitUntilWithMainLooper { hasNode(cellTag) }
        }
        composeRule.onNodeWithTag(cellTag).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `given a daily item limited to three times when saving then the home lists it on the next days and the next alarm is scheduled`() {
        val title = "Tomar remédio ponta a ponta"
        val today = LocalDate.now()

        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_QUICK_ADD_BAR) }
            composeRule.onNodeWithTag(SemanticsTags.HOME_QUICK_ADD_BAR).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.UPSERT_TITLE_INPUT) }
            composeRule.onNodeWithTag(SemanticsTags.UPSERT_TITLE_INPUT).performTextInput(title)

            composeRule.onNodeWithTag(SemanticsTags.UPSERT_RECURRENCE_ROW).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.RECURRENCE_DIALOG) }
            composeRule.onNodeWithTag(SemanticsTags.RECURRENCE_OPTION_DAILY).performClick()
            composeRule.onNodeWithTag(SemanticsTags.RECURRENCE_COUNT_INPUT).performTextInput("3")
            composeRule.onNodeWithTag(SemanticsTags.RECURRENCE_CONFIRM_BUTTON).performClick()
            waitUntilWithMainLooper { !hasNode(SemanticsTags.RECURRENCE_DIALOG) }

            composeRule.onNodeWithTag(SemanticsTags.UPSERT_SAVE_BUTTON).performClick()
            waitUntilWithMainLooper { !hasNode(SemanticsTags.UPSERT_TITLE_INPUT) && isTitleListed(title) }

            val stored = runBlocking { GlobalContext.get().get<ScheduleRepository>().getAll() }.single()
            assertEquals(RecurrencePattern.Daily(occurrenceLimit = 3), stored.recurrencePattern)
            assertEquals(today, stored.startDate)

            selectDay(today.plusDays(1))
            waitUntilWithMainLooper { isTitleListed(title) }
            selectDay(today.plusDays(2))
            waitUntilWithMainLooper { isTitleListed(title) }
            selectDay(today.plusDays(3))
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_EMPTY_DAY) && !isTitleListed(title) }

            val scheduledAlarm = shadowOf(alarmManager).scheduledAlarms.single()
            assertTrue(scheduledAlarm.triggerAtMs > System.currentTimeMillis())
        }
    }
}
