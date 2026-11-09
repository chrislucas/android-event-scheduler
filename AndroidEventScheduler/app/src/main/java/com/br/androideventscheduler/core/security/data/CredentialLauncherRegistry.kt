package com.br.androideventscheduler.core.security.data

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.br.androideventscheduler.core.security.domain.AuthResult
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Ponte entre a `MainActivity` e o [KeyguardIntentAuthenticator] (API 24–29). O `registerForActivityResult` precisa
 * ser chamado antes de a Activity chegar a `STARTED`, então a `MainActivity` chama [attach] em `onCreate`.
 * Solta a referência da Activity em `onDestroy` (sem vazamento).
 */
class CredentialLauncherRegistry {
    private var launcher: ActivityResultLauncher<Intent>? = null
    private var pending: CancellableContinuationHolder? = null

    fun attach(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val holder = pending
            pending = null
            holder?.resume(if (result.resultCode == Activity.RESULT_OK) AuthResult.Success else AuthResult.Canceled)
        }
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    launcher = null
                }
            },
        )
    }

    /** Abre a confirmação do sistema; sem Activity registrada, [AuthResult.NotAvailable]. */
    suspend fun launch(intent: Intent): AuthResult {
        val activeLauncher = launcher ?: return AuthResult.NotAvailable
        return suspendCancellableCoroutine { continuation ->
            pending = CancellableContinuationHolder { result -> if (continuation.isActive) continuation.resume(result, null) }
            continuation.invokeOnCancellation { pending = null }
            activeLauncher.launch(intent)
        }
    }

    private fun interface CancellableContinuationHolder {
        fun resume(result: AuthResult)
    }
}
