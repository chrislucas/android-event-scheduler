package com.br.androideventscheduler.feature.home.testing

import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleOccurrence
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.domain.recurrence.RecurrenceEngine
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate

/**
 * Repositório de teste da Home: itens em memória expandidos pelo [RecurrenceEngine] real, reemitidos a cada
 * mudança. [failing] faz as consultas de ocorrências falharem; [queries] registra os intervalos pedidos.
 */
class FakeHomeScheduleRepository(
    initialItems: List<ScheduleItem> = emptyList(),
) : ScheduleRepository {
    val items = MutableStateFlow(initialItems)
    val queries = mutableListOf<Pair<LocalDate, LocalDate>>()
    var failing: Boolean = false
    private val recurrenceEngine = RecurrenceEngine()

    override fun observeOccurrences(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<ScheduleOccurrence>> {
        queries += from to to
        return items.map { current ->
            if (failing) throw IOException("database unavailable")
            current.flatMap { item -> recurrenceEngine.expand(item, from, to) }
        }
    }

    override fun search(
        query: String,
        type: ScheduleType?,
    ): Flow<List<ScheduleItem>> = error("not used by home")

    override suspend fun getById(id: String): ScheduleItem? = error("not used by home")

    override fun observeById(id: String): Flow<ScheduleItem?> = error("not used by home")

    override suspend fun getAll(): List<ScheduleItem> = error("not used by home")

    override suspend fun upsert(item: ScheduleItem) = error("not used by home")

    override suspend fun upsertAll(items: List<ScheduleItem>) = error("not used by home")

    override suspend fun delete(id: String) = error("not used by home")
}
