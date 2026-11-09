package com.br.androideventscheduler.core.security.data

import androidx.biometric.BiometricPrompt
import com.br.androideventscheduler.core.security.domain.AuthResult

/** Converte o código de erro do `BiometricPrompt` em [AuthResult]. Função pura, testável na JVM. */
internal fun biometricErrorToAuthResult(errorCode: Int): AuthResult =
    when (errorCode) {
        BiometricPrompt.ERROR_USER_CANCELED,
        BiometricPrompt.ERROR_CANCELED,
        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
        -> AuthResult.Canceled

        BiometricPrompt.ERROR_LOCKOUT,
        BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
        -> AuthResult.LockedOut

        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> AuthResult.NotAvailable

        else -> AuthResult.Error(errorCode)
    }
