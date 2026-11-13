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
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.ui.SemanticsTags
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Ponta a ponta (specs 04 + 05 + 07 + 20) com a `MainActivity` e o grafo real: Home → amanhã → adição rápida →
 * Alerta "5 minutos antes" → salvar → o item é gravado com `FIVE_MINUTES`, aparece na Home e o alarme fica agendado
 * 5 minutos antes do início. O dia é amanhã para que o gatilho (próxima hora cheia − 5 min) nunca caia no passado.
 */
@RunWith(RobolectricTestRunner::class)
// Tela alta: o formulário de criação com a lista de alertas aberta e a agenda do dia ficam inteiros na área visível.
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class FiveMinuteReminderRobolectricTest {
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

    private fun isTitleListed(title: String) =
        composeRule
            .onAllNodes(hasText(title) and hasAnyAncestor(hasTestTag(SemanticsTags.HOME_SCHEDULE_LIST)))
            .fetchSemanticsNodes()
            .isNotEmpty()

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
    fun `given five minutes chosen in the form when saving from home then the item is listed and the alarm rings five minutes before`() {
        val title = "Alerta de 5 minutos ponta a ponta"
        val tomorrow = LocalDate.now().plusDays(1)

        ActivityScenario.launch(MainActivity::class.java).use {
            // A barra de adição rápida aparece antes da grade: espera a célula de hoje para selecionar o dia.
            val todayCell = SemanticsTags.HOME_DAY_CELL_PREFIX + LocalDate.now().toEpochDay()
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_QUICK_ADD_BAR) && hasNode(todayCell) }
            selectDay(tomorrow)
            composeRule.onNodeWithTag(SemanticsTags.HOME_QUICK_ADD_BAR).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.UPSERT_TITLE_INPUT) }
            composeRule.onNodeWithTag(SemanticsTags.UPSERT_TITLE_INPUT).performTextInput(title)

            composeRule.onNodeWithTag(SemanticsTags.UPSERT_REMINDER_ROW).performClick()
            val fiveMinutesOption = SemanticsTags.UPSERT_REMINDER_OPTION_PREFIX + ReminderOffset.FIVE_MINUTES.name
            waitUntilWithMainLooper { hasNode(fiveMinutesOption) }
            composeRule.onNodeWithTag(fiveMinutesOption).performClick()

            composeRule.onNodeWithTag(SemanticsTags.UPSERT_SAVE_BUTTON).performClick()
            waitUntilWithMainLooper { !hasNode(SemanticsTags.UPSERT_TITLE_INPUT) && isTitleListed(title) }

            val stored = runBlocking { GlobalContext.get().get<ScheduleRepository>().getAll() }.single()
            assertEquals(ReminderOffset.FIVE_MINUTES, stored.reminderOffset)
            assertEquals(tomorrow, stored.startDate)
            val expectedTrigger =
                LocalDateTime
                    .of(stored.startDate, stored.startTime)
                    .atZone(ZoneId.systemDefault())
                    .minusMinutes(5)
                    .toInstant()
                    .toEpochMilli()
            assertEquals(expectedTrigger, shadowOf(alarmManager).scheduledAlarms.single().triggerAtMs)
        }
    }
}
