package com.br.androideventscheduler.feature.detail

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.detail.presentation.screen.ScheduleDetailScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
                ScheduleDetailScreen(scheduleId = "stored-1", onBack = {}, onEdit = { editedIds += it }, onOpenCopy = {})
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
                ScheduleDetailScreen(scheduleId = "does-not-exist", onBack = { backCalls++ }, onEdit = {}, onOpenCopy = {})
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
                ScheduleDetailScreen(scheduleId = "to-delete", onBack = { backCalls++ }, onEdit = {}, onOpenCopy = {})
            }
        }
        waitUntilWithMainLooper { tagCount(SemanticsTags.DETAIL_TITLE) > 0 }

        runBlocking { repository.delete("to-delete") }
        waitUntilWithMainLooper { backCalls > 0 }

        assertEquals(1, backCalls)
    }

    @Test
    fun `given open detail when deleting through the dialog then the item is removed and the screen goes back once`() {
        runBlocking { repository.upsert(scheduleItem(id = "doomed", title = "Consulta")) }
        var backCalls = 0
        composeRule.setContent {
            AndroidEventSchedulerTheme {
                ScheduleDetailScreen(scheduleId = "doomed", onBack = { backCalls++ }, onEdit = {}, onOpenCopy = {})
            }
        }
        waitUntilWithMainLooper { tagCount(SemanticsTags.DETAIL_TITLE) > 0 }

        composeRule.onNodeWithTag(SemanticsTags.DETAIL_DELETE_BUTTON).performClick()
        waitUntilWithMainLooper { tagCount(SemanticsTags.DELETE_CONFIRM_DIALOG) > 0 }
        composeRule.onNodeWithTag(SemanticsTags.DELETE_CONFIRM_BUTTON).performClick()
        waitUntilWithMainLooper { backCalls > 0 }
        composeRule.waitForIdle()

        assertEquals(1, backCalls)
        assertNull(runBlocking { repository.getById("doomed") })
        assertEquals(0, tagCount(SemanticsTags.DELETE_CONFIRM_DIALOG))
    }

    @Test
    fun `given open detail when cancelling the delete dialog then the item stays and the screen does not go back`() {
        runBlocking { repository.upsert(scheduleItem(id = "kept", title = "Consulta")) }
        var backCalls = 0
        composeRule.setContent {
            AndroidEventSchedulerTheme {
                ScheduleDetailScreen(scheduleId = "kept", onBack = { backCalls++ }, onEdit = {}, onOpenCopy = {})
            }
        }
        waitUntilWithMainLooper { tagCount(SemanticsTags.DETAIL_TITLE) > 0 }

        composeRule.onNodeWithTag(SemanticsTags.DETAIL_DELETE_BUTTON).performClick()
        waitUntilWithMainLooper { tagCount(SemanticsTags.DELETE_CONFIRM_DIALOG) > 0 }
        composeRule.onNodeWithTag(SemanticsTags.DELETE_CANCEL_BUTTON).performClick()
        waitUntilWithMainLooper { tagCount(SemanticsTags.DELETE_CONFIRM_DIALOG) == 0 }

        assertEquals(0, backCalls)
        assertNotNull(runBlocking { repository.getById("kept") })
    }

    @Test
    fun `given recurring item when opening the delete dialog then it warns about all occurrences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            repository.upsert(scheduleItem(id = "series", title = "Academia", recurrencePattern = RecurrencePattern.Daily()))
        }
        composeRule.setContent {
            AndroidEventSchedulerTheme {
                ScheduleDetailScreen(scheduleId = "series", onBack = {}, onEdit = {}, onOpenCopy = {})
            }
        }
        waitUntilWithMainLooper { tagCount(SemanticsTags.DETAIL_TITLE) > 0 }

        composeRule.onNodeWithTag(SemanticsTags.DETAIL_DELETE_BUTTON).performClick()
        waitUntilWithMainLooper { tagCount(SemanticsTags.DELETE_CONFIRM_DIALOG) > 0 }

        composeRule.onNodeWithText(context.getString(R.string.delete_dialog_message_recurring)).assertExists()
    }

    @Test
    fun `given open detail when duplicating then the copy is stored and the snackbar open action navigates to it`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking { repository.upsert(scheduleItem(id = "source", title = "Consulta", location = "Clínica")) }
        val openedCopyIds = mutableListOf<String>()
        composeRule.setContent {
            AndroidEventSchedulerTheme {
                ScheduleDetailScreen(
                    scheduleId = "source",
                    onBack = {},
                    onEdit = {},
                    onOpenCopy = { openedCopyIds += it },
                )
            }
        }
        waitUntilWithMainLooper { tagCount(SemanticsTags.DETAIL_TITLE) > 0 }

        composeRule.onNodeWithTag(SemanticsTags.DETAIL_DUPLICATE_BUTTON).performClick()
        waitUntilWithMainLooper {
            composeRule.onAllNodesWithText(context.getString(R.string.detail_copy_created)).fetchSemanticsNodes().isNotEmpty()
        }

        val copy = runBlocking { repository.getAll() }.single { it.id != "source" }
        assertEquals("Consulta (Cópia)", copy.title)
        assertEquals("Clínica", copy.location)
        composeRule.onNodeWithText(context.getString(R.string.detail_copy_open_action)).performClick()
        waitUntilWithMainLooper { openedCopyIds.isNotEmpty() }
        assertEquals(listOf(copy.id), openedCopyIds)
        assertEquals("Consulta", runBlocking { repository.getById("source") }?.title)
    }
}
