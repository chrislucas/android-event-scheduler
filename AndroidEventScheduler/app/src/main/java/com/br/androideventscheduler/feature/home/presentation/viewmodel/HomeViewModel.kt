package com.br.androideventscheduler.feature.home.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleOccurrence
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.feature.home.domain.calendar.MonthGridBuilder
import com.br.androideventscheduler.feature.home.domain.repository.HomePreferencesRepository
import com.br.androideventscheduler.feature.home.presentation.effect.HomeUiEffect
import com.br.androideventscheduler.feature.home.presentation.event.HomeUiEvent
import com.br.androideventscheduler.feature.home.presentation.state.CalendarDayCell
import com.br.androideventscheduler.feature.home.presentation.state.HomeUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

/** Resultado da consulta da grade visível, privado ao ViewModel. */
private sealed interface GridData {
    data class Loaded(
        val month: YearMonth,
        val grid: List<LocalDate>,
        val occurrences: List<ScheduleOccurrence>,
    ) : GridData

    data class Failed(
        val month: YearMonth,
    ) : GridData
}

/** Fontes "de cromo" da tela (seletor de mês, gaveta, filtros e conta), agrupadas para caber no `combine`. */
private data class HomeChrome(
    val pickerYear: Int?,
    val isDrawerOpen: Boolean,
    val enabledSources: Set<CalendarSource>,
    val accountEmail: String,
)

/**
 * Adesivo do dia [epochDay] e visibilidade do seu seletor (spec 09), agrupados para caber no `combine`.
 * [epochDay] permite ignorar o adesivo de outro dia durante a troca de seleção.
 */
private data class DayStickerSource(
    val epochDay: Long,
    val sticker: String?,
    val isPickerVisible: Boolean,
)

