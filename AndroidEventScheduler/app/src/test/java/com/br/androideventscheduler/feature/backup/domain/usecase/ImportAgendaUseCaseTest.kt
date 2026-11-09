package com.br.androideventscheduler.feature.backup.domain.usecase

import com.br.androideventscheduler.core.alarm.domain.usecase.ScheduleNextAlarmUseCase
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.feature.backup.domain.model.ImportPreview
import com.br.androideventscheduler.feature.backup.domain.model.ImportResult
import com.br.androideventscheduler.feature.backup.domain.model.ImportStrategy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ImportAgendaUseCaseTest {
    private val repository = mockk<ScheduleRepository>(relaxUnitFun = true)
    private val scheduleNextAlarm = mockk<ScheduleNextAlarmUseCase>(relaxed = true)
    private val copyIds = ArrayDeque(listOf("copy-1", "copy-2", "copy-3"))
    private val useCase = ImportAgendaUseCase(repository, scheduleNextAlarm, idProvider = { copyIds.removeFirst() })

    private val newItem = scheduleItem(id = "new", title = "Novo")
    private val existingA = scheduleItem(id = "a", title = "Do arquivo A")
    private val existingB = scheduleItem(id = "b", title = "Do arquivo B")

    private fun preview(
        valid: List<ScheduleItem> = listOf(newItem, existingA, existingB),
        existingIds: Set<String> = setOf("a", "b"),
        invalidCount: Int = 0,
    ) = ImportPreview(
        validItems = valid,
        newCount = valid.count { it.id !in existingIds },
        existingCount = valid.count { it.id in existingIds },
        invalidCount = invalidCount,
        existingIds = existingIds,
    )

    private fun capturedUpsertAll(): List<ScheduleItem> {
        val slot = mutableListOf<List<ScheduleItem>>()
        coVerify(exactly = 1) { repository.upsertAll(capture(slot)) }
        return slot.single()
    }

    @Test
    fun `given replace strategy when importing then all valid items are saved in a single transaction`() =
        runTest {
            val result = useCase(preview(), ImportStrategy.REPLACE_EXISTING, " (Importado)").getOrThrow()

            assertEquals(listOf(newItem, existingA, existingB), capturedUpsertAll())
            assertEquals(ImportResult(importedCount = 3, skippedCount = 0, alarmFailures = 0), result)
        }

    @Test
    fun `given keep existing strategy when importing then only items with new ids are saved`() =
        runTest {
            val result = useCase(preview(), ImportStrategy.KEEP_EXISTING, " (Importado)").getOrThrow()

            assertEquals(listOf(newItem), capturedUpsertAll())
            assertEquals(ImportResult(importedCount = 1, skippedCount = 2, alarmFailures = 0), result)
        }

    @Test
    fun `given copies strategy when importing then existing items get new ids and suffixed titles and new ones stay`() =
        runTest {
            val result = useCase(preview(), ImportStrategy.IMPORT_AS_COPIES, " (Importado)").getOrThrow()

            assertEquals(
                listOf(
                    newItem,
                    existingA.copy(id = "copy-1", title = "Do arquivo A (Importado)"),
                    existingB.copy(id = "copy-2", title = "Do arquivo B (Importado)"),
                ),
                capturedUpsertAll(),
            )
            assertEquals(3, result.importedCount)
        }

    @Test
    fun `given copies strategy and a long title when importing then the suffixed title stays within the limit`() =
        runTest {
            val longTitle = "t".repeat(200)
            val item = scheduleItem(id = "a", title = longTitle)

            useCase(preview(valid = listOf(item), existingIds = setOf("a")), ImportStrategy.IMPORT_AS_COPIES, " (Importado)").getOrThrow()

            val savedTitle = capturedUpsertAll().single().title
            assertEquals(200, savedTitle.length)
            assertTrue(savedTitle.endsWith(" (Importado)"))
        }

    @Test
    fun `given imported items when importing then the next alarm is scheduled once per saved item`() =
        runTest {
            useCase(preview(), ImportStrategy.KEEP_EXISTING, " (Importado)").getOrThrow()

            verify(exactly = 1) { scheduleNextAlarm(newItem) }
            verify(exactly = 0) { scheduleNextAlarm(existingA) }
            verify(exactly = 0) { scheduleNextAlarm(existingB) }
        }

    @Test
    fun `given an alarm that fails when importing then the import still succeeds and the failure is counted`() =
        runTest {
            every { scheduleNextAlarm(existingA) } throws SecurityException("exact alarm denied")

            val result = useCase(preview(), ImportStrategy.REPLACE_EXISTING, " (Importado)").getOrThrow()

            assertEquals(ImportResult(importedCount = 3, skippedCount = 0, alarmFailures = 1), result)
            verify(exactly = 1) { scheduleNextAlarm(newItem) }
            verify(exactly = 1) { scheduleNextAlarm(existingB) }
        }

    @Test
    fun `given the transaction fails when importing then the result is a failure and no alarm is scheduled`() =
        runTest {
            coEvery { repository.upsertAll(any()) } throws IOException("disk full")

            val result = useCase(preview(), ImportStrategy.REPLACE_EXISTING, " (Importado)")

            assertTrue(result.isFailure)
            verify(exactly = 0) { scheduleNextAlarm(any()) }
        }

    @Test
    fun `given the same file imported twice with replace when importing then the saved items are identical`() =
        runTest {
            val saved = mutableListOf<List<ScheduleItem>>()
            coEvery { repository.upsertAll(capture(saved)) } returns Unit

            useCase(preview(), ImportStrategy.REPLACE_EXISTING, " (Importado)").getOrThrow()
            useCase(preview(), ImportStrategy.REPLACE_EXISTING, " (Importado)").getOrThrow()

            assertEquals(2, saved.size)
            assertEquals(saved[0], saved[1])
            assertTrue(saved.all { batch -> batch.map { it.id } == listOf("new", "a", "b") })
        }

    @Test
    fun `given a preview without valid items when importing then it saves nothing and reports zero`() =
        runTest {
            val result =
                useCase(preview(valid = emptyList(), existingIds = emptySet(), invalidCount = 2), ImportStrategy.REPLACE_EXISTING, "x")
                    .getOrThrow()

            assertEquals(ImportResult(importedCount = 0, skippedCount = 0, alarmFailures = 0), result)
        }
}
