package com.br.androideventscheduler.core.ui.format

import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.common.StringProvider
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource

/**
 * Rótulo do calendário: o e-mail da conta ativa ([personalAccountLabel]) para [CalendarSource.PERSONAL_ACCOUNT],
 * `Family` ou `Meus lembretes` para os demais.
 */
fun CalendarSource.label(
    personalAccountLabel: String,
    strings: StringProvider,
): String =
    when (this) {
        CalendarSource.PERSONAL_ACCOUNT -> personalAccountLabel
        CalendarSource.FAMILY -> strings.getString(R.string.calendar_label_family)
        CalendarSource.MY_REMINDERS -> strings.getString(R.string.calendar_label_my_reminders)
    }
