package com.br.androideventscheduler.feature.backup

import android.os.Looper
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.MainActivity
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.backup.data.fileJson
import com.br.androideventscheduler.feature.backup.data.itemJson
import com.br.androideventscheduler.feature.backup.testing.IncomingFileTestSupport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Ponta a ponta (spec 17): arquivos recusados — não são backup, versão futura, corrompido, vazio e acima de 5 MB —
 * mostram o motivo na tela de backup e **nada** é gravado.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class RejectedFileImportRobolectricTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun waitUntilWithMainLooper(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun hasNode(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun assertRefusedWith(
        content: ByteArray,
        messageRes: Int,
    ) {
        val uri = IncomingFileTestSupport.registerAttachment(content)

        ActivityScenario.launch<MainActivity>(IncomingFileTestSupport.viewIntent(uri)).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.BACKUP_IMPORT_ERROR) }

            composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_ERROR).assertTextEquals(context.getString(messageRes))
            assertFalse(hasNode(SemanticsTags.BACKUP_IMPORT_PREVIEW))
            assertEquals(emptyList<Any>(), runBlocking { GlobalContext.get().get<ScheduleRepository>().getAll() })
        }
    }

    @Test
    fun `given a json that is not a rememberme backup when opened then the reason is shown and nothing is written`() {
        assertRefusedWith(fileJson(itemJson(), format = "\"outro-app\"").toByteArray(), R.string.backup_error_not_backup)
    }

    @Test
    fun `given a file from a newer version when opened then the update hint is shown and nothing is written`() {
        assertRefusedWith(fileJson(itemJson(), version = "9").toByteArray(), R.string.backup_error_newer_version)
    }

    @Test
    fun `given a truncated file when opened then it is reported as corrupted and nothing is written`() {
        assertRefusedWith(fileJson(itemJson()).dropLast(15).toByteArray(), R.string.backup_error_corrupted)
    }

    @Test
    fun `given a file with no valid item when opened then it is reported as corrupted and nothing is written`() {
        assertRefusedWith(fileJson(itemJson(startDate = "\"2026-13-45\"")).toByteArray(), R.string.backup_error_corrupted)
    }

    @Test
    fun `given a file without items when opened then the empty file reason is shown and nothing is written`() {
        assertRefusedWith(fileJson("").toByteArray(), R.string.backup_error_empty_file)
    }

    @Test
    fun `given a file above 5 MB when opened then it is refused as too large and nothing is written`() {
        val huge = ByteArray(5 * 1024 * 1024 + 1) { 'x'.code.toByte() }

        assertRefusedWith(huge, R.string.backup_error_too_large)
    }

    @Test
    fun `given a valid file with some invalid items when opened then only the valid ones are offered`() {
        val items = listOf(itemJson(id = "ok"), itemJson(id = "ruim", startTime = "\"99:99\"")).joinToString(",")
        val uri = IncomingFileTestSupport.registerAttachment(fileJson(items).toByteArray())

        ActivityScenario.launch<MainActivity>(IncomingFileTestSupport.viewIntent(uri)).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.BACKUP_IMPORT_PREVIEW) }

            composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_PREVIEW_NEW).assertTextEquals("1 novo")
            composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_PREVIEW_INVALID).assertTextEquals("1 item ignorado por dados inválidos")
            assertEquals(emptyList<Any>(), runBlocking { GlobalContext.get().get<ScheduleRepository>().getAll() })
        }
    }
}
