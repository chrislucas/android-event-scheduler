package com.br.androideventscheduler.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.preferences.domain.SettingsValidationError
import com.br.androideventscheduler.core.preferences.domain.ThemeMode
import com.br.androideventscheduler.core.preferences.domain.ThemePreferenceRepository
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
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
    private val themePreferenceRepository: ThemePreferenceRepository,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<SettingsUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<SettingsUiEffect> = _uiEffect.receiveAsFlow()

    init {
        // Rascunho: leitura única, reemissões do DataStore não devem sobrescrever o que o usuário está editando.
        // Tema: observado, porque grava na hora e a seleção na tela vem do que foi persistido. Só `themeMode` muda.
        viewModelScope.launch(dispatcher) {
            val initialDraft = userSettingsRepository.current()
            themePreferenceRepository.observe().collect { mode -> applyThemeMode(initialDraft, mode) }
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

            is SettingsUiEvent.AppearanceEvent -> {
                onAppearanceEvent(event)
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
        }
    }

    private fun applyThemeMode(
        initialDraft: UserSettings,
        mode: ThemeMode,
    ) {
        _uiState.update { state ->
            when (state) {
                SettingsUiState.Loading -> SettingsUiState.Content(draft = initialDraft, themeMode = mode)
                is SettingsUiState.Content -> state.copy(themeMode = mode)
            }
        }
    }

    private fun onAppearanceEvent(event: SettingsUiEvent.AppearanceEvent) {
        when (event) {
            is SettingsUiEvent.AppearanceEvent.OnThemeModeSelected -> setThemeMode(event.mode)
        }
    }

    /** Não toca no rascunho nem em `UserSettingsRepository.save()`: só a chave do tema é gravada. */
    private fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch(dispatcher) {
            runCatching { themePreferenceRepository.set(mode) }
                .onFailure { _uiEffect.send(SettingsUiEffect.ShowError(R.string.settings_theme_save_error)) }
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
