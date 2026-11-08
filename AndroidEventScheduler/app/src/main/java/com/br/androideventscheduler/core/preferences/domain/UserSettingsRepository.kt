package com.br.androideventscheduler.core.preferences.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface UserSettingsRepository {
    /** Preferências atuais; padrões quando nada foi salvo ou o armazenamento está ilegível. */
    fun observe(): Flow<UserSettings>

    /** Primeira leitura (útil para carregar um formulário uma única vez). */
    suspend fun current(): UserSettings = observe().first()

    suspend fun save(settings: UserSettings)
}
