package com.br.androideventscheduler.feature.auth.presentation.event

import com.br.androideventscheduler.core.security.domain.AuthResult

internal sealed interface LockUiEvent {
    /** A tela apareceu: abre a confirmação do sistema **uma vez** (rotação não repete). */
    data object OnScreenShown : LockUiEvent

    data object OnUnlockClicked : LockUiEvent

    data class OnAuthResult(
        val result: AuthResult,
    ) : LockUiEvent
}
