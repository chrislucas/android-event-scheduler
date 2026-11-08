package com.br.androideventscheduler.feature.upsert.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.core.ui.format.toShortDateLabel
import com.br.androideventscheduler.core.ui.format.toTimeLabel
import com.br.androideventscheduler.feature.upsert.presentation.event.QuickTime
import com.br.androideventscheduler.feature.upsert.presentation.state.ActiveInlinePicker
import java.time.LocalDate
import java.time.LocalTime

private const val QUICK_DAYS_TODAY = 0
private const val QUICK_DAYS_TOMORROW = 1
private const val QUICK_DAYS_THREE = 3
private const val QUICK_DAYS_WEEK = 7
private val QUICK_TIME_MORNING: LocalTime = LocalTime.of(7, 0)
private val QUICK_TIME_AFTERNOON: LocalTime = LocalTime.of(15, 0)

/** Valores exibidos pela [DateTimeSection]. */
internal data class DateTimeSectionState(
    val mode: ScheduleType,
    val isAllDay: Boolean,
    val startDate: LocalDate,
    val startTime: LocalTime,
    val endDate: LocalDate,
    val endTime: LocalTime,
    val activePicker: ActiveInlinePicker,
)

/** Ações da [DateTimeSection]; cada uma vira um evento do ViewModel na tela. */
internal data class DateTimeSectionActions(
    val onAllDayToggled: (Boolean) -> Unit,
    val onPickerToggled: (ActiveInlinePicker) -> Unit,
    val onDateSelected: (ActiveInlinePicker, LocalDate) -> Unit,
    val onTimeChanged: (ActiveInlinePicker, LocalTime) -> Unit,
    val onQuickDate: (Int) -> Unit,
    val onQuickTime: (QuickTime) -> Unit,
)

/**
 * Datas e horas do formulário. Evento: dia inteiro, início e fim (data e hora); Lembrete: data e hora.
 * Cada linha abre/fecha o seu seletor inline logo abaixo; só um fica aberto por vez (`activePicker`).
 */
@Composable
internal fun DateTimeSection(
    state: DateTimeSectionState,
    actions: DateTimeSectionActions,
    modifier: Modifier = Modifier,
) {
    val isEvent = state.mode == ScheduleType.EVENT
    Column(modifier = modifier.fillMaxWidth()) {
        if (isEvent) {
            AllDayRow(isAllDay = state.isAllDay, onToggled = actions.onAllDayToggled)
        }
        DateRow(
            label = stringResource(if (isEvent) R.string.upsert_label_start_date else R.string.upsert_label_reminder_date),
            date = state.startDate,
            target = ActiveInlinePicker.START_DATE,
            tag = SemanticsTags.UPSERT_DATE_SELECTOR_ROW,
            state = state,
            actions = actions,
        )
        if (!state.isAllDay) {
            TimeRow(
                label = stringResource(if (isEvent) R.string.upsert_label_start_time else R.string.upsert_label_reminder_time),
                time = state.startTime,
                target = ActiveInlinePicker.START_TIME,
                tag = SemanticsTags.UPSERT_TIME_SELECTOR_ROW,
                state = state,
                actions = actions,
            )
        }
        if (isEvent) {
            DateRow(
                label = stringResource(R.string.upsert_label_end_date),
                date = state.endDate,
                target = ActiveInlinePicker.END_DATE,
                tag = SemanticsTags.UPSERT_END_DATE_SELECTOR_ROW,
                state = state,
                actions = actions,
            )
            if (!state.isAllDay) {
                TimeRow(
                    label = stringResource(R.string.upsert_label_end_time),
                    time = state.endTime,
                    target = ActiveInlinePicker.END_TIME,
                    tag = SemanticsTags.UPSERT_END_TIME_SELECTOR_ROW,
                    state = state,
                    actions = actions,
                )
            }
        }
    }
}

