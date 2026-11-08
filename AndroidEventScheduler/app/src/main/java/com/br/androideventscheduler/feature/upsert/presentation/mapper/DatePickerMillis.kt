package com.br.androideventscheduler.feature.upsert.presentation.mapper

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * O `DatePickerState` do Material 3 guarda a data como **milissegundos à meia-noite UTC** (não no fuso do
 * aparelho). Converter sempre por UTC: usar o fuso local deslocaria a data em um dia em fusos negativos
 * (ex.: São Paulo, UTC-3, mostraria o dia anterior).
 */
internal fun LocalDate.toDatePickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun Long.toLocalDateFromDatePickerMillis(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
