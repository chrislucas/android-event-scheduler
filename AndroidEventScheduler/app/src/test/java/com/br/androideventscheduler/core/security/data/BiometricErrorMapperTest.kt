package com.br.androideventscheduler.core.security.data

import androidx.biometric.BiometricPrompt
import com.br.androideventscheduler.core.security.domain.AuthResult
import org.junit.Assert.assertEquals
import org.junit.Test

class BiometricErrorMapperTest {
    @Test
    fun `given user canceled or negative button errors when mapping then result is canceled`() {
        listOf(
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
        ).forEach { code -> assertEquals("código $code", AuthResult.Canceled, biometricErrorToAuthResult(code)) }
    }

    @Test
    fun `given lockout errors when mapping then result is locked out`() {
        listOf(BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT)
            .forEach { code -> assertEquals("código $code", AuthResult.LockedOut, biometricErrorToAuthResult(code)) }
    }

    @Test
    fun `given no device credential error when mapping then result is not available`() {
        assertEquals(AuthResult.NotAvailable, biometricErrorToAuthResult(BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL))
    }

    @Test
    fun `given an unknown error code when mapping then result is error with the same code`() {
        assertEquals(AuthResult.Error(BiometricPrompt.ERROR_VENDOR), biometricErrorToAuthResult(BiometricPrompt.ERROR_VENDOR))
    }
}
