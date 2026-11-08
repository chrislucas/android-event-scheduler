package com.br.androideventscheduler.core.schedule.domain.search

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTextNormalizerTest {
    @Test
    fun `given accented uppercase text when normalizing then accents and case are removed`() {
        assertEquals("reuniao de equipe", SearchTextNormalizer.normalize("REUNIÃO de Equipe"))
    }

    @Test
    fun `given cedilla and mixed diacritics when normalizing then base letters remain`() {
        assertEquals("acao pe joao", SearchTextNormalizer.normalize("Ação Pé João"))
    }

    @Test
    fun `given extra whitespace when normalizing then it is collapsed and trimmed`() {
        assertEquals("cafe com bolo", SearchTextNormalizer.normalize("  café \n com\tbolo  "))
    }

    @Test
    fun `given emoji when normalizing then it is preserved`() {
        assertEquals("🚀 lancamento", SearchTextNormalizer.normalize("🚀 Lançamento"))
    }
}
