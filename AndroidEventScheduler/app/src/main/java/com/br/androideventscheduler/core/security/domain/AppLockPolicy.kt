package com.br.androideventscheduler.core.security.domain

/** Regra pura de quando o app deve bloquear ao voltar do segundo plano (relógio monotônico, em milissegundos). */
object AppLockPolicy {
    fun shouldLock(
        enabled: Boolean,
        backgroundedAtMillis: Long?,
        nowMillis: Long,
        timeout: AppLockTimeout,
    ): Boolean = enabled && backgroundedAtMillis != null && nowMillis - backgroundedAtMillis >= timeout.durationMillis
}
