package com.br.androideventscheduler.core.ui.format

import android.content.res.Resources
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.common.StringProvider
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d 'de' MMM", AppLocale)
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", AppLocale)

/** Data curta: `qui., 5 de nov.` */
fun LocalDate.toShortDateLabel(): String = format(shortDateFormatter)

/** Hora 24 h: `22:00` */
fun LocalTime.toTimeLabel(): String = format(timeFormatter)

/** Data e hora curtas: `qui., 5 de nov., 22:00` */
fun shortDateTimeLabel(
    date: LocalDate,
    time: LocalTime,
    resources: Resources,
): String = shortDateTimeLabel(date, time, ResourcesStringProvider(resources))

fun shortDateTimeLabel(
    date: LocalDate,
    time: LocalTime,
    strings: StringProvider,
): String = strings.getString(R.string.date_time_short, date.toShortDateLabel(), time.toTimeLabel())
