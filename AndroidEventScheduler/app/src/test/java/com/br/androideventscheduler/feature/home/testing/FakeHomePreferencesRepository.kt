package com.br.androideventscheduler.feature.home.testing

import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.feature.home.domain.repository.HomePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Repositório em memória das preferências da Home: [enabled] permite simular reemissões externas. */
class FakeHomePreferencesRepository(
    initial: Set<CalendarSource> = CalendarSource.entries.toSet(),
) : HomePreferencesRepository {
    val enabled = MutableStateFlow(initial)
    val toggles = mutableListOf<Pair<CalendarSource, Boolean>>()

    /** Adesivos por `epochDay`; ausente = sem adesivo. */
    val daySticker = MutableStateFlow<Map<Long, String>>(emptyMap())
    val dayStickerWrites = mutableListOf<Pair<Long, String?>>()

    override fun observeEnabledCalendars(): Flow<Set<CalendarSource>> = enabled

    override suspend fun setCalendarEnabled(
        source: CalendarSource,
        enabled: Boolean,
    ) {
        toggles += source to enabled
        this.enabled.value = if (enabled) this.enabled.value + source else this.enabled.value - source
    }

    override fun observeDaySticker(epochDay: Long): Flow<String?> = daySticker.map { it[epochDay] }

    override suspend fun setDaySticker(
        epochDay: Long,
        emoji: String?,
    ) {
        dayStickerWrites += epochDay to emoji
        daySticker.value = if (emoji.isNullOrBlank()) daySticker.value - epochDay else daySticker.value + (epochDay to emoji)
    }
}