/**
 * Home reativa. O `uiState` é **derivado** (`combine`) de fontes privadas — o dia selecionado (no
 * `SavedStateHandle`), o ano do seletor de mês, a gaveta lateral (também no `SavedStateHandle`), os
 * calendários visíveis (DataStore) e o e-mail da conta — e das ocorrências observadas no repositório; os eventos
 * só escrevem nessas fontes. O mês visível é sempre o mês do dia selecionado
 * (trocar de mês também move a seleção, como em R3.2), o que mantém os dois consistentes e persistidos juntos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class HomeViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val homePreferencesRepository: HomePreferencesRepository,
    private val monthGridBuilder: MonthGridBuilder,
    private val clock: Clock,
    private val savedStateHandle: SavedStateHandle,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val selectedDateFlow: Flow<LocalDate> =
        savedStateHandle
            .getStateFlow(KEY_SELECTED_EPOCH_DAY, LocalDate.now(clock).toEpochDay())
            .map { epochDay -> LocalDate.ofEpochDay(epochDay) }
    private val monthPickerYearFlow = MutableStateFlow<Int?>(null)
    private val drawerOpenFlow = savedStateHandle.getStateFlow(KEY_DRAWER_OPEN, false)
    private val dayStickerPickerFlow = savedStateHandle.getStateFlow(KEY_DAY_STICKER_PICKER_OPEN, false)
    private val retryTrigger = MutableStateFlow(0)

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<HomeUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<HomeUiEffect> = _uiEffect.receiveAsFlow()

    init {
        viewModelScope.launch(dispatcher) {
            combine(observeGridData(), selectedDateFlow, observeChrome(), observeDaySticker(), ::toUiState)
                .filterNotNull()
                .collect { state -> _uiState.value = state }
        }
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.OnPreviousMonthClicked -> {
                moveToMonth(visibleMonth().minusMonths(1))
            }

            HomeUiEvent.OnNextMonthClicked -> {
                moveToMonth(visibleMonth().plusMonths(1))
            }

            HomeUiEvent.OnMonthHeaderClicked -> {
                monthPickerYearFlow.value = visibleMonth().year
            }

            HomeUiEvent.OnMonthPickerDismissed -> {
                monthPickerYearFlow.value = null
            }

            is HomeUiEvent.OnMonthPickerYearChanged -> {
                monthPickerYearFlow.update { year -> year?.plus(event.delta) }
            }

            is HomeUiEvent.OnMonthPicked -> {
                monthPickerYearFlow.value = null
                moveToMonth(event.month)
            }

            HomeUiEvent.OnSearchClicked -> {
                sendEffect(HomeUiEffect.NavigateToSearch)
            }

            HomeUiEvent.OnQuickAddClicked -> {
                sendEffect(HomeUiEffect.NavigateToUpsert(selectedDate().toEpochDay()))
            }

            is HomeUiEvent.OnScheduleClicked -> {
                sendEffect(HomeUiEffect.NavigateToDetail(event.scheduleId))
            }

            HomeUiEvent.OnRetryClicked -> {
                retryTrigger.update { it + 1 }
            }

            is HomeUiEvent.DrawerEvent -> {
                onDrawerEvent(event)
            }

            is HomeUiEvent.DayStickerEvent -> {
                onDayStickerEvent(event)
            }

            is HomeUiEvent.DayCellEvent -> {
                onDayCellEvent(event)
            }
        }
    }

    /** Toques na célula do dia (spec 05 e spec 18). */
    private fun onDayCellEvent(event: HomeUiEvent.DayCellEvent) {
        when (event) {
            is HomeUiEvent.OnDayClicked -> {
                selectDate(event.date)
            }

            is HomeUiEvent.OnDayDoubleTapped -> {
                // R3.1: selecionar muda o mês visível quando o dia é de um mês vizinho.
                selectDate(event.date)
                sendEffect(HomeUiEffect.NavigateToUpsert(event.date.toEpochDay()))
            }
        }
    }

    /** Eventos da gaveta lateral (spec 06). */
    private fun onDrawerEvent(event: HomeUiEvent.DrawerEvent) {
        when (event) {
            HomeUiEvent.OnMenuClicked -> {
                savedStateHandle[KEY_DRAWER_OPEN] = true
            }

            HomeUiEvent.OnDrawerDismissed -> {
                savedStateHandle[KEY_DRAWER_OPEN] = false
            }

            is HomeUiEvent.OnCalendarSourceToggled -> {
                toggleCalendar(event.source)
            }

            HomeUiEvent.OnSettingsClicked -> {
                savedStateHandle[KEY_DRAWER_OPEN] = false
                sendEffect(HomeUiEffect.NavigateToSettings)
            }
        }
    }

    /** Eventos do adesivo do dia (spec 09). */
    private fun onDayStickerEvent(event: HomeUiEvent.DayStickerEvent) {
        when (event) {
            HomeUiEvent.OnDayStickerClicked -> {
                savedStateHandle[KEY_DAY_STICKER_PICKER_OPEN] = true
            }

            HomeUiEvent.OnDayStickerPickerDismissed -> {
                savedStateHandle[KEY_DAY_STICKER_PICKER_OPEN] = false
            }

            is HomeUiEvent.OnDayStickerSelected -> {
                savedStateHandle[KEY_DAY_STICKER_PICKER_OPEN] = false
                setDaySticker(event.emoji)
            }
        }
    }

    /** O adesivo exibido vem do fluxo do repositório (fonte única); falha de gravação mantém o que já estava. */
    private fun setDaySticker(emoji: String?) {
        val epochDay = selectedDate().toEpochDay()
        viewModelScope.launch(dispatcher) {
            try {
                homePreferencesRepository.setDaySticker(epochDay, emoji)
            } catch (_: IOException) {
                // Sem efeito visível: a próxima emissão do DataStore continua sendo a verdade.
            }
        }
    }

    private fun selectedDate(): LocalDate =
        LocalDate.ofEpochDay(savedStateHandle[KEY_SELECTED_EPOCH_DAY] ?: LocalDate.now(clock).toEpochDay())

    private fun visibleMonth(): YearMonth = YearMonth.from(selectedDate())

    private fun selectDate(date: LocalDate) {
        savedStateHandle[KEY_SELECTED_EPOCH_DAY] = date.toEpochDay()
    }

    /** R3.2: hoje, se [month] é o mês atual; senão o mesmo dia do mês, limitado ao último dia de [month]. */
    private fun moveToMonth(month: YearMonth) {
        val today = LocalDate.now(clock)
        val target =
            if (month == YearMonth.from(today)) {
                today
            } else {
                month.atDay(selectedDate().dayOfMonth.coerceAtMost(month.lengthOfMonth()))
            }
        selectDate(target)
    }

    /** A UI só reflete o resultado pelo fluxo do repositório (fonte única); falha de gravação mantém o filtro atual. */
    private fun toggleCalendar(source: CalendarSource) {
        viewModelScope.launch(dispatcher) {
            try {
                val current = homePreferencesRepository.observeEnabledCalendars().first()
                homePreferencesRepository.setCalendarEnabled(source, enabled = source !in current)
            } catch (_: IOException) {
                // Sem efeito visível: a próxima emissão do DataStore continua sendo a verdade.
            }
        }
    }

    private fun observeChrome(): Flow<HomeChrome> =
        combine(
            monthPickerYearFlow,
            drawerOpenFlow,
            homePreferencesRepository.observeEnabledCalendars(),
            userSettingsRepository.observe().map { it.activeAccountEmail }.distinctUntilChanged(),
            ::HomeChrome,
        )

    private fun observeDaySticker(): Flow<DayStickerSource> =
        selectedDateFlow
            .flatMapLatest { date ->
                val epochDay = date.toEpochDay()
                combine(homePreferencesRepository.observeDaySticker(epochDay), dayStickerPickerFlow) { sticker, isPickerVisible ->
                    DayStickerSource(epochDay, sticker, isPickerVisible)
                }
            }

    private fun observeGridData(): Flow<GridData> =
        combine(selectedDateFlow.map { YearMonth.from(it) }.distinctUntilChanged(), retryTrigger) { month, _ -> month }
            .flatMapLatest { month ->
                val grid = monthGridBuilder.build(month)
                scheduleRepository
                    .observeOccurrences(grid.first(), grid.last())
                    .map<List<ScheduleOccurrence>, GridData> { occurrences -> GridData.Loaded(month, grid, occurrences) }
                    .catch { emit(GridData.Failed(month)) }
            }

    /** `null` enquanto o resultado ainda é de outro mês que o da seleção (troca de mês em andamento). */
    private fun toUiState(
        data: GridData,
        selectedDate: LocalDate,
        chrome: HomeChrome,
        dayStickerSource: DayStickerSource,
    ): HomeUiState? =
        when {
            data is GridData.Failed && data.month == YearMonth.from(selectedDate) -> {
                HomeUiState.Error(R.string.home_error_generic)
            }

            data is GridData.Loaded && data.month == YearMonth.from(selectedDate) -> {
                buildContent(data, selectedDate, chrome, dayStickerSource)
            }

            else -> {
                null
            }
        }

    /** Recebe as ocorrências já prontas e aplica o filtro de calendários (só a Home; pesquisa e alarmes o ignoram). */
    private fun buildContent(
        data: GridData.Loaded,
        selectedDate: LocalDate,
        chrome: HomeChrome,
        dayStickerSource: DayStickerSource,
    ): HomeUiState.Content {
        val today = LocalDate.now(clock)
        val ordered = data.occurrences.filter { it.item.calendarSource in chrome.enabledSources }.sortedWith(dayOrder)
        val cells =
            data.grid.map { date ->
                val onDate = ordered.filter { occurrence -> date in occurrence.startDate..occurrence.endDate }
                CalendarDayCell(
                    date = date,
                    isInVisibleMonth = YearMonth.from(date) == data.month,
                    isToday = date == today,
                    isSelected = date == selectedDate,
                    indicatorColors = onDate.map { it.item.colorHex }.distinct().take(MAX_INDICATORS),
                    scheduleCount = onDate.size,
                )
            }
        return HomeUiState.Content(
            visibleMonth = data.month,
            selectedDate = selectedDate,
            cells = cells,
            daySchedules = ordered.filter { occurrence -> selectedDate in occurrence.startDate..occurrence.endDate },
            accountEmail = chrome.accountEmail,
            isMonthPickerVisible = chrome.pickerYear != null,
            monthPickerYear = chrome.pickerYear ?: data.month.year,
            isDrawerOpen = chrome.isDrawerOpen,
            enabledSources = chrome.enabledSources,
            selectedDaySticker = dayStickerSource.sticker.takeIf { dayStickerSource.epochDay == selectedDate.toEpochDay() },
            isDayStickerPickerVisible = dayStickerSource.isPickerVisible,
        )
    }

    private fun sendEffect(effect: HomeUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }

    private companion object {
        const val KEY_SELECTED_EPOCH_DAY = "home_selected_epoch_day"
        const val KEY_DRAWER_OPEN = "home_drawer_open"
        const val KEY_DAY_STICKER_PICKER_OPEN = "home_day_sticker_picker_open"
        const val MAX_INDICATORS = 3

        /** Dia inteiro primeiro; depois por horário de início. */
        val dayOrder: Comparator<ScheduleOccurrence> =
            compareBy<ScheduleOccurrence> { !it.item.isAllDay }
                .thenBy { it.item.startTime }
                .thenBy { it.item.id }
    }
}
