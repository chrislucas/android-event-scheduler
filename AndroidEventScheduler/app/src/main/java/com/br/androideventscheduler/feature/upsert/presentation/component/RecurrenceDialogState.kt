package com.br.androideventscheduler.feature.upsert.presentation.component

import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import java.time.DayOfWeek
import java.time.LocalDate

/** Opções exclusivas do diálogo de repetição. */
internal enum class RecurrenceKind { NONE, DAILY, WEEKLY }

/**
 * Rascunho local do diálogo de repetição: nada chega à tela de criar/editar até o usuário confirmar.
 * Puro (sem Android/Compose) para ser testado na JVM; o diálogo só o consome.
 */
internal data class RecurrenceDraft(
    val kind: RecurrenceKind,
    val selectedDays: Set<DayOfWeek>,
    val countText: String,
) {
    fun withKind(kind: RecurrenceKind): RecurrenceDraft = copy(kind = kind)

    /** Alterna o dia; o último dia selecionado não pode ser desmarcado. */
    fun toggleDay(day: DayOfWeek): RecurrenceDraft {
        val updatedDays =
            when {
                day !in selectedDays -> selectedDays + day
                selectedDays.size > 1 -> selectedDays - day
                else -> selectedDays
            }
        return copy(selectedDays = updatedDays)
    }

    /** Aceita apenas dígitos, até [MAX_COUNT_DIGITS]. */
    fun withCountText(raw: String): RecurrenceDraft = copy(countText = raw.filter(Char::isDigit).take(MAX_COUNT_DIGITS))

    /** Limite vazio ou zero significa "sem limite". */
    fun toPattern(): RecurrencePattern {
        val occurrenceLimit = countText.toIntOrNull()?.takeIf { it > 0 }
        return when (kind) {
            RecurrenceKind.NONE -> RecurrencePattern.None
            RecurrenceKind.DAILY -> RecurrencePattern.Daily(occurrenceLimit)
            RecurrenceKind.WEEKLY -> RecurrencePattern.Weekly(selectedDays, occurrenceLimit)
        }
    }

    companion object {
        const val MAX_COUNT_DIGITS = 4

        /** Sem dias no padrão (None/Daily), o dia da semana de [startDate] já vem marcado para o caso de virar semanal. */
        fun from(
            pattern: RecurrencePattern,
            startDate: LocalDate,
        ): RecurrenceDraft {
            val countText = pattern.occurrenceLimit?.toString().orEmpty()
            return when (pattern) {
                RecurrencePattern.None -> RecurrenceDraft(RecurrenceKind.NONE, setOf(startDate.dayOfWeek), countText)
                is RecurrencePattern.Daily -> RecurrenceDraft(RecurrenceKind.DAILY, setOf(startDate.dayOfWeek), countText)
                is RecurrencePattern.Weekly -> RecurrenceDraft(RecurrenceKind.WEEKLY, pattern.daysOfWeek, countText)
            }
        }
    }
}
