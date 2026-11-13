package com.br.androideventscheduler.core.schedule.domain.model

/**
 * Antecedência do alerta em relação ao início. [minutes] `null` = sem alerta.
 *
 * A ordem de declaração é a ordem das opções na UI. O `ordinal` não é persistido (Room e DataStore guardam [minutes],
 * o arquivo de agenda guarda o `name`), então um valor novo pode entrar em qualquer posição.
 */
enum class ReminderOffset(
    val minutes: Int?,
) {
    NONE(minutes = null),
    FIVE_MINUTES(minutes = 5),
    TEN_MINUTES(minutes = 10),
    FIFTEEN_MINUTES(minutes = 15),
    THIRTY_MINUTES(minutes = 30),
    ONE_HOUR(minutes = 60),
    ;

    companion object {
        val DEFAULT = FIFTEEN_MINUTES

        fun fromMinutes(minutes: Int?): ReminderOffset? = entries.firstOrNull { it.minutes == minutes }
    }
}
