package com.br.androideventscheduler.feature.home.presentation.mapper

import com.br.androideventscheduler.core.ui.format.AppLocale
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val monthTitleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", AppLocale)
private val dayHeaderFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d EEE", AppLocale)
private val dayOfMonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM", AppLocale)

/** Título da barra superior: `novembro 2026`. */
internal fun YearMonth.toMonthTitle(): String = format(monthTitleFormatter)

/** Cabeçalho do dia selecionado: `5 QUI.` */
internal fun LocalDate.toDayHeader(): String = format(dayHeaderFormatter).uppercase(AppLocale)

/** Data falada pelo leitor de tela: `5 de novembro`. */
internal fun LocalDate.toSpokenDate(): String = format(dayOfMonthFormatter)

/** Mês abreviado do seletor: `JAN.` */
internal fun Month.toPickerLabel(): String = getDisplayName(TextStyle.SHORT, AppLocale).uppercase(AppLocale)

/** Nome completo do mês para o leitor de tela: `janeiro`. */
internal fun Month.toFullName(): String = getDisplayName(TextStyle.FULL, AppLocale)
