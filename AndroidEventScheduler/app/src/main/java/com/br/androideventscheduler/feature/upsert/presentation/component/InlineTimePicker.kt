package com.br.androideventscheduler.feature.upsert.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalTime

/** Seletor de horário inline (M3 `TimePicker`, 24 h). [selectedTime] é a fonte da verdade, como no calendário. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InlineTimePicker(
    selectedTime: LocalTime,
    onTimeChanged: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickerState =
        rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = true,
        )
    val currentSelectedTime = rememberUpdatedState(selectedTime)
    val currentOnTimeChanged = rememberUpdatedState(onTimeChanged)

    LaunchedEffect(selectedTime) {
        if (pickerState.hour != selectedTime.hour) pickerState.hour = selectedTime.hour
        if (pickerState.minute != selectedTime.minute) pickerState.minute = selectedTime.minute
    }
    LaunchedEffect(pickerState) {
        snapshotFlow { LocalTime.of(pickerState.hour, pickerState.minute) }
            .distinctUntilChanged()
            .collect { pickedTime ->
                if (pickedTime != currentSelectedTime.value) currentOnTimeChanged.value(pickedTime)
            }
    }

    val pickerDescription = stringResource(R.string.upsert_inline_time_picker_description)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .semantics { contentDescription = pickerDescription }
                .testTag(SemanticsTags.UPSERT_INLINE_TIME_PICKER),
        contentAlignment = Alignment.Center,
    ) {
        TimePicker(state = pickerState)
    }
}

@Preview(name = "Seletor de horário inline", showBackground = true)
@Composable
private fun InlineTimePickerPreview() {
    AndroidEventSchedulerTheme {
        InlineTimePicker(selectedTime = LocalTime.of(22, 0), onTimeChanged = {})
    }
}
