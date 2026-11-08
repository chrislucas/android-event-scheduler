package com.br.androideventscheduler.core.preferences.testing

import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

/** Repositório em memória: permite simular reemissões externas e falhas de gravação. */
class FakeUserSettingsRepository(
    initial: UserSettings = UserSettings(),
) : UserSettingsRepository {
    val settings = MutableStateFlow(initial)
    var failOnSave: Boolean = false
    var saveCalls: Int = 0
        private set

    override fun observe(): Flow<UserSettings> = settings

    override suspend fun save(settings: UserSettings) {
        saveCalls++
        if (failOnSave) throw IOException("disk full")
        this.settings.value = settings
    }
}
