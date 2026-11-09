package com.br.androideventscheduler.feature.home.domain.repository

import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import kotlinx.coroutines.flow.Flow

/** Preferências próprias da Home (filtros da gaveta lateral e adesivo de cada dia). */
interface HomePreferencesRepository {
    /** Calendários visíveis na Home; todos, enquanto o usuário não mudar nada. */
    fun observeEnabledCalendars(): Flow<Set<CalendarSource>>

    /** Liga/desliga [source]; lê o conjunto atual dentro da transação, então toques rápidos não se perdem. */
    suspend fun setCalendarEnabled(
        source: CalendarSource,
        enabled: Boolean,
    )

    /** Adesivo escolhido para o dia [epochDay] (`LocalDate.toEpochDay()`); `null` quando não há. */
    fun observeDaySticker(epochDay: Long): Flow<String?>

    /** Grava o adesivo do dia; `null` ou em branco remove. Cada dia é independente dos demais. */
    suspend fun setDaySticker(
        epochDay: Long,
        emoji: String?,
    )
}
