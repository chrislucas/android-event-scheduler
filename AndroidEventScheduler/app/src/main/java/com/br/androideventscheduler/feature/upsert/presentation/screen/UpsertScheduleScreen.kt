package com.br.androideventscheduler.feature.upsert.presentation.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.navigation.UpsertSchedule
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.ui.CollectEffects
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.core.ui.format.toDisplayLabel
import com.br.androideventscheduler.feature.upsert.presentation.component.CalendarSourceRow
import com.br.androideventscheduler.feature.upsert.presentation.component.DateTimeSection
import com.br.androideventscheduler.feature.upsert.presentation.component.DateTimeSectionActions
import com.br.androideventscheduler.feature.upsert.presentation.component.DateTimeSectionState
import com.br.androideventscheduler.feature.upsert.presentation.component.LocationField
import com.br.androideventscheduler.feature.upsert.presentation.component.ModeSegmentedPill
import com.br.androideventscheduler.feature.upsert.presentation.component.NotesField
import com.br.androideventscheduler.feature.upsert.presentation.component.ReminderOffsetRow
import com.br.androideventscheduler.feature.upsert.presentation.component.SettingRow
import com.br.androideventscheduler.feature.upsert.presentation.component.TitleRow
import com.br.androideventscheduler.feature.upsert.presentation.effect.UpsertScheduleUiEffect
import com.br.androideventscheduler.feature.upsert.presentation.event.UpsertScheduleUiEvent
import com.br.androideventscheduler.feature.upsert.presentation.state.ActiveInlinePicker
import com.br.androideventscheduler.feature.upsert.presentation.state.UpsertScheduleUiState
import com.br.androideventscheduler.feature.upsert.presentation.viewmodel.UpsertScheduleViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import java.time.LocalTime

private val SCREEN_HORIZONTAL_PADDING = 16.dp

// Sem preview: injeta o ViewModel (Koin); as previews cobrem o conteúdo stateless UpsertScheduleContent.
@Composable
internal fun UpsertScheduleScreen(
    scheduleId: String?,
    initialDateEpochDay: Long?,
    onBack: () -> Unit,
    viewModel: UpsertScheduleViewModel = koinViewModel { parametersOf(UpsertSchedule(scheduleId, initialDateEpochDay)) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    CollectEffects(viewModel.uiEffect) { effect ->
        when (effect) {
            UpsertScheduleUiEffect.NavigateBack -> {
                onBack()
            }

            is UpsertScheduleUiEffect.ShowMessage -> {
                val message = context.getString(effect.messageRes)
                if (uiState is UpsertScheduleUiState.Content) {
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                } else {
                    // Ainda carregando (item inexistente): a tela fecha em seguida e um Snackbar sumiria junto.
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    UpsertScheduleContent(uiState = uiState, snackbarHostState = snackbarHostState, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpsertScheduleContent(
    uiState: UpsertScheduleUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (UpsertScheduleUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = uiState as? UpsertScheduleUiState.Content
    val saveDescription = stringResource(R.string.upsert_save_description)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = content?.let { screenTitle(it) }.orEmpty()) },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(UpsertScheduleUiEvent.OnCancelClicked) },
                        modifier = Modifier.testTag(SemanticsTags.UPSERT_CANCEL_BUTTON),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.upsert_cancel_description),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onEvent(UpsertScheduleUiEvent.OnSaveClicked) },
                        enabled = content != null && !content.isSaving,
                        modifier =
                            Modifier
                                .semantics { contentDescription = saveDescription }
                                .testTag(SemanticsTags.UPSERT_SAVE_BUTTON),
                    ) {
                        Text(stringResource(R.string.upsert_save_button))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (uiState) {
            UpsertScheduleUiState.Loading -> UpsertLoading(Modifier.padding(innerPadding))
            is UpsertScheduleUiState.Content -> UpsertForm(content = uiState, onEvent = onEvent, contentPadding = innerPadding)
        }
    }
}

@Composable
private fun screenTitle(content: UpsertScheduleUiState.Content): String =
    stringResource(if (content.isEditing) R.string.upsert_title_edit else R.string.upsert_title_new)

@Composable
private fun UpsertLoading(modifier: Modifier = Modifier) {
    val loadingDescription = stringResource(R.string.state_loading)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier =
                Modifier
                    .testTag(SemanticsTags.UPSERT_LOADING)
                    .semantics { contentDescription = loadingDescription },
        )
    }
}

