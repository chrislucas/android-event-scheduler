package com.br.androideventscheduler.core.security.di

import com.br.androideventscheduler.core.security.data.BiometricPromptAuthenticator
import com.br.androideventscheduler.core.security.data.KeyguardIntentAuthenticator
import com.br.androideventscheduler.core.security.domain.DeviceCredentialAuthenticator
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** O único `if` de versão do app (R5.3): o `SecurityModule` escolhe o autenticador pela API do aparelho. */
@RunWith(RobolectricTestRunner::class)
class SecurityModuleRobolectricTest {
    private fun authenticator(): DeviceCredentialAuthenticator = GlobalContext.get().get()

    @Test
    @Config(sdk = [30])
    fun `given android 11 when resolving the authenticator then it uses the biometric prompt`() {
        assertTrue(authenticator() is BiometricPromptAuthenticator)
    }

    @Test
    @Config(sdk = [34])
    fun `given android 14 when resolving the authenticator then it uses the biometric prompt`() {
        assertTrue(authenticator() is BiometricPromptAuthenticator)
    }

    @Test
    @Config(sdk = [29])
    fun `given android 10 when resolving the authenticator then it uses the keyguard manager`() {
        assertTrue(authenticator() is KeyguardIntentAuthenticator)
    }

    @Test
    @Config(sdk = [24])
    fun `given android 7 when resolving the authenticator then it uses the keyguard manager`() {
        assertTrue(authenticator() is KeyguardIntentAuthenticator)
    }
}
