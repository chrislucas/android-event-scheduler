package com.br.androideventscheduler.feature.auth.presentation.state

import androidx.annotation.StringRes

internal sealed class LockUiState {
    data object Idle : LockUiState()

    /** A confirmação do sistema está aberta. */
    data object Authenticating : LockUiState()

    /** Resultado que o usuário precisa ver (lockout, indisponível, erro); o botão "Desbloquear" continua ativo. */
    data class Message(
        @StringRes val messageRes: Int,
    ) : LockUiState()
}
