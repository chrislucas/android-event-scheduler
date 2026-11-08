package com.br.androideventscheduler.feature.home.domain.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Monta a grade fixa do calendário mensal: 6 semanas × 7 dias, incluindo dias dos meses vizinhos. */
class MonthGridBuilder(
    private val firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
) {
    /** Sempre [GRID_SIZE] datas consecutivas, começando no [firstDayOfWeek] da semana que contém o dia 1. */
    fun build(month: YearMonth): List<LocalDate> {
        val gridStart = month.atDay(1).with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        return List(GRID_SIZE) { offset -> gridStart.plusDays(offset.toLong()) }
    }

    companion object {
        const val WEEKS = 6
        const val DAYS_PER_WEEK = 7
        const val GRID_SIZE = WEEKS * DAYS_PER_WEEK
    }
}
