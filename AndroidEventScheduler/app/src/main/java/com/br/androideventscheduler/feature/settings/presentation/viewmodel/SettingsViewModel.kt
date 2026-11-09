package com.br.androideventscheduler.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.preferences.domain.SettingsValidationError
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import com.br.androideventscheduler.core.security.data.AppLockController
import com.br.androideventscheduler.core.security.domain.AuthResult
import com.br.androideventscheduler.core.security.domain.DeviceSecurityChecker
import com.br.androideventscheduler.feature.settings.presentation.effect.SettingsUiEffect
import com.br.androideventscheduler.feature.settings.presentation.event.SettingsUiEvent
import com.br.androideventscheduler.feature.settings.presentation.state.SettingsUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class SettingsViewModel(
    private val userSettingsRepository: UserSettingsRepository,
    private val appLockController: AppLockController,
    private val deviceSecurity: DeviceSecurityChecker,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<SettingsUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<SettingsUiEffect> = _uiEffect.receiveAsFlow()

    /** Valor que a confirmação de credencial em andamento vai gravar (ligar ou desligar). */
    private var pendingAppLockEnabled: Boolean? = null

    init {
        // Leitura única: reemissões do DataStore não devem sobrescrever o que o usuário está editando.
        viewModelScope.launch(dispatcher) {
            _uiState.value =
                SettingsUiState.Content(
                    draft = userSettingsRepository.current(),
                    isDeviceSecure = deviceSecurity.isDeviceSecure(),
                )
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.OnEmailChanged -> {
                updateContent { copy(draft = draft.copy(activeAccountEmail = event.email), emailError = false) }
            }

            is SettingsUiEvent.OnReminderSelected -> {
                updateContent { copy(draft = draft.copy(defaultReminderOffset = event.offset)) }
            }

            is SettingsUiEvent.OnSnoozeSelected -> {
                updateContent { copy(draft = draft.copy(defaultSnoozeMinutes = event.minutes)) }
            }

            is SettingsUiEvent.OnFullScreenToggled -> {
                updateContent { copy(draft = draft.copy(fullScreenEventAlertEnabled = event.enabled)) }
            }

            SettingsUiEvent.OnSaveClicked -> {
                save()
            }

            SettingsUiEvent.OnBackClicked -> {
                sendEffect(SettingsUiEffect.NavigateBack)
            }

            SettingsUiEvent.OnBackupClicked -> {
                sendEffect(SettingsUiEffect.NavigateToBackup)
            }

            // Segurança (spec 16)
            is SettingsUiEvent.AppLockEvent -> {
                onAppLockEvent(event)
            }
        }
    }

    private fun onAppLockEvent(event: SettingsUiEvent.AppLockEvent) {
        when (event) {
            is SettingsUiEvent.AppLockEvent.OnToggled -> requestAppLockChange(event.enabled)
            is SettingsUiEvent.AppLockEvent.OnTimeoutSelected -> persistAppLock { copy(appLockTimeout = event.timeout) }
            is SettingsUiEvent.AppLockEvent.OnAuthResult -> onAppLockAuthResult(event.result)
            SettingsUiEvent.AppLockEvent.OnOpenSecuritySettingsClicked -> sendEffect(SettingsUiEffect.AppLockEffect.OpenSecuritySettings)
            SettingsUiEvent.AppLockEvent.OnScreenResumed -> updateContent { copy(isDeviceSecure = deviceSecurity.isDeviceSecure()) }
        }
    }

    /** Ligar e desligar pedem a credencial **antes** de gravar (R1.2/R1.3); aparelho sem bloqueio seguro não pode. */
    private fun requestAppLockChange(enabled: Boolean) {
        val content = _uiState.value as? SettingsUiState.Content ?: return
        if (!content.isDeviceSecure) return
        pendingAppLockEnabled = enabled
        // A confirmação (Activity própria em API 24–29) leva o app a segundo plano: não pode bloquear na volta.
        appLockController.suppressNextLock()
        sendEffect(SettingsUiEffect.AppLockEffect.RequestAuthentication)
    }

    private fun onAppLockAuthResult(result: AuthResult) {
        val requested = pendingAppLockEnabled
        pendingAppLockEnabled = null
        when (result) {
            AuthResult.Success -> {
                if (requested != null) persistAppLock { copy(appLockEnabled = requested) }
            }

            AuthResult.Canceled -> {
                Unit
            }

            AuthResult.LockedOut -> {
                sendEffect(SettingsUiEffect.ShowError(R.string.lock_locked_out))
            }

            AuthResult.NotAvailable -> {
                sendEffect(SettingsUiEffect.ShowError(R.string.lock_not_available))
            }

            is AuthResult.Error -> {
                sendEffect(SettingsUiEffect.ShowError(R.string.lock_error))
            }
        }
    }

    /**
     * Grava só os campos de segurança, sobre o que já está salvo (o resto do rascunho segue pendente do botão Salvar), e
     * espelha o resultado no rascunho. Ligar desbloqueia: o usuário acabou de confirmar a credencial.
     */
    private fun persistAppLock(transform: UserSettings.() -> UserSettings) {
        viewModelScope.launch(dispatcher) {
            runCatching {
                val updated = userSettingsRepository.current().transform()
                userSettingsRepository.save(updated)
                updated
            }.onSuccess { updated ->
                updateContent { copy(draft = draft.copy(appLockEnabled = updated.appLockEnabled, appLockTimeout = updated.appLockTimeout)) }
                if (updated.appLockEnabled) appLockController.unlock()
            }.onFailure {
                _uiEffect.send(SettingsUiEffect.ShowError(R.string.settings_save_error))
            }
        }
    }

    private fun save() {
        val content = _uiState.value as? SettingsUiState.Content ?: return
        if (content.isSaving) return
        content.draft
            .validate()
            .onSuccess { validSettings -> persist(validSettings) }
            .onFailure { validationError -> showValidationError(validationError) }
    }

    private fun persist(validSettings: UserSettings) {
        updateContent { copy(isSaving = true) }
        viewModelScope.launch(dispatcher) {
            runCatching { userSettingsRepository.save(validSettings) }
                .onSuccess {
                    updateContent { copy(draft = validSettings, isSaving = false) }
                    _uiEffect.send(SettingsUiEffect.ShowSavedConfirmation)
                    _uiEffect.send(SettingsUiEffect.NavigateBack)
                }.onFailure {
                    updateContent { copy(isSaving = false) }
                    _uiEffect.send(SettingsUiEffect.ShowError(R.string.settings_save_error))
                }
        }
    }

    private fun showValidationError(validationError: Throwable) {
        when (validationError) {
            SettingsValidationError.InvalidEmail -> updateContent { copy(emailError = true) }
            else -> sendEffect(SettingsUiEffect.ShowError(R.string.settings_invalid_snooze))
        }
    }

    /** Edita só a variante [SettingsUiState.Content]; ignora eventos que cheguem durante o carregamento. */
    private inline fun updateContent(crossinline transform: SettingsUiState.Content.() -> SettingsUiState.Content) {
        _uiState.update { state -> if (state is SettingsUiState.Content) state.transform() else state }
    }

    private fun sendEffect(effect: SettingsUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
