package com.br.androideventscheduler.feature.backup.domain.usecase

import com.br.androideventscheduler.core.alarm.domain.usecase.ScheduleNextAlarmUseCase
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.feature.backup.domain.model.ImportPreview
import com.br.androideventscheduler.feature.backup.domain.model.ImportResult
import com.br.androideventscheduler.feature.backup.domain.model.ImportStrategy
import java.util.UUID

/**
 * Grava os itens de uma [ImportPreview] já validada, conforme a [ImportStrategy], e agenda o alarme de cada um.
 * Orquestra banco + alarme: a gravação é **uma transação** (`upsertAll`: ou entram todos ou nenhum) e a falha de um
 * alarme **não** desfaz a importação; se a gravação falhar, nenhum alarme é agendado.
 * O sufixo das cópias vem da apresentação, pois o domínio não conhece resources.
 */
class ImportAgendaUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val scheduleNextAlarm: ScheduleNextAlarmUseCase,
    private val idProvider: () -> String = { UUID.randomUUID().toString() },
) {
    suspend operator fun invoke(
        preview: ImportPreview,
        strategy: ImportStrategy,
        copySuffix: String,
    ): Result<ImportResult> =
        runCatching {
            val selected = select(preview, strategy, copySuffix)
            scheduleRepository.upsertAll(selected)
            val alarmFailures = selected.count { item -> runCatching { scheduleNextAlarm(item) }.isFailure }
            ImportResult(
                importedCount = selected.size,
                skippedCount = preview.validItems.size - selected.size,
                alarmFailures = alarmFailures,
            )
        }

    private fun select(
        preview: ImportPreview,
        strategy: ImportStrategy,
        copySuffix: String,
    ): List<ScheduleItem> =
        when (strategy) {
            ImportStrategy.REPLACE_EXISTING -> {
                preview.validItems
            }

            ImportStrategy.KEEP_EXISTING -> {
                preview.validItems.filter { it.id !in preview.existingIds }
            }

            ImportStrategy.IMPORT_AS_COPIES -> {
                preview.validItems.map { item ->
                    if (item.id in preview.existingIds) {
                        item.copy(id = idProvider(), title = titleWithSuffix(item.title, copySuffix))
                    } else {
                        item
                    }
                }
            }
        }

    /** Mantém o título dentro do limite do formato mesmo com o sufixo, sem partir um emoji. */
    private fun titleWithSuffix(
        title: String,
        suffix: String,
    ): String {
        val room = (MAX_TITLE_LENGTH - suffix.length).coerceAtLeast(0)
        if (title.length <= room) return title + suffix
        val cut = if (room > 0 && title[room - 1].isHighSurrogate()) room - 1 else room
        return title.substring(0, cut) + suffix
    }

    private companion object {
        const val MAX_TITLE_LENGTH = 200
    }
}
