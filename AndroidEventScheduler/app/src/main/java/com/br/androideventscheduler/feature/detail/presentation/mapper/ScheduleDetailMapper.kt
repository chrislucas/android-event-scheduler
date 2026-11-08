package com.br.androideventscheduler.feature.detail.presentation.mapper

import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.common.StringProvider
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.ui.format.label
import com.br.androideventscheduler.core.ui.format.shortDateTimeLabel
import com.br.androideventscheduler.core.ui.format.toDisplayLabel
import com.br.androideventscheduler.core.ui.format.toShortDateLabel
import com.br.androideventscheduler.core.ui.format.toTimeLabel
import com.br.androideventscheduler.feature.detail.presentation.state.ScheduleDetailUi
import java.time.LocalTime

/** `ScheduleItem` → [ScheduleDetailUi] com todos os textos prontos (sem Android além de resources via [StringProvider]). */
internal class ScheduleDetailMapper(
    private val strings: StringProvider,
) {
    fun map(
        item: ScheduleItem,
        accountEmail: String,
    ): ScheduleDetailUi =
        ScheduleDetailUi(
            id = item.id,
            title = item.title,
            stickerEmoji = item.stickerEmoji,
            colorHex = item.colorHex,
            dateRangeText = item.toDateRangeText(),
            calendarLabel = item.calendarSource.label(personalAccountLabel = accountEmail, strings = strings),
            calendarColorHex = item.calendarSource.colorHex,
            reminderText = item.reminderOffset.toDisplayLabel(strings),
            recurrenceText = item.recurrencePattern.toDisplayLabel(strings),
            location = item.location.takeIf { it.isNotBlank() },
            notes = item.notes.takeIf { it.isNotBlank() },
        )

    /** Para itens recorrentes mostra a data inicial da série (R2.4): [ScheduleItem.startDate]. */
    private fun ScheduleItem.toDateRangeText(): String =
        when {
            type == ScheduleType.REMINDER -> shortDateTimeLabel(startDate, startTime, strings)
            isAllDay -> allDayText()
            endTime == null -> shortDateTimeLabel(startDate, startTime, strings)
            startDate == endDate -> sameDayText(endTime)
            else -> multiDayText(endTime)
        }

    private fun ScheduleItem.allDayText(): String {
        val dateText =
            if (startDate == endDate) {
                startDate.toShortDateLabel()
            } else {
                strings.getString(R.string.detail_range_arrow, startDate.toShortDateLabel(), endDate.toShortDateLabel())
            }
        return strings.getString(R.string.detail_range_all_day, dateText)
    }

    private fun ScheduleItem.sameDayText(endTime: LocalTime): String =
        strings.getString(
            R.string.detail_range_same_day,
            shortDateTimeLabel(startDate, startTime, strings),
            endTime.toTimeLabel(),
        )

    private fun ScheduleItem.multiDayText(endTime: LocalTime): String =
        strings.getString(
            R.string.detail_range_arrow,
            shortDateTimeLabel(startDate, startTime, strings),
            shortDateTimeLabel(endDate, endTime, strings),
        )
}
