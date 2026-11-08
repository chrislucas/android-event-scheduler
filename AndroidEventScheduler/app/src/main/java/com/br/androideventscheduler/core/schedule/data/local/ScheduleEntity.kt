package com.br.androideventscheduler.core.schedule.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Linha da tabela `schedules`. Datas em epoch day, horas em minuto do dia, recorrência como
 * tipo + máscara de dias (bit `dayOfWeek.value - 1`, segunda = bit 0) + limite.
 * [searchText] guarda título, notas e local normalizados (sem acentos/caixa) para a busca.
 */
@Entity(
    tableName = "schedules",
    indices = [Index("startDateEpochDay"), Index("type")],
)
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int?,
    val isAllDay: Boolean,
    val location: String,
    val notes: String,
    val colorHex: Long,
    val calendarSourceId: String,
    val reminderMinutes: Int?,
    val recurrenceType: String,
    val recurrenceDaysMask: Int,
    val occurrenceLimit: Int?,
    val stickerEmoji: String?,
    val searchText: String,
)
