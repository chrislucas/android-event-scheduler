package com.br.androideventscheduler.feature.search.testing

import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleOccurrence
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

/** Repositório de teste focado na pesquisa: cada consulta é registrada e respondida por [searchHandler]. */
class FakeScheduleRepository : ScheduleRepository {
    data class SearchCall(
        val query: String,
        val type: ScheduleType?,
    )

    val searchCalls = mutableListOf<SearchCall>()
    var searchHandler: (query: String, type: ScheduleType?) -> Flow<List<ScheduleItem>> = { _, _ -> flowOf(emptyList()) }

    override fun search(
        query: String,
        type: ScheduleType?,
    ): Flow<List<ScheduleItem>> {
        searchCalls += SearchCall(query, type)
        return searchHandler(query, type)
    }

    override fun observeOccurrences(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<ScheduleOccurrence>> = error("not used by search")

    override suspend fun getById(id: String): ScheduleItem? = error("not used by search")

    override fun observeById(id: String): Flow<ScheduleItem?> = error("not used by search")

    override suspend fun getAll(): List<ScheduleItem> = error("not used by search")

    override suspend fun upsert(item: ScheduleItem) = error("not used by search")

    override suspend fun upsertAll(items: List<ScheduleItem>) = error("not used by search")

    override suspend fun delete(id: String) = error("not used by search")
}
