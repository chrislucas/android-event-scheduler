package com.br.androideventscheduler.core.security.data

import android.app.KeyguardManager
import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.br.androideventscheduler.core.security.domain.AuthResult
import com.br.androideventscheduler.core.security.domain.DeviceCredentialAuthenticator

/**
 * API 24–29: o `BiometricPrompt` não aceita `DEVICE_CREDENTIAL` isolado, então usa a tela de confirmação do sistema
 * (`KeyguardManager.createConfirmDeviceCredentialIntent`) lançada pelo [CredentialLauncherRegistry].
 */
internal class KeyguardIntentAuthenticator(
    private val context: Context,
    private val launcherRegistry: CredentialLauncherRegistry,
) : DeviceCredentialAuthenticator {
    // `createConfirmDeviceCredentialIntent` está obsoleto desde a API 29, mas é o caminho oficial para API 24–29: o
    // BiometricPrompt só aceita DEVICE_CREDENTIAL isolado a partir da API 30 (este autenticador nunca roda lá).
    @Suppress("DEPRECATION")
    override suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
    ): AuthResult {
        val keyguardManager = context.getSystemService(KeyguardManager::class.java)
        val intent = keyguardManager?.createConfirmDeviceCredentialIntent(title, subtitle) ?: return AuthResult.NotAvailable
        return launcherRegistry.launch(intent)
    }
}
