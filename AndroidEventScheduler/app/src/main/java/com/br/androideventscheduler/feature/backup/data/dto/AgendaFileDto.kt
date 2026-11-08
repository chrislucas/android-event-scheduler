package com.br.androideventscheduler.feature.backup.data.dto

import kotlinx.serialization.Serializable

/**
 * Cabeçalho + itens do arquivo `rememberme-agenda` (formato documentado em `docs/formato-agenda-json.md`).
 * Todos os campos são texto/números simples: o arquivo é entrada não confiável e só vira domínio depois de validado
 * pelo `AgendaDtoMapper`. Enums viajam pelo **nome estável**, nunca pelo ordinal.
 */
@Serializable
internal data class AgendaFileDto(
    val format: String,
    val version: Int,
    val exportedAt: String,
    val appVersion: String,
    val items: List<ScheduleItemDto>,
)

/**
 * Um agendamento. Obrigatórios na importação: `id`, `title`, `type`, `startDate` e `startTime`; os demais têm
 * padrão quando ausentes (a exportação sempre escreve todos).
 */
@Serializable
internal data class ScheduleItemDto(
    val id: String,
    val title: String,
    val type: String,
    val startDate: String,
    val endDate: String? = null,
    val startTime: String,
    val endTime: String? = null,
    val isAllDay: Boolean = false,
    val location: String = "",
    val notes: String = "",
    val colorHex: String? = null,
    val calendarSource: String? = null,
    val reminderOffset: String? = null,
    val recurrence: RecurrenceDto = RecurrenceDto(),
    val stickerEmoji: String? = null,
)

@Serializable
internal data class RecurrenceDto(
    val type: String = RECURRENCE_NONE,
    val daysOfWeek: List<String> = emptyList(),
    val occurrenceLimit: Int? = null,
) {
    companion object {
        const val RECURRENCE_NONE = "NONE"
        const val RECURRENCE_DAILY = "DAILY"
        const val RECURRENCE_WEEKLY = "WEEKLY"
    }
}
