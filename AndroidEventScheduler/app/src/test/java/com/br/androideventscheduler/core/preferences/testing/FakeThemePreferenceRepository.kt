package com.br.androideventscheduler.core.preferences.testing

import com.br.androideventscheduler.core.preferences.domain.ThemeMode
import com.br.androideventscheduler.core.preferences.domain.ThemePreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Repositório de tema em memória. */
class FakeThemePreferenceRepository(
    initial: ThemeMode = ThemeMode.DEFAULT,
) : ThemePreferenceRepository {
    val mode = MutableStateFlow(initial)
    val setCalls = mutableListOf<ThemeMode>()

    override fun observe(): Flow<ThemeMode> = mode

    override suspend fun set(mode: ThemeMode) {
        setCalls += mode
        this.mode.value = mode
    }
}
