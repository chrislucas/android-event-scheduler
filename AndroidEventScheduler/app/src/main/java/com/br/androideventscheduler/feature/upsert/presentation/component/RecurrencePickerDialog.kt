package com.br.androideventscheduler.feature.upsert.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.ui.SemanticsTags
import java.time.DayOfWeek
import java.time.LocalDate

private val DIALOG_CORNER_RADIUS = 24.dp
private val DIALOG_HORIZONTAL_MARGIN = 16.dp
private val DIALOG_CONTENT_PADDING = 16.dp
private val OPTION_MIN_HEIGHT = 48.dp
private val DAY_TOGGLE_VISUAL_SIZE = 40.dp

private const val DAYS_IN_WEEK = 7

/** Domingo → sábado, como no calendário brasileiro e nos arrays de strings. */
private val DISPLAY_WEEK_ORDER: List<DayOfWeek> =
    listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
    )

/** Persiste o rascunho em tipos primitivos (rotação/morte do processo). */
private val RecurrenceDraftSaver: Saver<RecurrenceDraft, List<String>> =
    Saver(
        save = { draft -> listOf(draft.kind.name, draft.selectedDays.joinToString(",") { it.name }, draft.countText) },
        restore = { saved ->
            RecurrenceDraft(
                kind = RecurrenceKind.valueOf(saved[0]),
                selectedDays =
                    saved[1]
                        .split(",")
                        .filter(String::isNotEmpty)
                        .map(DayOfWeek::valueOf)
                        .toSet(),
                countText = saved[2],
            )
        },
    )

/**
 * Diálogo de repetição. O rascunho é local ([RecurrenceDraft]): nada muda na tela até [onConfirm].
 * Cancelar, tocar fora e voltar chamam [onDismiss].
 */
@Composable
internal fun RecurrencePickerDialog(
    initialPattern: RecurrencePattern,
    startDate: LocalDate,
    onConfirm: (RecurrencePattern) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable(initialPattern, startDate, stateSaver = RecurrenceDraftSaver) {
        mutableStateOf(RecurrenceDraft.from(initialPattern, startDate))
    }
    val dialogDescription = stringResource(R.string.recurrence_dialog_description)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(DIALOG_CORNER_RADIUS),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier =
                modifier
                    .padding(horizontal = DIALOG_HORIZONTAL_MARGIN)
                    .fillMaxWidth()
                    .semantics { contentDescription = dialogDescription }
                    .testTag(SemanticsTags.RECURRENCE_DIALOG),
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(DIALOG_CONTENT_PADDING),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.recurrence_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                RecurrenceOptions(draft = draft, onDraftChange = { draft = it })
                if (draft.kind == RecurrenceKind.WEEKLY) {
                    WeeklyDaySelectorRow(selectedDays = draft.selectedDays, onDayToggled = { draft = draft.toggleDay(it) })
                }
                if (draft.kind != RecurrenceKind.NONE) {
                    RecurrenceCountField(countText = draft.countText, onCountTextChange = { draft = draft.withCountText(it) })
                }
                RecurrenceActions(onConfirm = { onConfirm(draft.toPattern()) }, onDismiss = onDismiss)
            }
        }
    }
}

@Composable
private fun RecurrenceOptions(
    draft: RecurrenceDraft,
    onDraftChange: (RecurrenceDraft) -> Unit,
) {
    Column(modifier = Modifier.selectableGroup()) {
        RecurrenceRadioRow(
            label = stringResource(R.string.recurrence_none),
            isSelected = draft.kind == RecurrenceKind.NONE,
            onClick = { onDraftChange(draft.withKind(RecurrenceKind.NONE)) },
            tag = SemanticsTags.RECURRENCE_OPTION_NONE,
        )
        RecurrenceRadioRow(
            label = stringResource(R.string.recurrence_daily),
            isSelected = draft.kind == RecurrenceKind.DAILY,
            onClick = { onDraftChange(draft.withKind(RecurrenceKind.DAILY)) },
            tag = SemanticsTags.RECURRENCE_OPTION_DAILY,
        )
        RecurrenceRadioRow(
            label = stringResource(R.string.recurrence_option_weekly),
            isSelected = draft.kind == RecurrenceKind.WEEKLY,
            onClick = { onDraftChange(draft.withKind(RecurrenceKind.WEEKLY)) },
            tag = SemanticsTags.RECURRENCE_OPTION_WEEKLY,
        )
    }
}

/** Opção do grupo de rádio: a linha inteira é o alvo de toque; o [RadioButton] é só visual. */
@Composable
internal fun RecurrenceRadioRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = OPTION_MIN_HEIGHT)
                .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
                .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
    }
}

