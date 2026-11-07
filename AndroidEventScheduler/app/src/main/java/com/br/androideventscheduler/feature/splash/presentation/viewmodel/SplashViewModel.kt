package com.br.androideventscheduler.feature.splash.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.br.androideventscheduler.feature.splash.presentation.effect.SplashUiEffect
import com.br.androideventscheduler.feature.splash.presentation.event.SplashUiEvent
import com.br.androideventscheduler.feature.splash.presentation.state.SplashUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal class SplashViewModel(
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Showing)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    // Channel BUFFERED: a navegação é entregue exatamente uma vez, mesmo se a tela estiver girando.
    private val _uiEffect = Channel<SplashUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<SplashUiEffect> = _uiEffect.receiveAsFlow()

    // O ViewModel sobrevive à rotação; esta flag evita reiniciar a contagem ao recompor.
    private var hasStarted = false

    fun onEvent(event: SplashUiEvent) {
        when (event) {
            SplashUiEvent.OnSplashStarted -> startOnce()
        }
    }

    private fun startOnce() {
        if (hasStarted) return
        hasStarted = true
        viewModelScope.launch(dispatcher) {
            delay(SPLASH_DELAY_MILLIS)
            _uiEffect.send(SplashUiEffect.NavigateToHome)
        }
    }

    companion object {
        const val SPLASH_DELAY_MILLIS = 1_500L
    }
}
