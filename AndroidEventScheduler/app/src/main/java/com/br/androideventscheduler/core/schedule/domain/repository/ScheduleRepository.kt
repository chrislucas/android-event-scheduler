package com.br.androideventscheduler.core.schedule.domain.repository

import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleOccurrence
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Acesso aos agendamentos. Funções `suspend` lançam em falha de I/O; quem chama usa `runCatching`.
 * Os `Flow`s reemitem sempre que a tabela muda.
 */
interface ScheduleRepository {
    /** Ocorrências (recorrência já expandida) que intersectam `[from, to]`, ordenadas por data e hora. */
    fun observeOccurrences(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<ScheduleOccurrence>>

    /** Busca por título, notas e local, sem diferenciar caixa nem acentos. Consulta em branco → lista vazia. */
    fun search(
        query: String,
        type: ScheduleType? = null,
    ): Flow<List<ScheduleItem>>

    suspend fun getById(id: String): ScheduleItem?

    /** Emite `null` se o item não existe ou foi removido. */
    fun observeById(id: String): Flow<ScheduleItem?>

    suspend fun getAll(): List<ScheduleItem>

    suspend fun upsert(item: ScheduleItem)

    suspend fun delete(id: String)
}
