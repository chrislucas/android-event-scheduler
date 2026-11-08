package com.br.androideventscheduler.core.ui.format

import android.content.res.Resources
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.common.StringProvider
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset

private const val MINUTES_PER_HOUR = 60

/** Rótulo do alerta: `Sem alerta`, `10 minutos antes`, `1 hora antes`. */
fun ReminderOffset.toDisplayLabel(resources: Resources): String = toDisplayLabel(ResourcesStringProvider(resources))

/** Mesma formatação de [toDisplayLabel] para quem já trabalha com [StringProvider] (mappers testáveis na JVM). */
fun ReminderOffset.toDisplayLabel(strings: StringProvider): String {
    val offsetMinutes = minutes ?: return strings.getString(R.string.reminder_none)
    return if (offsetMinutes == MINUTES_PER_HOUR) {
        strings.getString(R.string.reminder_one_hour_before)
    } else {
        strings.getQuantityString(R.plurals.reminder_minutes_before, offsetMinutes, offsetMinutes)
    }
}
