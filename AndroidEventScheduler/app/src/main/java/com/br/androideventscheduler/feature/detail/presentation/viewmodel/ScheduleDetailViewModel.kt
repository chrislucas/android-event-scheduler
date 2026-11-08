package com.br.androideventscheduler.feature.detail.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.feature.detail.presentation.effect.ScheduleDetailUiEffect
import com.br.androideventscheduler.feature.detail.presentation.event.ScheduleDetailUiEvent
import com.br.androideventscheduler.feature.detail.presentation.mapper.ScheduleDetailMapper
import com.br.androideventscheduler.feature.detail.presentation.state.ScheduleDetailUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch

internal class ScheduleDetailViewModel(
    private val scheduleId: String,
    private val scheduleRepository: ScheduleRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val mapper: ScheduleDetailMapper,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ScheduleDetailUiState>(ScheduleDetailUiState.Loading)
    val uiState: StateFlow<ScheduleDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<ScheduleDetailUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<ScheduleDetailUiEffect> = _uiEffect.receiveAsFlow()

    init {
        observeSchedule()
    }

    fun onEvent(event: ScheduleDetailUiEvent) {
        when (event) {
            ScheduleDetailUiEvent.OnBackClicked -> sendEffect(ScheduleDetailUiEffect.NavigateBack)
            ScheduleDetailUiEvent.OnEditClicked -> sendEffect(ScheduleDetailUiEffect.NavigateToEdit(scheduleId))
        }
    }

    /**
     * Observa o item e o e-mail da conta. Quando o item some, a coleta termina (um `null` é o último valor):
     * nunca carregado ⇒ "não encontrado" + mensagem; já carregado (excluído em outra tela) ⇒ só volta.
     */
    private fun observeSchedule() {
        viewModelScope.launch(dispatcher) {
            var hasLoadedOnce = false
            val accountEmail = userSettingsRepository.observe().map { it.activeAccountEmail }.distinctUntilChanged()
            combine(scheduleRepository.observeById(scheduleId), accountEmail) { item, email -> item to email }
                .transformWhile { itemAndEmail ->
                    emit(itemAndEmail)
                    itemAndEmail.first != null
                }.collect { (item, email) ->
                    if (item != null) {
                        hasLoadedOnce = true
                        _uiState.value = ScheduleDetailUiState.Content(mapper.map(item, email))
                    } else {
                        if (!hasLoadedOnce) {
                            _uiState.value = ScheduleDetailUiState.NotFound
                            _uiEffect.send(ScheduleDetailUiEffect.ShowMessage(R.string.detail_not_found))
                        }
                        _uiEffect.send(ScheduleDetailUiEffect.NavigateBack)
                    }
                }
        }
    }

    private fun sendEffect(effect: ScheduleDetailUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
