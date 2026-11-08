package com.br.androideventscheduler.feature.alarm.presentation.state

import androidx.annotation.StringRes
import com.br.androideventscheduler.core.preferences.domain.UserSettings

internal sealed class FullScreenAlarmUiState {
    data object Loading : FullScreenAlarmUiState()

    /**
     * Textos já prontos (título, horário) e dados crus para a UI formatar com resources
     * ([minutesUntilStart], [snoozeOptions]). [errorMessageRes] é a falha do último "Adiar".
     */
    data class Content(
        val title: String,
        val timeText: String,
        val location: String?,
        val notes: String?,
        val colorHex: Long,
        val minutesUntilStart: Int,
        val snoozeOptions: List<Int> = UserSettings.ALLOWED_SNOOZE_MINUTES,
        val selectedSnoozeMinutes: Int = UserSettings.DEFAULT_SNOOZE_MINUTES,
        val isProcessing: Boolean = false,
        @StringRes val errorMessageRes: Int? = null,
    ) : FullScreenAlarmUiState()
}
