package com.br.androideventscheduler.feature.detail.presentation.mapper

import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.common.StringProvider
import com.br.androideventscheduler.feature.detail.presentation.state.ScheduleDetailUi

/** Assunto e corpo do texto enviado ao seletor de compartilhamento do sistema. */
internal data class ShareContent(
    val subject: String,
    val body: String,
)

/**
 * [ScheduleDetailUi] (já formatado para a tela de detalhes) → [ShareContent]. Usa só os textos prontos do modelo
 * de UI, então o resumo nunca diverge da tela e não inclui o `id` interno nem o e-mail da conta.
 */
internal class ScheduleShareTextBuilder(
    private val strings: StringProvider,
) {
    fun build(schedule: ScheduleDetailUi): ShareContent {
        val lines =
            buildList {
                add(strings.getString(R.string.share_line_title, schedule.title))
                add(strings.getString(R.string.share_line_date, schedule.dateRangeText))
                add(strings.getString(R.string.share_line_reminder, schedule.reminderText))
                add(strings.getString(R.string.share_line_recurrence, schedule.recurrenceText))
                schedule.location?.let { add(strings.getString(R.string.share_line_location, it)) }
                schedule.notes?.let { add(strings.getString(R.string.share_line_notes, it)) }
            }
        return ShareContent(
            subject = strings.getString(R.string.share_subject, schedule.title),
            body = lines.joinToString(separator = "\n"),
        )
    }
}
