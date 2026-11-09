package com.br.androideventscheduler.core.security.domain

/**
 * Estado do bloqueio do app, **só em memória** (nunca persistido: matar o processo sempre volta bloqueado).
 * [Pending] = preferências ainda não lidas do DataStore; o gate mostra um fundo neutro, sem dados da agenda.
 */
enum class AppLockState {
    Pending,
    Locked,
    Unlocked,
}
