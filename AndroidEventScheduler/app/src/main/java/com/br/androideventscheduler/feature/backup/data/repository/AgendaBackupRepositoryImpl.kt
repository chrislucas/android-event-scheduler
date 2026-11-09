package com.br.androideventscheduler.feature.backup.data.repository

import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.feature.backup.data.codec.AgendaFileFormat
import com.br.androideventscheduler.feature.backup.data.codec.AgendaJsonCodec
import com.br.androideventscheduler.feature.backup.data.file.DocumentStreams
import com.br.androideventscheduler.feature.backup.data.file.ExportFileStore
import com.br.androideventscheduler.feature.backup.data.file.mapStorageErrors
import com.br.androideventscheduler.feature.backup.domain.model.BackupError
import com.br.androideventscheduler.feature.backup.domain.model.BackupFileName
import com.br.androideventscheduler.feature.backup.domain.model.ExportResult
import com.br.androideventscheduler.feature.backup.domain.model.ExportTarget
import com.br.androideventscheduler.feature.backup.domain.model.ImportPreview
import com.br.androideventscheduler.feature.backup.domain.repository.AgendaBackupRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate

internal class AgendaBackupRepositoryImpl(
    private val scheduleRepository: ScheduleRepository,
    private val codec: AgendaJsonCodec,
    private val exportFileStore: ExportFileStore,
    private val documentStreams: DocumentStreams,
    private val clock: Clock,
    private val appVersion: String,
    private val ioDispatcher: CoroutineDispatcher,
) : AgendaBackupRepository {
    override suspend fun countItems(): Int = scheduleRepository.getAll().size

    override suspend fun export(target: ExportTarget): ExportResult =
        withContext(ioDispatcher) {
            val items = scheduleRepository.getAll()
            if (items.isEmpty()) throw BackupError.EmptyAgenda
            val content = codec.encode(items, exportedAt = clock.instant(), appVersion = appVersion)
            val fileName = BackupFileName.forDate(LocalDate.now(clock))
            val uri =
                mapStorageErrors {
                    when (target) {
                        ExportTarget.ShareCache -> {
                            exportFileStore.writeShareFile(fileName, content)
                        }

                        is ExportTarget.Document -> {
                            documentStreams.writeText(target.uri, content)
                            target.uri
                        }
                    }
                }
            ExportResult(uri = uri, fileName = fileName, itemCount = items.size)
        }

    override suspend fun readForImport(uri: String): ImportPreview =
        withContext(ioDispatcher) {
            val text = mapStorageErrors { documentStreams.readText(uri, AgendaFileFormat.MAX_FILE_BYTES) }
            val decoded = codec.decode(text)
            if (decoded.items.isEmpty()) throw if (decoded.invalidCount == 0) BackupError.EmptyAgenda else BackupError.Corrupted

            val currentIds = scheduleRepository.getAll().mapTo(HashSet()) { it.id }
            val existingIds = decoded.items.mapNotNullTo(LinkedHashSet()) { item -> item.id.takeIf { it in currentIds } }
            ImportPreview(
                validItems = decoded.items,
                newCount = decoded.items.size - existingIds.size,
                existingCount = existingIds.size,
                invalidCount = decoded.invalidCount,
                existingIds = existingIds,
            )
        }
}
