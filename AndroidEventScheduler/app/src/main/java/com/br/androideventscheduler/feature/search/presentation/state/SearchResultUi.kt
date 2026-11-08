package com.br.androideventscheduler.feature.search.presentation.state

/** Resultado já formatado para exibição (textos montados por `SearchResultMapper`). */
internal data class SearchResultUi(
    val id: String,
    val colorHex: Long,
    val stickerEmoji: String?,
    val title: String,
    val dateTimeText: String,
    val typeText: String,
    val calendarLabel: String,
    val recurrenceText: String?,
)
