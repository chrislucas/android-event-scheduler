package com.br.androideventscheduler

import android.os.Looper
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.ui.SemanticsTags
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Ponta a ponta (specs 05 + 07) com a `MainActivity` e o grafo real: Splash → Home → adição rápida →
 * criar/editar → salvar → volta para a Home, que lista o novo agendamento no dia selecionado (hoje).
 */
@RunWith(RobolectricTestRunner::class)
// Tela alta: o formulário de criação e a agenda do dia ficam inteiros na área visível.
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class CreateScheduleShowsOnHomeRobolectricTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private fun waitUntilWithMainLooper(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun hasNode(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun scheduleTitleOnHome(title: String) = hasText(title) and hasAnyAncestor(hasTestTag(SemanticsTags.HOME_SCHEDULE_LIST))

    @Test
    fun `given the home when creating a schedule through quick add and saving then it is listed on the home`() {
        val title = "Dentista ponta a ponta"

        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_QUICK_ADD_BAR) }
            composeRule.onNodeWithTag(SemanticsTags.HOME_QUICK_ADD_BAR).performClick()

            waitUntilWithMainLooper { hasNode(SemanticsTags.UPSERT_TITLE_INPUT) }
            composeRule.onNodeWithTag(SemanticsTags.UPSERT_TITLE_INPUT).performTextInput(title)
            composeRule.onNodeWithTag(SemanticsTags.UPSERT_SAVE_BUTTON).performClick()

            waitUntilWithMainLooper {
                !hasNode(SemanticsTags.UPSERT_TITLE_INPUT) &&
                    composeRule.onAllNodes(scheduleTitleOnHome(title)).fetchSemanticsNodes().isNotEmpty()
            }

            val stored = runBlocking { GlobalContext.get().get<ScheduleRepository>().getAll() }
            assertEquals(listOf(title), stored.map { it.title })
            assertEquals(LocalDate.now(), stored.single().startDate)
        }
    }
}
