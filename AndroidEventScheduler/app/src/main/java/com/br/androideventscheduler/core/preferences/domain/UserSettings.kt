package com.br.androideventscheduler.core.preferences.domain

import com.br.androideventscheduler.core.security.domain.AppLockTimeout
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset

/** Preferências do usuário, compartilhadas por Configurações, criação de itens, alarmes e Home. */
data class UserSettings(
    val activeAccountEmail: String = DEFAULT_ACCOUNT_EMAIL,
    val defaultReminderOffset: ReminderOffset = ReminderOffset.DEFAULT,
    val defaultSnoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    val fullScreenEventAlertEnabled: Boolean = true,
    // Bloqueio do app (spec 16)
    val appLockEnabled: Boolean = false,
    val appLockTimeout: AppLockTimeout = AppLockTimeout.DEFAULT,
) {
    /** Valida e normaliza (e-mail com `trim`). Falha com [SettingsValidationError]. */
    fun validate(): Result<UserSettings> {
        val trimmedEmail = activeAccountEmail.trim()
        return when {
            !EMAIL_PATTERN.matches(trimmedEmail) -> Result.failure(SettingsValidationError.InvalidEmail)
            defaultSnoozeMinutes !in ALLOWED_SNOOZE_MINUTES -> Result.failure(SettingsValidationError.InvalidSnooze)
            else -> Result.success(copy(activeAccountEmail = trimmedEmail))
        }
    }

    companion object {
        const val DEFAULT_ACCOUNT_EMAIL = "christoffer.luccas@gmail.com"
        const val DEFAULT_SNOOZE_MINUTES = 10
        val ALLOWED_SNOOZE_MINUTES = listOf(5, 10, 15)

        /** `local@dominio.tld`, sem espaços — simples de propósito (domínio puro, sem `android.util.Patterns`). */
        private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}

sealed class SettingsValidationError : Exception() {
    data object InvalidEmail : SettingsValidationError() {
        private fun readResolve(): Any = InvalidEmail
    }

    data object InvalidSnooze : SettingsValidationError() {
        private fun readResolve(): Any = InvalidSnooze
    }
}
