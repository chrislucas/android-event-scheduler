package com.br.androideventscheduler.feature.splash.presentation.effect

internal sealed interface SplashUiEffect {
    data object NavigateToHome : SplashUiEffect
}
