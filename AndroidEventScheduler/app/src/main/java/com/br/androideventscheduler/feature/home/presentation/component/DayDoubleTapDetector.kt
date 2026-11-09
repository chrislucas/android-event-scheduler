package com.br.androideventscheduler.feature.home.presentation.component

/**
 * Regra pura do toque duplo na grade (spec 18). O tempo vem por parâmetro (`uptimeMillis` do evento de soltar),
 * sem relógio dentro da regra. Não é thread-safe; uma instância por grade (`remember`).
 *
 * [K] identifica "onde" o toque caiu. A grade usa a posição da célula (0..41), e não a data, porque o primeiro toque
 * num dia de mês vizinho troca o mês e a mesma posição passa a mostrar outra data (R3.1).
 */
internal class DayDoubleTapDetector<K : Any>(
    private val timeoutMillis: Long,
) {
    private var lastKey: K? = null
    private var lastUpMillis: Long = 0L

    /** Devolve `true` quando [key] completa um toque duplo; depois disso a contagem recomeça. */
    fun onTap(
        key: K,
        upMillis: Long,
    ): Boolean {
        val isDouble = key == lastKey && upMillis - lastUpMillis in 0..timeoutMillis
        if (isDouble) {
            lastKey = null
        } else {
            lastKey = key
            lastUpMillis = upMillis
        }
        return isDouble
    }

    /** Zera a contagem (ex.: troca de mês por deslize). */
    fun reset() {
        lastKey = null
    }
}
