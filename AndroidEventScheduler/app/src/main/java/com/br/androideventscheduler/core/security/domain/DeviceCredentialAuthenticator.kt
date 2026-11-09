package com.br.androideventscheduler.core.security.domain

import androidx.fragment.app.FragmentActivity

/** Resultado da confirmação da credencial do dispositivo (PIN, padrão ou senha). */
sealed interface AuthResult {
    data object Success : AuthResult

    /** O usuário fechou o diálogo do sistema. */
    data object Canceled : AuthResult

    /** Tentativas demais: o sistema bloqueou temporariamente (ou de forma permanente). */
    data object LockedOut : AuthResult

    /** O aparelho não tem credencial configurada (ou o sistema não consegue pedi-la). */
    data object NotAvailable : AuthResult

    data class Error(
        val code: Int,
    ) : AuthResult
}

/** Delega ao sistema operacional a confirmação da credencial; o app não cria nem guarda PIN próprio. */
interface DeviceCredentialAuthenticator {
    /** Mostra a confirmação de credencial do sistema. Chamado a partir da Activity em primeiro plano. */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
    ): AuthResult
}
