package com.br.androideventscheduler.feature.backup

import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import com.br.androideventscheduler.MainActivity
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.backup.data.fileJson
import com.br.androideventscheduler.feature.backup.data.itemJson
import com.br.androideventscheduler.feature.backup.testing.IncomingFileTestSupport
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
 * Ponta a ponta (specs 17 + 05) com a `MainActivity` e o grafo real: um arquivo recebido por `ACTION_VIEW`/`ACTION_SEND`
 * abre a pré-visualização depois da Splash e, ao confirmar, os itens aparecem na Home.
 */
@RunWith(RobolectricTestRunner::class)
// Tela alta: a pré-visualização e a agenda do dia ficam inteiras na área visível.
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class IncomingFileRobolectricTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val repository: ScheduleRepository get() = GlobalContext.get().get()

    private fun waitUntilWithMainLooper(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun hasNode(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun scheduleTitleOnHome(title: String) = hasText(title) and hasAnyAncestor(hasTestTag(SemanticsTags.HOME_SCHEDULE_LIST))

    private fun validFile(): ByteArray {
        val today = "\"${LocalDate.now()}\""
        val items =
            listOf(
                itemJson(id = "mail-1", title = "\"Consulta recebida por e-mail\"", startDate = today, endDate = today),
                itemJson(
                    id = "mail-2",
                    title = "\"Almoço recebido por e-mail\"",
                    startDate = today,
                    endDate = today,
                    startTime = "\"12:30\"",
                ),
            ).joinToString(",")
        return fileJson(items).toByteArray()
    }

    @Test
    fun `given a view intent with a valid json when the app opens then the preview shows the file counts`() {
        val uri = IncomingFileTestSupport.registerAttachment(validFile())

        ActivityScenario.launch<MainActivity>(IncomingFileTestSupport.viewIntent(uri)).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.BACKUP_IMPORT_PREVIEW) }

            composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_PREVIEW_TOTAL).assertTextEquals("2 agendamentos no arquivo")
            composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_PREVIEW_NEW).assertTextEquals("2 novos")
            assertEquals(emptyList<Any>(), runBlocking { repository.getAll() })
        }
    }

    @Test
    fun `given a send intent with a valid json when the app opens then the preview is shown too`() {
        val uri = IncomingFileTestSupport.registerAttachment(validFile())

        ActivityScenario.launch<MainActivity>(IncomingFileTestSupport.sendIntent(uri)).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.BACKUP_IMPORT_PREVIEW) }

            composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_PREVIEW_TOTAL).assertTextEquals("2 agendamentos no arquivo")
        }
    }

    @Test
    fun `given a received file when confirming the import then the items are saved and listed on the home`() {
        val uri = IncomingFileTestSupport.registerAttachment(validFile())

        ActivityScenario.launch<MainActivity>(IncomingFileTestSupport.viewIntent(uri)).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.BACKUP_IMPORT_PREVIEW) }
            composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_CONFIRM_BUTTON).performScrollTo().performClick()

            waitUntilWithMainLooper {
                !hasNode(SemanticsTags.BACKUP_IMPORT_PREVIEW) &&
                    composeRule.onAllNodes(scheduleTitleOnHome("Consulta recebida por e-mail")).fetchSemanticsNodes().isNotEmpty() &&
                    composeRule.onAllNodes(scheduleTitleOnHome("Almoço recebido por e-mail")).fetchSemanticsNodes().isNotEmpty()
            }

            val stored = runBlocking { repository.getAll() }
            assertEquals(setOf("mail-1", "mail-2"), stored.map { it.id }.toSet())
        }
    }

    @Test
    fun `given an item that already exists when importing with keep existing then the current one is untouched`() {
        runBlocking { repository.upsert(scheduleItem(id = "mail-1", title = "Atual", startDate = LocalDate.now())) }
        val uri = IncomingFileTestSupport.registerAttachment(validFile())

        ActivityScenario.launch<MainActivity>(IncomingFileTestSupport.viewIntent(uri)).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.BACKUP_IMPORT_PREVIEW) }
            composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_PREVIEW_EXISTING).assertTextEquals("1 já existe na agenda")
            composeRule
                .onNodeWithTag(SemanticsTags.BACKUP_IMPORT_STRATEGY_OPTION_PREFIX + "KEEP_EXISTING")
                .performScrollTo()
                .performClick()
            composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_CONFIRM_BUTTON).performScrollTo().performClick()

            waitUntilWithMainLooper { !hasNode(SemanticsTags.BACKUP_IMPORT_PREVIEW) && runBlocking { repository.getAll() }.size == 2 }

            val titles = runBlocking { repository.getAll() }.associate { it.id to it.title }
            assertEquals("Atual", titles["mail-1"])
            assertEquals("Almoço recebido por e-mail", titles["mail-2"])
        }
    }

    @Test
    fun `given a received file when cancelling the preview then nothing is written`() {
        val uri = IncomingFileTestSupport.registerAttachment(validFile())

        ActivityScenario.launch<MainActivity>(IncomingFileTestSupport.viewIntent(uri)).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.BACKUP_IMPORT_PREVIEW) }
            composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_CANCEL_BUTTON).performScrollTo().performClick()

            waitUntilWithMainLooper { !hasNode(SemanticsTags.BACKUP_IMPORT_PREVIEW) }
            assertEquals(emptyList<Any>(), runBlocking { repository.getAll() })
        }
    }

    @Test
    fun `given a view intent with another mime type when the app opens then it lands on the home without the preview`() {
        val uri = IncomingFileTestSupport.registerAttachment(validFile())
        val intent = IncomingFileTestSupport.viewIntent(uri).setDataAndType(uri, "text/plain").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        ActivityScenario.launch<MainActivity>(intent).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_MENU_BUTTON) }

            assertEquals(false, hasNode(SemanticsTags.BACKUP_IMPORT_PREVIEW))
            assertEquals(false, hasNode(SemanticsTags.BACKUP_EXPORT_SHARE_BUTTON))
        }
    }
}
