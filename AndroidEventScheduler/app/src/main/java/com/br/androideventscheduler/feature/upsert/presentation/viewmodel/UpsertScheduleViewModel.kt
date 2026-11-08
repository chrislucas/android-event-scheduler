package com.br.androideventscheduler.feature.upsert.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.feature.upsert.domain.ScheduleDraftTimeRules
import com.br.androideventscheduler.feature.upsert.domain.ScheduleDraftTimes
import com.br.androideventscheduler.feature.upsert.domain.model.SaveScheduleError
import com.br.androideventscheduler.feature.upsert.domain.usecase.SaveScheduleUseCase
import com.br.androideventscheduler.feature.upsert.presentation.effect.UpsertScheduleUiEffect
import com.br.androideventscheduler.feature.upsert.presentation.event.QuickTime
import com.br.androideventscheduler.feature.upsert.presentation.event.UpsertScheduleUiEvent
import com.br.androideventscheduler.feature.upsert.presentation.state.ActiveInlinePicker
import com.br.androideventscheduler.feature.upsert.presentation.state.UpsertScheduleUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/**
 * Criação (`scheduleId == null`) e edição de um agendamento. Datas e horas seguem [ScheduleDraftTimeRules];
 * a gravação é do [SaveScheduleUseCase]. [idProvider] é injetável para testes determinísticos.
 */