@Composable
private fun DateRow(
    label: String,
    date: LocalDate,
    target: ActiveInlinePicker,
    tag: String,
    state: DateTimeSectionState,
    actions: DateTimeSectionActions,
) {
    val isOpen = state.activePicker == target
    SettingRow(
        iconRes = R.drawable.ic_calendar_today,
        label = label,
        value = date.toShortDateLabel(),
        tag = tag,
        onClick = { actions.onPickerToggled(target) },
        isExpanded = isOpen,
    )
    AnimatedVisibility(visible = isOpen) {
        Column {
            // Atalhos de data só para Lembrete (o início do Evento usa o calendário).
            if (state.mode == ScheduleType.REMINDER) {
                QuickChipsRow(
                    chips = dateChips(actions.onQuickDate),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            InlineDatePicker(
                selectedDate = date,
                onDateSelected = { pickedDate -> actions.onDateSelected(target, pickedDate) },
            )
        }
    }
}

@Composable
private fun TimeRow(
    label: String,
    time: LocalTime,
    target: ActiveInlinePicker,
    tag: String,
    state: DateTimeSectionState,
    actions: DateTimeSectionActions,
) {
    val isOpen = state.activePicker == target
    SettingRow(
        iconRes = R.drawable.ic_schedule,
        label = label,
        value = time.toTimeLabel(),
        tag = tag,
        onClick = { actions.onPickerToggled(target) },
        isExpanded = isOpen,
    )
    AnimatedVisibility(visible = isOpen) {
        Column {
            // Atalhos de hora mudam o início (e o fim acompanha); o seletor do fim não os oferece.
            if (target == ActiveInlinePicker.START_TIME) {
                QuickChipsRow(
                    chips = timeChips(actions.onQuickTime),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            InlineTimePicker(
                selectedTime = time,
                onTimeChanged = { pickedTime -> actions.onTimeChanged(target, pickedTime) },
            )
        }
    }
}

@Composable
private fun dateChips(onQuickDate: (Int) -> Unit): List<QuickChip> =
    listOf(
        QuickChip("today", stringResource(R.string.upsert_chip_today)) { onQuickDate(QUICK_DAYS_TODAY) },
        QuickChip("tomorrow", stringResource(R.string.upsert_chip_tomorrow)) { onQuickDate(QUICK_DAYS_TOMORROW) },
        QuickChip("in_3_days", stringResource(R.string.upsert_chip_in_3_days)) { onQuickDate(QUICK_DAYS_THREE) },
        QuickChip("in_1_week", stringResource(R.string.upsert_chip_in_1_week)) { onQuickDate(QUICK_DAYS_WEEK) },
    )

@Composable
private fun timeChips(onQuickTime: (QuickTime) -> Unit): List<QuickChip> =
    listOf(
        QuickChip("in_1_hour", stringResource(R.string.upsert_chip_in_1_hour)) { onQuickTime(QuickTime.InOneHour) },
        QuickChip("at_07_00", QUICK_TIME_MORNING.toTimeLabel()) { onQuickTime(QuickTime.At(QUICK_TIME_MORNING)) },
        QuickChip("at_15_00", QUICK_TIME_AFTERNOON.toTimeLabel()) { onQuickTime(QuickTime.At(QUICK_TIME_AFTERNOON)) },
    )

@Composable
private fun AllDayRow(
    isAllDay: Boolean,
    onToggled: (Boolean) -> Unit,
) {
    val label = stringResource(R.string.upsert_all_day_label)
    val description = stringResource(R.string.upsert_all_day_description)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = isAllDay, role = Role.Switch, onValueChange = onToggled)
                .semantics { contentDescription = description }
                .testTag(SemanticsTags.UPSERT_ALL_DAY_SWITCH)
                .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = isAllDay, onCheckedChange = null)
    }
}

private val previewActions =
    DateTimeSectionActions(
        onAllDayToggled = {},
        onPickerToggled = {},
        onDateSelected = { _, _ -> },
        onTimeChanged = { _, _ -> },
        onQuickDate = {},
        onQuickTime = {},
    )

private val previewState =
    DateTimeSectionState(
        mode = ScheduleType.EVENT,
        isAllDay = false,
        startDate = LocalDate.of(2026, 11, 5),
        startTime = LocalTime.of(22, 0),
        endDate = LocalDate.of(2026, 11, 5),
        endTime = LocalTime.of(23, 0),
        activePicker = ActiveInlinePicker.NONE,
    )

@Preview(name = "Evento", showBackground = true)
@Composable
private fun DateTimeSectionEventPreview() {
    AndroidEventSchedulerTheme { DateTimeSection(state = previewState, actions = previewActions) }
}

@Preview(name = "Evento de dia inteiro", showBackground = true)
@Composable
private fun DateTimeSectionAllDayPreview() {
    AndroidEventSchedulerTheme {
        DateTimeSection(state = previewState.copy(isAllDay = true), actions = previewActions)
    }
}

@Preview(name = "Lembrete com calendário aberto", showBackground = true)
@Composable
private fun DateTimeSectionReminderCalendarPreview() {
    AndroidEventSchedulerTheme {
        DateTimeSection(
            state = previewState.copy(mode = ScheduleType.REMINDER, activePicker = ActiveInlinePicker.START_DATE),
            actions = previewActions,
        )
    }
}

@Preview(name = "Evento com hora aberta", showBackground = true)
@Composable
private fun DateTimeSectionTimePickerPreview() {
    AndroidEventSchedulerTheme {
        DateTimeSection(state = previewState.copy(activePicker = ActiveInlinePicker.START_TIME), actions = previewActions)
    }
}
