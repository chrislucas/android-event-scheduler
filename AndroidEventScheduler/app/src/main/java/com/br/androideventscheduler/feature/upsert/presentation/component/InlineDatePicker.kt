package com.br.androideventscheduler.feature.upsert.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.upsert.presentation.mapper.toDatePickerMillis
import com.br.androideventscheduler.feature.upsert.presentation.mapper.toLocalDateFromDatePickerMillis
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Calendário mensal inline (M3 `DatePicker`). O estado do picker é em milissegundos **UTC**: toda conversão
 * passa por [toDatePickerMillis]/[toLocalDateFromDatePickerMillis]. [selectedDate] é a fonte da verdade:
 * mudanças externas (atalhos) são empurradas para o picker e toques do usuário saem por [onDateSelected].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InlineDatePicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toDatePickerMillis(),
            initialDisplayedMonthMillis = selectedDate.toDatePickerMillis(),
        )
    val currentSelectedDate = rememberUpdatedState(selectedDate)
    val currentOnDateSelected = rememberUpdatedState(onDateSelected)

    LaunchedEffect(selectedDate) {
        if (pickerState.selectedDateMillis?.toLocalDateFromDatePickerMillis() != selectedDate) {
            pickerState.selectedDateMillis = selectedDate.toDatePickerMillis()
            pickerState.displayedMonthMillis = selectedDate.toDatePickerMillis()
        }
    }
    LaunchedEffect(pickerState) {
        snapshotFlow { pickerState.selectedDateMillis }
            .filterNotNull()
            .map { it.toLocalDateFromDatePickerMillis() }
            .distinctUntilChanged()
            .collect { pickedDate ->
                if (pickedDate != currentSelectedDate.value) currentOnDateSelected.value(pickedDate)
            }
    }

    val calendarDescription = stringResource(R.string.upsert_inline_calendar_description)
    DatePicker(
        state = pickerState,
        title = null,
        headline = null,
        showModeToggle = false,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = calendarDescription }
                .testTag(SemanticsTags.UPSERT_INLINE_CALENDAR),
    )
}

@Preview(name = "Calendário inline", showBackground = true)
@Composable
private fun InlineDatePickerPreview() {
    AndroidEventSchedulerTheme {
        InlineDatePicker(selectedDate = LocalDate.of(2026, 11, 5), onDateSelected = {})
    }
}