/** Sete alternadores (D S T Q Q S S). O último dia marcado não pode ser desmarcado (regra do [RecurrenceDraft]). */
@Composable
internal fun WeeklyDaySelectorRow(
    selectedDays: Set<DayOfWeek>,
    onDayToggled: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initials = stringArrayResource(R.array.recurrence_day_initials)
    val fullNames = stringArrayResource(R.array.recurrence_day_names)
    val groupDescription = stringResource(R.string.recurrence_days_group_description)
    val selectedState = stringResource(R.string.upsert_state_selected)
    val notSelectedState = stringResource(R.string.upsert_state_not_selected)
    Row(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = groupDescription },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DISPLAY_WEEK_ORDER.forEach { day ->
            val labelIndex = day.value % DAYS_IN_WEEK
            val isSelected = day in selectedDays
            val stateLabel = if (isSelected) selectedState else notSelectedState
            val fullName = fullNames[labelIndex]
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onDayToggled(day) })
                        .semantics {
                            contentDescription = fullName
                            stateDescription = stateLabel
                        }.testTag(SemanticsTags.RECURRENCE_DAY_TOGGLE_PREFIX + day.name),
                contentAlignment = Alignment.Center,
            ) {
                DayToggleVisual(initial = initials[labelIndex], isSelected = isSelected)
            }
        }
    }
}

@Composable
private fun DayToggleVisual(
    initial: String,
    isSelected: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier =
            Modifier
                .size(DAY_TOGGLE_VISUAL_SIZE)
                .background(color = if (isSelected) colors.primary else colors.surfaceVariant, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecurrenceCountField(
    countText: String,
    onCountTextChange: (String) -> Unit,
) {
    val description = stringResource(R.string.recurrence_count_description)
    OutlinedTextField(
        value = countText,
        onValueChange = onCountTextChange,
        label = { Text(stringResource(R.string.recurrence_count_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = description }
                .testTag(SemanticsTags.RECURRENCE_COUNT_INPUT),
    )
}

@Composable
private fun RecurrenceActions(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss, modifier = Modifier.testTag(SemanticsTags.RECURRENCE_DISMISS_BUTTON)) {
            Text(stringResource(R.string.recurrence_dismiss_button))
        }
        TextButton(onClick = onConfirm, modifier = Modifier.testTag(SemanticsTags.RECURRENCE_CONFIRM_BUTTON)) {
            Text(stringResource(R.string.recurrence_confirm_button))
        }
    }
}

private val previewStartDate: LocalDate = LocalDate.of(2026, 11, 5) // quinta-feira

@Preview(name = "Não repetir", showBackground = true)
@Composable
private fun RecurrencePickerDialogNonePreview() {
    AndroidEventSchedulerTheme {
        RecurrencePickerDialog(RecurrencePattern.None, previewStartDate, onConfirm = {}, onDismiss = {})
    }
}

@Preview(name = "Diariamente", showBackground = true)
@Composable
private fun RecurrencePickerDialogDailyPreview() {
    AndroidEventSchedulerTheme {
        RecurrencePickerDialog(RecurrencePattern.Daily(occurrenceLimit = 10), previewStartDate, onConfirm = {}, onDismiss = {})
    }
}

@Preview(name = "Semanal ter+qui, 5000 vezes", showBackground = true)
@Composable
private fun RecurrencePickerDialogWeeklyPreview() {
    AndroidEventSchedulerTheme {
        val pattern = RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), occurrenceLimit = 5000)
        RecurrencePickerDialog(pattern, previewStartDate, onConfirm = {}, onDismiss = {})
    }
}

@Preview(name = "Semanal com um dia (bloqueado)", showBackground = true)
@Composable
private fun RecurrencePickerDialogWeeklySingleDayPreview() {
    AndroidEventSchedulerTheme {
        val pattern = RecurrencePattern.Weekly(setOf(DayOfWeek.THURSDAY))
        RecurrencePickerDialog(pattern, previewStartDate, onConfirm = {}, onDismiss = {})
    }
}

@Preview(name = "Seletor de dias", showBackground = true)
@Composable
private fun WeeklyDaySelectorRowPreview() {
    AndroidEventSchedulerTheme {
        WeeklyDaySelectorRow(selectedDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), onDayToggled = {})
    }
}

@Preview(name = "Opção de rádio", showBackground = true)
@Composable
private fun RecurrenceRadioRowPreview() {
    AndroidEventSchedulerTheme {
        RecurrenceRadioRow(label = "Diariamente", isSelected = true, onClick = {}, tag = "preview")
    }
}
