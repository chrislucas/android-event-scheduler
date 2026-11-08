package com.br.androideventscheduler.feature.detail

import android.os.Looper
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.detail.presentation.screen.ScheduleDetailScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Ponta a ponta com o grafo real: tela → ViewModel (Koin, dispatcher main) → repositório (Room em memória).
 * O `idle()` drena o looper principal, onde o Room retoma o resultado vindo da thread de I/O.
 */
@RunWith(RobolectricTestRunner::class)
class DetailFlowRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val repository: ScheduleRepository get() = GlobalContext.get().get()

    private fun waitUntilWithMainLooper(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun tagCount(tag: String): Int = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    @Test
    fun `given stored schedule when opening detail and tapping edit then it shows the item and asks to edit its id`() {
        runBlocking { repository.upsert(scheduleItem(id = "stored-1", title = "Consulta", location = "Clínica")) }
        val editedIds = mutableListOf<String>()

        composeRule.setContent {
            AndroidEventSchedulerTheme {
                ScheduleDetailScreen(scheduleId = "stored-1", onBack = {}, onEdit = { editedIds += it })
            }
        }
        waitUntilWithMainLooper { tagCount(SemanticsTags.DETAIL_TITLE) > 0 }
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_TITLE).assertTextContains("Consulta")
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_LOCATION).assertTextContains("Clínica")
        composeRule.onNodeWithTag(SemanticsTags.DETAIL_EDIT_BUTTON).performClick()
        waitUntilWithMainLooper { editedIds.isNotEmpty() }

        assertEquals(listOf("stored-1"), editedIds)
    }

    @Test
    fun `given unknown schedule id when opening detail then it navigates back`() {
        var backCalls = 0

        composeRule.setContent {
            AndroidEventSchedulerTheme {
                ScheduleDetailScreen(scheduleId = "does-not-exist", onBack = { backCalls++ }, onEdit = {})
            }
        }
        waitUntilWithMainLooper { backCalls > 0 }

        assertEquals(1, backCalls)
    }

    @Test
    fun `given open detail when the item is deleted elsewhere then the screen navigates back`() {
        runBlocking { repository.upsert(scheduleItem(id = "to-delete", title = "Temporário")) }
        var backCalls = 0
        composeRule.setContent {
            AndroidEventSchedulerTheme {
                ScheduleDetailScreen(scheduleId = "to-delete", onBack = { backCalls++ }, onEdit = {})
            }
        }
        waitUntilWithMainLooper { tagCount(SemanticsTags.DETAIL_TITLE) > 0 }

        runBlocking { repository.delete("to-delete") }
        waitUntilWithMainLooper { backCalls > 0 }

        assertEquals(1, backCalls)
    }
}
