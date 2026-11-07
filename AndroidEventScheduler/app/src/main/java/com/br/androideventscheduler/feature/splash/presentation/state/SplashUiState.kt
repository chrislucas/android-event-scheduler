package com.br.androideventscheduler.feature.splash.presentation.state

internal sealed class SplashUiState {
    /** A marca é exibida enquanto o tempo de abertura corre. */
    data object Showing : SplashUiState()
}