@Composable
private fun UpsertForm(
    content: UpsertScheduleUiState.Content,
    onEvent: (UpsertScheduleUiEvent) -> Unit,
    contentPadding: PaddingValues,
) {
    val sectionState =
        DateTimeSectionState(
            mode = content.mode,
            isAllDay = content.isAllDay,
            startDate = content.startDate,
            startTime = content.startTime,
            endDate = content.endDate,
            endTime = content.endTime,
            activePicker = content.activePicker,
        )
    val sectionActions =
        DateTimeSectionActions(
            onAllDayToggled = { onEvent(UpsertScheduleUiEvent.OnAllDayToggled(it)) },
            onPickerToggled = { onEvent(UpsertScheduleUiEvent.OnPickerToggled(it)) },
            onDateSelected = { target, date -> onEvent(UpsertScheduleUiEvent.OnDateSelected(target, date)) },
            onTimeChanged = { target, time -> onEvent(UpsertScheduleUiEvent.OnTimeChanged(target, time)) },
            onQuickDate = { onEvent(UpsertScheduleUiEvent.OnQuickDateChipClicked(it)) },
            onQuickTime = { onEvent(UpsertScheduleUiEvent.OnQuickTimeChipClicked(it)) },
        )
    val resources = LocalContext.current.resources

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding =
            PaddingValues(
                start = SCREEN_HORIZONTAL_PADDING,
                end = SCREEN_HORIZONTAL_PADDING,
                top = innerTop(contentPadding),
                bottom = contentPadding.calculateBottomPadding() + SCREEN_HORIZONTAL_PADDING,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ModeSegmentedPill(mode = content.mode, onModeSelected = { onEvent(UpsertScheduleUiEvent.OnModeSelected(it)) })
        }
        item {
            TitleRow(
                title = content.title,
                isError = content.titleError,
                onTitleChange = { onEvent(UpsertScheduleUiEvent.OnTitleChanged(it)) },
            )
        }
        item { DateTimeSection(state = sectionState, actions = sectionActions) }
        item {
            ReminderOffsetRow(
                selected = content.reminderOffset,
                onSelected = { onEvent(UpsertScheduleUiEvent.OnReminderOffsetSelected(it)) },
            )
        }
        if (content.mode == ScheduleType.EVENT) {
            item {
                CalendarSourceRow(
                    selected = content.calendarSource,
                    accountEmail = content.accountEmail,
                    onSelected = { onEvent(UpsertScheduleUiEvent.OnCalendarSourceSelected(it)) },
                )
            }
            item { LocationField(value = content.location, onValueChange = { onEvent(UpsertScheduleUiEvent.OnLocationChanged(it)) }) }
        }
        item {
            // Somente exibe o rótulo; o diálogo de repetição é a spec 08.
            SettingRow(
                iconRes = R.drawable.ic_repeat,
                label = stringResource(R.string.upsert_recurrence_label),
                value = content.recurrencePattern.toDisplayLabel(resources),
                tag = SemanticsTags.UPSERT_RECURRENCE_ROW,
            )
        }
        item { NotesField(value = content.notes, onValueChange = { onEvent(UpsertScheduleUiEvent.OnNotesChanged(it)) }) }
    }
}

private fun innerTop(contentPadding: PaddingValues) = contentPadding.calculateTopPadding() + 8.dp

private val previewDate: LocalDate = LocalDate.of(2026, 11, 5)

private fun previewContent(
    mode: ScheduleType = ScheduleType.REMINDER,
    block: UpsertScheduleUiState.Content.() -> UpsertScheduleUiState.Content = { this },
): UpsertScheduleUiState.Content =
    UpsertScheduleUiState
        .Content(
            mode = mode,
            title = "Pagar o aluguel",
            startDate = previewDate,
            startTime = LocalTime.of(22, 0),
            endDate = previewDate,
            endTime = if (mode == ScheduleType.EVENT) LocalTime.of(23, 0) else LocalTime.of(22, 0),
            reminderOffset = ReminderOffset.FIFTEEN_MINUTES,
            calendarSource = CalendarSource.defaultFor(mode),
            accountEmail = "ana@exemplo.com",
        ).block()

@Composable
private fun PreviewHost(uiState: UpsertScheduleUiState) {
    AndroidEventSchedulerTheme {
        UpsertScheduleContent(uiState = uiState, snackbarHostState = remember { SnackbarHostState() }, onEvent = {})
    }
}

@Preview(name = "Lembrete novo", showBackground = true)
@Composable
private fun UpsertScheduleContentNewReminderPreview() {
    PreviewHost(previewContent())
}

@Preview(name = "Evento novo", showBackground = true)
@Composable
private fun UpsertScheduleContentNewEventPreview() {
    PreviewHost(previewContent(ScheduleType.EVENT) { copy(title = "") })
}

@Preview(name = "Evento de dia inteiro", showBackground = true)
@Composable
private fun UpsertScheduleContentAllDayPreview() {
    PreviewHost(previewContent(ScheduleType.EVENT) { copy(isAllDay = true, endDate = previewDate.plusDays(2)) })
}

@Preview(name = "Calendário aberto", showBackground = true)
@Composable
private fun UpsertScheduleContentCalendarOpenPreview() {
    PreviewHost(previewContent { copy(activePicker = ActiveInlinePicker.START_DATE) })
}

@Preview(name = "Hora aberta", showBackground = true)
@Composable
private fun UpsertScheduleContentTimeOpenPreview() {
    PreviewHost(previewContent(ScheduleType.EVENT) { copy(activePicker = ActiveInlinePicker.START_TIME) })
}

@Preview(name = "Erro de título", showBackground = true)
@Composable
private fun UpsertScheduleContentTitleErrorPreview() {
    PreviewHost(previewContent { copy(title = "", titleError = true) })
}

@Preview(name = "Salvando", showBackground = true)
@Composable
private fun UpsertScheduleContentSavingPreview() {
    PreviewHost(previewContent { copy(isSaving = true) })
}

@Preview(name = "Edição", showBackground = true)
@Composable
private fun UpsertScheduleContentEditPreview() {
    PreviewHost(
        previewContent(ScheduleType.EVENT) {
            copy(
                scheduleId = "item-1",
                title = "Workshop de arquitetura Android",
                location = "Sala 3, bloco B",
                notes = "Levar o notebook",
                recurrencePattern = RecurrencePattern.Daily(occurrenceLimit = 5),
            )
        },
    )
}

@Preview(name = "Carregando", showBackground = true)
@Composable
private fun UpsertScheduleContentLoadingPreview() {
    PreviewHost(UpsertScheduleUiState.Loading)
}
