package com.br.androideventscheduler.feature.alarm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.alarm.domain.usecase.SnoozeAlarmUseCase
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleNotFoundException
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.feature.alarm.presentation.effect.FullScreenAlarmUiEffect
import com.br.androideventscheduler.feature.alarm.presentation.event.FullScreenAlarmUiEvent
import com.br.androideventscheduler.feature.alarm.presentation.mapper.FullScreenAlarmMapper
import com.br.androideventscheduler.feature.alarm.presentation.state.FullScreenAlarmUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class FullScreenAlarmViewModel(
    private val scheduleId: String,
    private val scheduleRepository: ScheduleRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val snoozeAlarmUseCase: SnoozeAlarmUseCase,
    private val mapper: FullScreenAlarmMapper,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow<FullScreenAlarmUiState>(FullScreenAlarmUiState.Loading)
    val uiState: StateFlow<FullScreenAlarmUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<FullScreenAlarmUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<FullScreenAlarmUiEffect> = _uiEffect.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        loadSchedule()
    }

    fun onEvent(event: FullScreenAlarmUiEvent) {
        when (event) {
            is FullScreenAlarmUiEvent.OnSnoozeDurationSelected -> selectSnoozeDuration(event.minutes)
            FullScreenAlarmUiEvent.OnSnoozeClicked -> snooze()
            FullScreenAlarmUiEvent.OnDismissClicked -> closeUnlessProcessing(cancelNotification = true)
            FullScreenAlarmUiEvent.OnBackPressed -> closeUnlessProcessing(cancelNotification = false)
            FullScreenAlarmUiEvent.OnReloadRequested -> loadSchedule()
        }
    }

    /** Item inexistente fecha a tela sem erro (cancelando a notificação órfã); falha de leitura fecha sem cancelar. */
    private fun loadSchedule() {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch(dispatcher) {
                val item = runCatching { scheduleRepository.getById(scheduleId) }
                if (item.isFailure) {
                    sendEffect(FullScreenAlarmUiEffect.CloseScreen(cancelNotification = false))
                    return@launch
                }
                val schedule = item.getOrNull()
                if (schedule == null) {
                    sendEffect(FullScreenAlarmUiEffect.CloseScreen(cancelNotification = true))
                    return@launch
                }
                val settings = runCatching { userSettingsRepository.current() }.getOrNull()
                val content = mapper.map(schedule, settings?.defaultSnoozeMinutes ?: UserSettings.DEFAULT_SNOOZE_MINUTES)
                // Spec 16: a tela aparece sobre o bloqueio do sistema, sem autenticação. Com a proteção do app ligada (ou sem
                // conseguir saber), só título e horário: local e notas ficam ocultos.
                _uiState.value = if (settings?.appLockEnabled == false) content else content.copy(location = null, notes = null)
            }
    }

    private fun selectSnoozeDuration(minutes: Int) {
        updateContent { content ->
            if (content.isProcessing || minutes !in content.snoozeOptions) {
                content
            } else {
                content.copy(selectedSnoozeMinutes = minutes, errorMessageRes = null)
            }
        }
    }

    private fun snooze() {
        val content = _uiState.value as? FullScreenAlarmUiState.Content ?: return
        if (content.isProcessing) return
        // O estado muda antes de lançar a coroutine: um segundo toque já encontra isProcessing = true.
        _uiState.value = content.copy(isProcessing = true, errorMessageRes = null)
        viewModelScope.launch(dispatcher) {
            val result = snoozeAlarmUseCase(scheduleId, content.selectedSnoozeMinutes)
            val failure = result.exceptionOrNull()
            when {
                failure == null || failure is ScheduleNotFoundException -> {
                    sendEffect(FullScreenAlarmUiEffect.CloseScreen(cancelNotification = true))
                }

                failure is CancellationException -> {
                    throw failure
                }

                else -> {
                    updateContent { it.copy(isProcessing = false, errorMessageRes = R.string.alarm_snooze_error) }
                }
            }
        }
    }

    private fun closeUnlessProcessing(cancelNotification: Boolean) {
        val content = _uiState.value as? FullScreenAlarmUiState.Content
        if (content?.isProcessing == true) return
        sendEffect(FullScreenAlarmUiEffect.CloseScreen(cancelNotification))
    }

    private inline fun updateContent(crossinline transform: (FullScreenAlarmUiState.Content) -> FullScreenAlarmUiState.Content) {
        _uiState.update { state -> if (state is FullScreenAlarmUiState.Content) transform(state) else state }
    }

    private fun sendEffect(effect: FullScreenAlarmUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
