package com.br.androideventscheduler.feature.auth.presentation.effect

internal sealed interface LockUiEffect {
    /** A tela chama o autenticador com a Activity e devolve o resultado em `OnAuthResult` (a Activity nunca vai ao ViewModel). */
    data object LaunchAuthentication : LockUiEffect
}
