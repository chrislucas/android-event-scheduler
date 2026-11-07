package com.br.androideventscheduler.core.schedule.domain.model

/** Calendário de origem de um item. [id] é estável (persistido e exportado); o rótulo é da apresentação. */
enum class CalendarSource(
    val id: String,
    val colorHex: Long,
) {
    PERSONAL_ACCOUNT(id = "personal_account", colorHex = 0xFF29B6F6),
    FAMILY(id = "family_calendar", colorHex = 0xFF7E57C2),
    MY_REMINDERS(id = "my_reminders", colorHex = 0xFFAB47BC),
    ;

    companion object {
        fun defaultFor(type: ScheduleType): CalendarSource =
            when (type) {
                ScheduleType.EVENT -> PERSONAL_ACCOUNT
                ScheduleType.REMINDER -> MY_REMINDERS
            }

        fun fromId(id: String): CalendarSource? = entries.firstOrNull { it.id == id }
    }
}
