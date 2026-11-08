package com.br.androideventscheduler.feature.backup.testing

import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.feature.backup.domain.model.ExportResult
import com.br.androideventscheduler.feature.backup.domain.model.ExportTarget
import com.br.androideventscheduler.feature.backup.domain.model.ImportPreview
import com.br.androideventscheduler.feature.backup.domain.repository.AgendaBackupRepository
import kotlinx.coroutines.CompletableDeferred

/**
 * Repositório de backup de teste: respostas configuráveis e chamadas registradas. [exportGate] e [readGate], quando
 * definidos, seguram a operação até serem completados (para testar o estado "em andamento" e toques repetidos).
 */
class FakeAgendaBackupRepository : AgendaBackupRepository {
    var itemCount: Int = 3
    var countError: Throwable? = null
    var exportResult: Result<ExportResult> = Result.success(ExportResult("content://fp/exports/a.json", "a.json", 3))
    var readResult: Result<ImportPreview> = Result.success(samplePreview())
    var exportGate: CompletableDeferred<Unit>? = null
    var readGate: CompletableDeferred<Unit>? = null

    val exportTargets = mutableListOf<ExportTarget>()
    val readUris = mutableListOf<String>()

    override suspend fun countItems(): Int = countError?.let { throw it } ?: itemCount

    override suspend fun export(target: ExportTarget): ExportResult {
        exportTargets += target
        exportGate?.await()
        return exportResult.getOrThrow()
    }

    override suspend fun readForImport(uri: String): ImportPreview {
        readUris += uri
        readGate?.await()
        return readResult.getOrThrow()
    }

    companion object {
        fun samplePreview(
            newCount: Int = 2,
            existingCount: Int = 1,
            invalidCount: Int = 0,
        ): ImportPreview {
            val existing = List(existingCount) { scheduleItem(id = "existing-$it") }
            val new = List(newCount) { scheduleItem(id = "new-$it") }
            return ImportPreview(
                validItems = new + existing,
                newCount = newCount,
                existingCount = existingCount,
                invalidCount = invalidCount,
                existingIds = existing.map { it.id }.toSet(),
            )
        }
    }
}