@Suppress("LongParameterList")
internal class UpsertScheduleViewModel(
    private val scheduleId: String?,
    private val initialDateEpochDay: Long?,
    private val scheduleRepository: ScheduleRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val saveScheduleUseCase: SaveScheduleUseCase,
    private val clock: Clock,
    private val dispatcher: CoroutineDispatcher,
    private val idProvider: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val _uiState = MutableStateFlow<UpsertScheduleUiState>(UpsertScheduleUiState.Loading)
    val uiState: StateFlow<UpsertScheduleUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<UpsertScheduleUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<UpsertScheduleUiEffect> = _uiEffect.receiveAsFlow()

    init {
        loadInitialState()
    }

    fun onEvent(event: UpsertScheduleUiEvent) {
        when (event) {
            is UpsertScheduleUiEvent.OnModeSelected -> selectMode(event.mode)
            is UpsertScheduleUiEvent.OnTitleChanged -> updateContent { it.copy(title = event.title, titleError = false) }
            is UpsertScheduleUiEvent.OnAllDayToggled -> toggleAllDay(event.isAllDay)
            is UpsertScheduleUiEvent.OnPickerToggled -> togglePicker(event.picker)
            is UpsertScheduleUiEvent.OnDateSelected -> selectDate(event.target, event.date)
            is UpsertScheduleUiEvent.OnTimeChanged -> changeTime(event.target, event.time)
            is UpsertScheduleUiEvent.OnQuickDateChipClicked -> selectQuickDate(event.daysOffset)
            is UpsertScheduleUiEvent.OnQuickTimeChipClicked -> selectQuickTime(event.quickTime)
            is UpsertScheduleUiEvent.OnReminderOffsetSelected -> updateContent { it.copy(reminderOffset = event.offset) }
            is UpsertScheduleUiEvent.OnCalendarSourceSelected -> updateContent { it.copy(calendarSource = event.source) }
            is UpsertScheduleUiEvent.OnLocationChanged -> updateContent { it.copy(location = event.location) }
            is UpsertScheduleUiEvent.OnNotesChanged -> updateContent { it.copy(notes = event.notes) }
            UpsertScheduleUiEvent.OnSaveClicked -> save()
            UpsertScheduleUiEvent.OnCancelClicked -> sendEffect(UpsertScheduleUiEffect.NavigateBack)
        }
    }

    private fun loadInitialState() {
        viewModelScope.launch(dispatcher) {
            val initialState = runCatching { buildInitialState() }.getOrNull()
            if (initialState == null) {
                // O estado permanece Loading: a tela fecha logo em seguida.
                _uiEffect.send(UpsertScheduleUiEffect.ShowMessage(R.string.upsert_not_found))
                _uiEffect.send(UpsertScheduleUiEffect.NavigateBack)
            } else {
                _uiState.value = initialState
            }
        }
    }

    /** `null` quando o item a editar não existe. Falhas de I/O propagam e são tratadas por [loadInitialState]. */
    private suspend fun buildInitialState(): UpsertScheduleUiState.Content? {
        val settings = userSettingsRepository.current()
        if (scheduleId != null) {
            val item = scheduleRepository.getById(scheduleId) ?: return null
            return item.toContent(accountEmail = settings.activeAccountEmail)
        }
        val nextFullHour = ScheduleDraftTimeRules.nextFullHour(LocalDateTime.now(clock))
        val startDate = initialDateEpochDay?.let(LocalDate::ofEpochDay) ?: nextFullHour.toLocalDate()
        val startTime = nextFullHour.toLocalTime()
        return UpsertScheduleUiState.Content(
            startDate = startDate,
            startTime = startTime,
            endDate = startDate,
            endTime = startTime,
            reminderOffset = settings.defaultReminderOffset,
            calendarSource = CalendarSource.defaultFor(ScheduleType.REMINDER),
            accountEmail = settings.activeAccountEmail,
        )
    }

    private fun ScheduleItem.toContent(accountEmail: String) =
        UpsertScheduleUiState.Content(
            scheduleId = id,
            mode = type,
            title = title,
            isAllDay = isAllDay,
            startDate = startDate,
            startTime = startTime,
            endDate = endDate,
            endTime = endTime ?: startTime,
            reminderOffset = reminderOffset,
            calendarSource = calendarSource,
            accountEmail = accountEmail,
            recurrencePattern = recurrencePattern,
            colorHex = colorHex,
            stickerEmoji = stickerEmoji,
            location = location,
            notes = notes,
        )

    private fun selectMode(mode: ScheduleType) {
        updateContent { content ->
            if (content.mode == mode) return@updateContent content
            when (mode) {
                ScheduleType.EVENT -> {
                    content
                        .withTimes(ScheduleDraftTimeRules.switchToEvent(content.toDraftTimes()))
                        .copy(mode = mode, calendarSource = CalendarSource.defaultFor(mode), activePicker = ActiveInlinePicker.NONE)
                }

                ScheduleType.REMINDER -> {
                    content
                        .withTimes(ScheduleDraftTimeRules.switchToReminder(content.toDraftTimes()))
                        .copy(
                            mode = mode,
                            calendarSource = CalendarSource.defaultFor(mode),
                            location = "",
                            activePicker = ActiveInlinePicker.NONE,
                        )
                }
            }
        }
    }

    private fun toggleAllDay(isAllDay: Boolean) {
        updateContent { content ->
            if (content.mode != ScheduleType.EVENT) return@updateContent content
            val timePickerWasOpen = content.activePicker.isTimePicker()
            content.copy(
                isAllDay = isAllDay,
                activePicker = if (isAllDay && timePickerWasOpen) ActiveInlinePicker.NONE else content.activePicker,
            )
        }
    }

    private fun togglePicker(picker: ActiveInlinePicker) {
        updateContent { content ->
            content.copy(activePicker = if (content.activePicker == picker) ActiveInlinePicker.NONE else picker)
        }
    }

    private fun selectDate(
        target: ActiveInlinePicker,
        date: LocalDate,
    ) {
        updateContent { content ->
            when (target) {
                ActiveInlinePicker.START_DATE -> content.moveStart(LocalDateTime.of(date, content.startTime))
                ActiveInlinePicker.END_DATE -> content.moveEnd(LocalDateTime.of(date, content.endTime))
                else -> content
            }
        }
    }

    private fun changeTime(
        target: ActiveInlinePicker,
        time: LocalTime,
    ) {
        updateContent { content ->
            when (target) {
                ActiveInlinePicker.START_TIME -> content.moveStart(LocalDateTime.of(content.startDate, time))
                ActiveInlinePicker.END_TIME -> content.moveEnd(LocalDateTime.of(content.endDate, time))
                else -> content
            }
        }
    }

    private fun selectQuickDate(daysOffset: Int) {
        val date = ScheduleDraftTimeRules.quickDate(daysOffset, today = LocalDate.now(clock))
        updateContent { content -> content.moveStart(LocalDateTime.of(date, content.startTime)) }
    }

    private fun selectQuickTime(quickTime: QuickTime) {
        updateContent { content ->
            when (quickTime) {
                QuickTime.InOneHour -> content.moveStart(ScheduleDraftTimeRules.inOneHour(LocalDateTime.now(clock)))
                is QuickTime.At -> content.moveStart(LocalDateTime.of(content.startDate, quickTime.time))
            }
        }
    }

    private fun save() {
        val content = _uiState.value as? UpsertScheduleUiState.Content ?: return
        if (content.isSaving) return
        _uiState.value = content.copy(isSaving = true)
        val item = content.toScheduleItem(id = scheduleId ?: idProvider())
        viewModelScope.launch(dispatcher) {
            saveScheduleUseCase(item)
                .onSuccess { _uiEffect.send(UpsertScheduleUiEffect.NavigateBack) }
                .onFailure { error -> handleSaveFailure(error) }
        }
    }

    private suspend fun handleSaveFailure(error: Throwable) {
        updateContent { it.copy(isSaving = false, titleError = error == SaveScheduleError.BlankTitle) }
        when (error) {
            SaveScheduleError.BlankTitle -> Unit
            SaveScheduleError.EndBeforeStart -> _uiEffect.send(UpsertScheduleUiEffect.ShowMessage(R.string.upsert_error_end_before_start))
            else -> _uiEffect.send(UpsertScheduleUiEffect.ShowMessage(R.string.upsert_save_error))
        }
    }

    /** Lembrete: sem fim, dia inteiro nem local; calendário fixo "Meus lembretes". */
    private fun UpsertScheduleUiState.Content.toScheduleItem(id: String): ScheduleItem =
        when (mode) {
            ScheduleType.REMINDER -> {
                ScheduleItem(
                    id = id,
                    title = title,
                    type = mode,
                    startDate = startDate,
                    endDate = startDate,
                    startTime = startTime,
                    endTime = null,
                    isAllDay = false,
                    location = "",
                    notes = notes,
                    colorHex = colorHex,
                    calendarSource = CalendarSource.MY_REMINDERS,
                    reminderOffset = reminderOffset,
                    recurrencePattern = recurrencePattern,
                    stickerEmoji = stickerEmoji,
                )
            }

            ScheduleType.EVENT -> {
                ScheduleItem(
                    id = id,
                    title = title,
                    type = mode,
                    startDate = startDate,
                    endDate = endDate,
                    startTime = startTime,
                    endTime = if (isAllDay) null else endTime,
                    isAllDay = isAllDay,
                    location = location,
                    notes = notes,
                    colorHex = colorHex,
                    calendarSource = calendarSource,
                    reminderOffset = reminderOffset,
                    recurrencePattern = recurrencePattern,
                    stickerEmoji = stickerEmoji,
                )
            }
        }

    private fun UpsertScheduleUiState.Content.toDraftTimes() = ScheduleDraftTimes(startDate, startTime, endDate, endTime, isAllDay)

    private fun UpsertScheduleUiState.Content.withTimes(times: ScheduleDraftTimes) =
        copy(
            startDate = times.startDate,
            startTime = times.startTime,
            endDate = times.endDate,
            endTime = times.endTime,
            isAllDay = times.isAllDay,
        )

    private fun UpsertScheduleUiState.Content.moveStart(newStart: LocalDateTime) =
        withTimes(ScheduleDraftTimeRules.withNewStart(toDraftTimes(), newStart))

    private fun UpsertScheduleUiState.Content.moveEnd(newEnd: LocalDateTime) =
        withTimes(ScheduleDraftTimeRules.withNewEnd(toDraftTimes(), newEnd))

    private fun ActiveInlinePicker.isTimePicker() = this == ActiveInlinePicker.START_TIME || this == ActiveInlinePicker.END_TIME

    private inline fun updateContent(transform: (UpsertScheduleUiState.Content) -> UpsertScheduleUiState.Content) {
        _uiState.update { state -> if (state is UpsertScheduleUiState.Content) transform(state) else state }
    }

    private fun sendEffect(effect: UpsertScheduleUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
