package com.br.androideventscheduler.feature.search.presentation.mapper

import android.content.res.Resources
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.ui.format.shortDateTimeLabel
import com.br.androideventscheduler.core.ui.format.toDisplayLabel
import com.br.androideventscheduler.core.ui.format.toShortDateLabel
import com.br.androideventscheduler.feature.search.presentation.state.SearchResultUi

/** `ScheduleItem` → [SearchResultUi], com textos de `strings.xml` e de `core.ui.format`. */
internal class SearchResultMapper(
    private val resources: Resources,
) {
    /** [accountEmail] é o rótulo do calendário "conta pessoal". */
    fun map(
        items: List<ScheduleItem>,
        accountEmail: String,
    ): List<SearchResultUi> = items.map { item -> map(item, accountEmail) }

    private fun map(
        item: ScheduleItem,
        accountEmail: String,
    ) = SearchResultUi(
        id = item.id,
        colorHex = item.colorHex,
        stickerEmoji = item.stickerEmoji,
        title = item.title,
        dateTimeText = dateTimeText(item),
        typeText = resources.getString(typeLabelRes(item.type)),
        calendarLabel = calendarLabel(item.calendarSource, accountEmail),
        recurrenceText = item.recurrencePattern.takeIf { it != RecurrencePattern.None }?.toDisplayLabel(resources),
    )

    private fun dateTimeText(item: ScheduleItem): String =
        if (item.isAllDay) {
            resources.getString(R.string.search_result_all_day, item.startDate.toShortDateLabel())
        } else {
            shortDateTimeLabel(item.startDate, item.startTime, resources)
        }

    private fun typeLabelRes(type: ScheduleType): Int =
        when (type) {
            ScheduleType.EVENT -> R.string.search_type_event
            ScheduleType.REMINDER -> R.string.search_type_reminder
        }

    // Mapeamento local; a spec 10 cria um equivalente compartilhado em core.ui.format (trocar depois do merge).
    private fun calendarLabel(
        calendarSource: CalendarSource,
        accountEmail: String,
    ): String =
        when (calendarSource) {
            CalendarSource.PERSONAL_ACCOUNT -> accountEmail
            CalendarSource.FAMILY -> resources.getString(R.string.search_calendar_family)
            CalendarSource.MY_REMINDERS -> resources.getString(R.string.search_calendar_my_reminders)
        }
}
