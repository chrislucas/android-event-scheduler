package com.br.androideventscheduler.core.ui.format

import android.content.res.Resources
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset

private const val MINUTES_PER_HOUR = 60

/** Rótulo do alerta: `Sem alerta`, `10 minutos antes`, `1 hora antes`. */
fun ReminderOffset.toDisplayLabel(resources: Resources): String {
    val offsetMinutes = minutes ?: return resources.getString(R.string.reminder_none)
    return if (offsetMinutes == MINUTES_PER_HOUR) {
        resources.getString(R.string.reminder_one_hour_before)
    } else {
        resources.getQuantityString(R.plurals.reminder_minutes_before, offsetMinutes, offsetMinutes)
    }
}
