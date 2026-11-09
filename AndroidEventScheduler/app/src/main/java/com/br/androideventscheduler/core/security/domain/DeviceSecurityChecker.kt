package com.br.androideventscheduler.core.security.domain

/** Informa se o aparelho tem bloqueio de tela seguro (PIN, padrão ou senha) configurado. */
fun interface DeviceSecurityChecker {
    fun isDeviceSecure(): Boolean
}
