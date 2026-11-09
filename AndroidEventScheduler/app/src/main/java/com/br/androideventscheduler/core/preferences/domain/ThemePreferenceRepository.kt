package com.br.androideventscheduler.core.preferences.domain

import kotlinx.coroutines.flow.Flow

/**
 * Escolha de tema (spec 19). Repositório próprio, e não campo de [UserSettings]: o `save()` das configurações regrava
 * todas as chaves e só roda no botão Salvar, enquanto o tema é gravado na hora, sem tocar nas edições pendentes.
 */
interface ThemePreferenceRepository {
    /** Emite [ThemeMode.DEFAULT] quando nada foi gravado, o valor é desconhecido ou a leitura falha. */
    fun observe(): Flow<ThemeMode>

    suspend fun set(mode: ThemeMode)
}
