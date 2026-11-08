package com.br.androideventscheduler.feature.backup.data.mapper

import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.feature.backup.data.codec.AgendaFileFormat
import com.br.androideventscheduler.feature.backup.data.dto.RecurrenceDto
import com.br.androideventscheduler.feature.backup.data.dto.ScheduleItemDto
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val COLOR_WITH_ALPHA_LENGTH = 8
private const val COLOR_WITHOUT_ALPHA_LENGTH = 6
private const val HEX_RADIX = 16
private const val OPAQUE_ALPHA = 0xFF000000L
private const val ARGB_MASK = 0xFFFFFFFFL
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

/**
 * Conversão entre [ScheduleItemDto] (arquivo) e [ScheduleItem] (domínio). A leitura trata o arquivo como não
 * confiável: qualquer valor inválido devolve `null` (o item é ignorado e contado), nunca lança.
 */
internal object AgendaDtoMapper {
    fun toDto(item: ScheduleItem): ScheduleItemDto =
        ScheduleItemDto(
            id = item.id,
            title = item.title,
            type = item.type.name,
            startDate = item.startDate.toString(),
            endDate = item.endDate.toString(),
            startTime = item.startTime.format(timeFormatter),
            endTime = item.endTime?.format(timeFormatter),
            isAllDay = item.isAllDay,
            location = item.location,
            notes = item.notes,
            colorHex = "#%08X".format(Locale.ROOT, item.colorHex and ARGB_MASK),
            calendarSource = item.calendarSource.id,
            reminderOffset = item.reminderOffset.name,
            recurrence = item.recurrencePattern.toDto(),
            stickerEmoji = item.stickerEmoji,
        )

    /** `null` quando qualquer campo é inválido. Strings longas são **truncadas** (não invalidam o item). */
    fun toDomainOrNull(dto: ScheduleItemDto): ScheduleItem? = runCatching { dto.toDomain() }.getOrNull()

    /** Lança [IllegalArgumentException] (ou erro de parse de data/hora) em qualquer valor inválido; o chamador converte em `null`. */
    private fun ScheduleItemDto.toDomain(): ScheduleItem {
        val scheduleType = enumByName<ScheduleType>(type)
        val start = parseDate(startDate)
        val item =
            ScheduleItem(
                id = id.also { require(it.isNotBlank() && it.length <= AgendaFileFormat.MAX_ID_LENGTH) { "invalid id" } },
                title = title.trim().also { require(it.isNotEmpty()) { "blank title" } }.truncate(AgendaFileFormat.MAX_TITLE_LENGTH),
                type = scheduleType,
                startDate = start,
                endDate = endDate?.let(::parseDate) ?: start,
                startTime = parseTime(startTime),
                endTime = endTime?.let(::parseTime),
                isAllDay = isAllDay,
                location = location.truncate(AgendaFileFormat.MAX_LOCATION_LENGTH),
                notes = notes.truncate(AgendaFileFormat.MAX_NOTES_LENGTH),
                calendarSource = calendarSource?.let(::calendarSourceById) ?: CalendarSource.defaultFor(scheduleType),
                reminderOffset = reminderOffset?.let { enumByName<ReminderOffset>(it) } ?: ReminderOffset.DEFAULT,
                recurrencePattern = recurrence.toPattern(),
                stickerEmoji = stickerEmoji?.takeIf { it.isNotBlank() && it.length <= AgendaFileFormat.MAX_STICKER_LENGTH },
            )
        return colorHex?.let { item.copy(colorHex = parseColor(it)) } ?: item
    }

    private fun RecurrencePattern.toDto(): RecurrenceDto =
        when (this) {
            RecurrencePattern.None -> {
                RecurrenceDto(type = RecurrenceDto.RECURRENCE_NONE)
            }

            is RecurrencePattern.Daily -> {
                RecurrenceDto(type = RecurrenceDto.RECURRENCE_DAILY, occurrenceLimit = occurrenceLimit)
            }

            is RecurrencePattern.Weekly -> {
                RecurrenceDto(
                    type = RecurrenceDto.RECURRENCE_WEEKLY,
                    daysOfWeek = daysOfWeek.sorted().map { it.name },
                    occurrenceLimit = occurrenceLimit,
                )
            }
        }

    /** Construtores do domínio lançam em dados inválidos (Weekly vazio, limite < 1). */
    private fun RecurrenceDto.toPattern(): RecurrencePattern =
        when (type) {
            RecurrenceDto.RECURRENCE_NONE -> {
                RecurrencePattern.None
            }

            RecurrenceDto.RECURRENCE_DAILY -> {
                RecurrencePattern.Daily(occurrenceLimit)
            }

            RecurrenceDto.RECURRENCE_WEEKLY -> {
                RecurrencePattern.Weekly(
                    daysOfWeek.map { enumByName<DayOfWeek>(it) }.toSet(),
                    occurrenceLimit,
                )
            }

            else -> {
                throw IllegalArgumentException("unknown recurrence type")
            }
        }

    private inline fun <reified E : Enum<E>> enumByName(name: String): E =
        requireNotNull(enumValues<E>().firstOrNull { it.name == name }) { "unknown ${E::class.simpleName}" }

    private fun calendarSourceById(id: String): CalendarSource = requireNotNull(CalendarSource.fromId(id)) { "unknown calendar source" }

    private fun parseDate(text: String): LocalDate =
        LocalDate.parse(text).also { require(it.year in AgendaFileFormat.MIN_YEAR..AgendaFileFormat.MAX_YEAR) { "year out of range" } }

    private fun parseTime(text: String): LocalTime = LocalTime.parse(text, timeFormatter)

    /** `#AARRGGBB` ou `#RRGGBB` (opaco); qualquer outra forma é inválida. */
    private fun parseColor(text: String): Long {
        val digits = text.removePrefix("#")
        require(digits.all { Character.digit(it, HEX_RADIX) >= 0 }) { "invalid color" }
        return when (digits.length) {
            COLOR_WITH_ALPHA_LENGTH -> digits.toLong(HEX_RADIX)
            COLOR_WITHOUT_ALPHA_LENGTH -> OPAQUE_ALPHA or digits.toLong(HEX_RADIX)
            else -> throw IllegalArgumentException("invalid color length")
        }
    }

    /** Corta em [maxLength] sem partir um par substituto (emoji). */
    private fun String.truncate(maxLength: Int): String {
        if (length <= maxLength) return this
        val cut = if (this[maxLength - 1].isHighSurrogate()) maxLength - 1 else maxLength
        return substring(0, cut)
    }
}
