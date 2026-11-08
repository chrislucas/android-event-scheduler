package com.br.androideventscheduler.feature.alarm.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags

private val BUTTON_MIN_HEIGHT = 56.dp

/** Botões "Adiar" (pela duração escolhida) e "Dispensar"; desabilitados enquanto o adiamento é processado. */
@Composable
internal fun AlarmActionButtons(
    snoozeMinutes: Int,
    enabled: Boolean,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snoozeDescription = pluralStringResource(R.plurals.alarm_snooze_button_description, snoozeMinutes, snoozeMinutes)
    val dismissDescription = stringResource(R.string.alarm_dismiss_button_description)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onSnooze,
            enabled = enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = BUTTON_MIN_HEIGHT)
                    .testTag(SemanticsTags.ALARM_FULLSCREEN_SNOOZE_BUTTON)
                    .semantics { contentDescription = snoozeDescription },
        ) {
            Text(stringResource(R.string.alarm_snooze_button))
        }
        OutlinedButton(
            onClick = onDismiss,
            enabled = enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = BUTTON_MIN_HEIGHT)
                    .testTag(SemanticsTags.ALARM_FULLSCREEN_DISMISS_BUTTON)
                    .semantics { contentDescription = dismissDescription },
        ) {
            Text(stringResource(R.string.alarm_dismiss_button))
        }
    }
}

@Preview(name = "Habilitados", showBackground = true)
@Composable
private fun AlarmActionButtonsPreview() {
    AndroidEventSchedulerTheme {
        AlarmActionButtons(snoozeMinutes = 10, enabled = true, onSnooze = {}, onDismiss = {})
    }
}

@Preview(name = "Processando", showBackground = true)
@Composable
private fun AlarmActionButtonsProcessingPreview() {
    AndroidEventSchedulerTheme {
        AlarmActionButtons(snoozeMinutes = 5, enabled = false, onSnooze = {}, onDismiss = {})
    }
}
