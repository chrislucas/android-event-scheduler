package com.br.androideventscheduler.feature.splash.presentation.event

internal sealed interface SplashUiEvent {
    /** A tela entrou na composição; inicia a contagem para seguir à Home. */
    data object OnSplashStarted : SplashUiEvent
}
