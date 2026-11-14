package com.br.androideventscheduler.feature.backup

import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.MainActivity
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.backup.data.fullyPopulatedItems
import com.br.androideventscheduler.feature.backup.domain.model.ExportTarget
import com.br.androideventscheduler.feature.backup.domain.repository.AgendaBackupRepository
import com.br.androideventscheduler.feature.backup.testing.IncomingFileTestSupport
import com.br.androideventscheduler.feature.backup.testing.resetFileProviderCache
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * Ponta a ponta (spec 17), grafo real: a agenda é exportada para o cache, apagada e o **conteúdo gerado** é reaberto
 * como anexo recebido; depois de confirmar, a agenda volta idêntica (todos os campos e padrões de recorrência).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class ExportImportRoundTripRobolectricTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val scheduleRepository: ScheduleRepository get() = GlobalContext.get().get()
    private val backupRepository: AgendaBackupRepository get() = GlobalContext.get().get()

    @Before
    fun setUp() {
        resetFileProviderCache()
    }

    private fun waitUntilWithMainLooper(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun hasNode(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `given an exported agenda when its generated file is received and imported then the agenda is restored identical`() {
        val original = fullyPopulatedItems()
        val exported =
            runBlocking {
                scheduleRepository.upsertAll(original)
                backupRepository.export(ExportTarget.ShareCache)
            }
        val cacheFile = File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "exports/${exported.fileName}")
        assertTrue(cacheFile.exists())
        assertEquals(original.size, exported.itemCount)
        runBlocking { original.forEach { scheduleRepository.delete(it.id) } }
        assertEquals(emptyList<Any>(), runBlocking { scheduleRepository.getAll() })
        val uri = IncomingFileTestSupport.registerAttachment(cacheFile.readBytes(), exported.fileName)

        ActivityScenario.launch<MainActivity>(IncomingFileTestSupport.viewIntent(uri)).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.BACKUP_IMPORT_PREVIEW) }
            composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_CONFIRM_BUTTON).performScrollTo().performClick()
            waitUntilWithMainLooper {
                !hasNode(SemanticsTags.BACKUP_IMPORT_PREVIEW) &&
                    runBlocking { scheduleRepository.getAll() }.size == original.size
            }
        }

        val restored = runBlocking { scheduleRepository.getAll() }
        assertEquals(original.sortedBy { it.id }, restored.sortedBy { it.id })
    }

    @Test
    fun `given the same exported file imported twice with replace then the agenda is the same`() {
        val original = fullyPopulatedItems()
        val exported =
            runBlocking {
                scheduleRepository.upsertAll(original)
                backupRepository.export(ExportTarget.ShareCache)
            }
        val cacheFile = File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "exports/${exported.fileName}")
        val uri = IncomingFileTestSupport.registerAttachment(cacheFile.readBytes(), exported.fileName)

        repeat(2) {
            ActivityScenario.launch<MainActivity>(IncomingFileTestSupport.viewIntent(uri)).use {
                waitUntilWithMainLooper { hasNode(SemanticsTags.BACKUP_IMPORT_PREVIEW) }
                composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_CONFIRM_BUTTON).performScrollTo().performClick()
                waitUntilWithMainLooper { !hasNode(SemanticsTags.BACKUP_IMPORT_PREVIEW) }
            }
        }

        assertEquals(original.sortedBy { it.id }, runBlocking { scheduleRepository.getAll() }.sortedBy { it.id })
    }
}
