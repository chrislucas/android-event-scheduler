package com.br.androideventscheduler.core.schedule.domain.model

import java.time.DayOfWeek

/** Regra de repetição de um item. O limite conta ocorrências desde a primeira da série. */
sealed interface RecurrencePattern {
    val occurrenceLimit: Int?

    data object None : RecurrencePattern {
        override val occurrenceLimit: Int? = null
    }

    data class Daily(
        override val occurrenceLimit: Int? = null,
    ) : RecurrencePattern {
        init {
            requireValidLimit(occurrenceLimit)
        }
    }

    data class Weekly(
        val daysOfWeek: Set<DayOfWeek>,
        override val occurrenceLimit: Int? = null,
    ) : RecurrencePattern {
        init {
            require(daysOfWeek.isNotEmpty()) { "Weekly recurrence needs at least one day of week" }
            requireValidLimit(occurrenceLimit)
        }
    }
}

private fun requireValidLimit(occurrenceLimit: Int?) {
    require(occurrenceLimit == null || occurrenceLimit >= 1) { "occurrenceLimit must be >= 1, was $occurrenceLimit" }
}
