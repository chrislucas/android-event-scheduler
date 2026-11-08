package com.br.androideventscheduler.feature.detail.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.common.StringProvider
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleNotFoundException
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.feature.detail.domain.usecase.DeleteScheduleUseCase
import com.br.androideventscheduler.feature.detail.domain.usecase.DuplicateScheduleUseCase
import com.br.androideventscheduler.feature.detail.presentation.effect.ScheduleDetailUiEffect
import com.br.androideventscheduler.feature.detail.presentation.event.ScheduleDetailUiEvent
import com.br.androideventscheduler.feature.detail.presentation.mapper.ScheduleDetailMapper
import com.br.androideventscheduler.feature.detail.presentation.mapper.ScheduleShareTextBuilder
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ScheduleDetailViewModel(
    private val scheduleId: String,
    private val scheduleRepository: ScheduleRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val duplicateSchedule: DuplicateScheduleUseCase,
    private val deleteSchedule: DeleteScheduleUseCase,
    private val strings: StringProvider,
    private val savedStateHandle: SavedStateHandle,
    private val mapper: ScheduleDetailMapper,
    private val shareTextBuilder: ScheduleShareTextBuilder,
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
            ScheduleDetailUiEvent.OnDuplicateClicked -> executeDuplicate()
            ScheduleDetailUiEvent.OnDeleteClicked -> setDeleteDialogVisible(true)
            ScheduleDetailUiEvent.OnDismissDeleteDialog -> setDeleteDialogVisible(false)
            ScheduleDetailUiEvent.OnConfirmDeleteClicked -> executeDelete()
            ScheduleDetailUiEvent.OnShareClicked -> share()
            is ScheduleDetailUiEvent.OnOpenCopyClicked -> sendEffect(ScheduleDetailUiEffect.NavigateToCopy(event.copyId))
        }
    }

    /**
     * Marca a ação como em andamento **de forma síncrona** (antes de lançar a corrotina) e devolve `false`
     * se já havia outra em curso ou a tela não está carregada: é o que ignora toques repetidos.
     */
    private fun tryStartAction(): Boolean {
        val content = _uiState.value as? ScheduleDetailUiState.Content
        if (content == null || content.isProcessingAction) return false
        _uiState.value = content.copy(isProcessingAction = true)
        return true
    }

    private fun finishAction() {
        updateContent { it.copy(isProcessingAction = false) }
    }

    private fun executeDuplicate() {
        if (!tryStartAction()) return
        viewModelScope.launch(dispatcher) {
            val titleSuffix = strings.getString(R.string.detail_copy_title_suffix)
            duplicateSchedule(scheduleId, titleSuffix)
                .onSuccess { copy -> _uiEffect.send(ScheduleDetailUiEffect.ShowCopyCreated(copy.id)) }
                .onFailure { error ->
                    val messageRes =
                        if (error is ScheduleNotFoundException) R.string.detail_not_found else R.string.detail_duplicate_error
                    _uiEffect.send(ScheduleDetailUiEffect.ShowMessage(messageRes))
                }
            finishAction()
        }
    }

    /**
     * Fecha o diálogo e exclui. Em sucesso **não** emite `NavigateBack`: a observação do item (`observeById`)
     * emite `null` e `observeSchedule` volta uma única vez (fonte única). A ação segue "em andamento" até a
     * tela fechar, o que impede qualquer toque residual.
     */
    private fun executeDelete() {
        val isDialogOpen = (_uiState.value as? ScheduleDetailUiState.Content)?.isDeleteDialogVisible == true
        if (!isDialogOpen || !tryStartAction()) return
        setDeleteDialogVisible(false)
        viewModelScope.launch(dispatcher) {
            deleteSchedule(scheduleId).onFailure {
                _uiEffect.send(ScheduleDetailUiEffect.ShowMessage(R.string.detail_delete_error))
                finishAction()
            }
        }
    }

    /** Só monta o texto e emite o efeito (sem I/O); lançar o Intent e tratar a falha é da tela. */
    private fun share() {
        val content = _uiState.value as? ScheduleDetailUiState.Content ?: return
        val shareContent = shareTextBuilder.build(content.schedule)
        sendEffect(ScheduleDetailUiEffect.ShareText(subject = shareContent.subject, body = shareContent.body))
    }

    private fun setDeleteDialogVisible(isVisible: Boolean) {
        val content = _uiState.value as? ScheduleDetailUiState.Content ?: return
        if (isVisible && content.isProcessingAction) return
        savedStateHandle[KEY_DELETE_DIALOG_VISIBLE] = isVisible
        _uiState.value = content.copy(isDeleteDialogVisible = isVisible)
    }

    private inline fun updateContent(transform: (ScheduleDetailUiState.Content) -> ScheduleDetailUiState.Content) {
        _uiState.update { state -> if (state is ScheduleDetailUiState.Content) transform(state) else state }
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
                        val previous = _uiState.value as? ScheduleDetailUiState.Content
                        _uiState.value =
                            ScheduleDetailUiState.Content(
                                schedule = mapper.map(item, email),
                                isDeleteDialogVisible = savedStateHandle[KEY_DELETE_DIALOG_VISIBLE] ?: false,
                                isProcessingAction = previous?.isProcessingAction ?: false,
                            )
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

    private companion object {
        const val KEY_DELETE_DIALOG_VISIBLE = "detail_delete_dialog_visible"
    }
}
