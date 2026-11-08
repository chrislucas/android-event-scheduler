package com.br.androideventscheduler.feature.home.testing

import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.feature.home.domain.repository.HomePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Repositório em memória das preferências da Home: [enabled] permite simular reemissões externas. */
class FakeHomePreferencesRepository(
    initial: Set<CalendarSource> = CalendarSource.entries.toSet(),
) : HomePreferencesRepository {
    val enabled = MutableStateFlow(initial)
    val toggles = mutableListOf<Pair<CalendarSource, Boolean>>()

    override fun observeEnabledCalendars(): Flow<Set<CalendarSource>> = enabled

    override suspend fun setCalendarEnabled(
        source: CalendarSource,
        enabled: Boolean,
    ) {
        toggles += source to enabled
        this.enabled.value = if (enabled) this.enabled.value + source else this.enabled.value - source
    }
}
