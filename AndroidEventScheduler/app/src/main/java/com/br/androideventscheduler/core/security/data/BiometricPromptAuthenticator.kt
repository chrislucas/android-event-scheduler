package com.br.androideventscheduler.core.security.data

import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.br.androideventscheduler.core.security.domain.AuthResult
import com.br.androideventscheduler.core.security.domain.DeviceCredentialAuthenticator
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * API 30+: `BiometricPrompt` aceitando só a credencial do aparelho (`DEVICE_CREDENTIAL`). Com `DEVICE_CREDENTIAL` o
 * botão negativo é proibido (o sistema oferece o próprio "cancelar"), por isso não há `setNegativeButtonText`.
 * Deve ser chamado na thread principal, com a Activity em primeiro plano.
 */
internal class BiometricPromptAuthenticator : DeviceCredentialAuthenticator {
    override suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
    ): AuthResult =
        suspendCancellableCoroutine { continuation ->
            val callback =
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) continuation.resume(AuthResult.Success, onCancellation = null)
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        if (continuation.isActive) continuation.resume(biometricErrorToAuthResult(errorCode), onCancellation = null)
                    }
                }
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
            val info =
                BiometricPrompt.PromptInfo
                    .Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(DEVICE_CREDENTIAL)
                    .build()
            prompt.authenticate(info)
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        }
}
