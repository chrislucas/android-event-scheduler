package com.br.androideventscheduler.core.ui.format

import android.content.res.Resources
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.common.StringProvider
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import java.time.DayOfWeek
import java.time.format.TextStyle

/** Ordem de exibição dos dias: domingo → sábado (calendário brasileiro). */
private val DISPLAY_WEEK_ORDER =
    listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
    )

/**
 * Rótulo da repetição: `Não repetir`, `Diariamente`, `Toda semana em ter., qui. (5000 vezes)`.
 * O sufixo de quantidade só aparece quando há limite.
 */
fun RecurrencePattern.toDisplayLabel(resources: Resources): String = toDisplayLabel(ResourcesStringProvider(resources))

/** Mesma formatação de [toDisplayLabel] para quem já trabalha com [StringProvider] (mappers testáveis na JVM). */
fun RecurrencePattern.toDisplayLabel(strings: StringProvider): String {
    val baseLabel =
        when (this) {
            RecurrencePattern.None -> return strings.getString(R.string.recurrence_none)
            is RecurrencePattern.Daily -> strings.getString(R.string.recurrence_daily)
            is RecurrencePattern.Weekly -> strings.getString(R.string.recurrence_weekly_on, daysOfWeek.toShortDayList(strings))
        }
    val limit = occurrenceLimit ?: return baseLabel
    val timesLabel = strings.getQuantityString(R.plurals.recurrence_times, limit, limit)
    return strings.getString(R.string.recurrence_with_limit, baseLabel, timesLabel)
}

private fun Set<DayOfWeek>.toShortDayList(strings: StringProvider): String =
    DISPLAY_WEEK_ORDER
        .filter { it in this }
        .joinToString(separator = strings.getString(R.string.list_separator)) { day ->
            day.getDisplayName(TextStyle.SHORT, AppLocale)
        }
