package com.br.androideventscheduler

import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
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
 * Ponta a ponta (specs 05 + 10 + 11) com a `MainActivity` e o grafo real: Home → detalhe → excluir →
 * confirmar → volta para a Home, que deixa de listar o agendamento.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR-w411dp-h891dp")
class DeleteScheduleFromHomeRobolectricTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private fun waitUntilWithMainLooper(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun hasNode(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `given a schedule today when deleting it from its detail then the home no longer lists it`() {
        val repository = GlobalContext.get().get<ScheduleRepository>()
        runBlocking { repository.upsert(scheduleItem(id = "to-delete", title = "Reunião", startDate = LocalDate.now())) }
        val itemTag = SemanticsTags.HOME_SCHEDULE_ITEM_PREFIX + "to-delete"

        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilWithMainLooper { hasNode(itemTag) }
            composeRule.onNodeWithTag(itemTag).performClick()

            waitUntilWithMainLooper { hasNode(SemanticsTags.DETAIL_DELETE_BUTTON) }
            composeRule.onNodeWithTag(SemanticsTags.DETAIL_DELETE_BUTTON).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.DELETE_CONFIRM_BUTTON) }
            composeRule.onNodeWithTag(SemanticsTags.DELETE_CONFIRM_BUTTON).performClick()

            waitUntilWithMainLooper {
                !hasNode(SemanticsTags.DETAIL_TITLE) && hasNode(SemanticsTags.HOME_EMPTY_DAY) && !hasNode(itemTag)
            }

            assertEquals(null, runBlocking { repository.getById("to-delete") })
        }
    }
}
