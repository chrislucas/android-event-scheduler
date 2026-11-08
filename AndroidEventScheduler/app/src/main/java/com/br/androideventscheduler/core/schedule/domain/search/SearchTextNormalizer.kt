package com.br.androideventscheduler.core.schedule.domain.search

import java.text.Normalizer
import java.util.Locale

/**
 * Normaliza texto para busca sem diferenciar maiúsculas nem acentos (`REUNIÃO` → `reuniao`).
 * O SQLite só ignora caixa em ASCII, então a coluna `searchText` guarda o texto já normalizado.
 */
object SearchTextNormalizer {
    private val combiningMarks = Regex("\\p{Mn}+")
    private val whitespaceRuns = Regex("\\s+")

    fun normalize(text: String): String =
        Normalizer
            .normalize(text, Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)
            .replace(whitespaceRuns, " ")
            .trim()
}
