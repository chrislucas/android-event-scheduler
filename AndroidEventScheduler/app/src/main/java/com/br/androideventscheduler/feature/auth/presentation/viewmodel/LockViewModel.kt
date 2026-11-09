package com.br.androideventscheduler.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.security.data.AppLockController
import com.br.androideventscheduler.core.security.domain.AuthResult
import com.br.androideventscheduler.feature.auth.presentation.effect.LockUiEffect
import com.br.androideventscheduler.feature.auth.presentation.event.LockUiEvent
import com.br.androideventscheduler.feature.auth.presentation.state.LockUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal class LockViewModel(
    private val appLockController: AppLockController,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow<LockUiState>(LockUiState.Idle)
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<LockUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<LockUiEffect> = _uiEffect.receiveAsFlow()

    /** Impede reabrir a confirmação a cada recomposição/rotação; volta a `false` ao desbloquear. */
    private var autoLaunchDone = false

    fun onEvent(event: LockUiEvent) {
        when (event) {
            LockUiEvent.OnScreenShown -> {
                if (!autoLaunchDone) {
                    autoLaunchDone = true
                    launchAuthentication()
                }
            }

            LockUiEvent.OnUnlockClicked -> {
                launchAuthentication()
            }

            is LockUiEvent.OnAuthResult -> {
                onAuthResult(event.result)
            }
        }
    }

    private fun launchAuthentication() {
        if (_uiState.value == LockUiState.Authenticating) return
        _uiState.value = LockUiState.Authenticating
        viewModelScope.launch(dispatcher) { _uiEffect.send(LockUiEffect.LaunchAuthentication) }
    }

    private fun onAuthResult(result: AuthResult) {
        when (result) {
            AuthResult.Success -> {
                autoLaunchDone = false
                _uiState.value = LockUiState.Idle
                appLockController.unlock()
            }

            AuthResult.Canceled -> {
                _uiState.value = LockUiState.Idle
            }

            AuthResult.LockedOut -> {
                _uiState.value = LockUiState.Message(R.string.lock_locked_out)
            }

            AuthResult.NotAvailable -> {
                _uiState.value = LockUiState.Message(R.string.lock_not_available)
            }

            is AuthResult.Error -> {
                _uiState.value = LockUiState.Message(R.string.lock_error)
            }
        }
    }
}
