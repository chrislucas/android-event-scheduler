package com.br.androideventscheduler.core.schedule.data.repository

import com.br.androideventscheduler.core.schedule.data.local.ScheduleDao
import com.br.androideventscheduler.core.schedule.data.mapper.toDomain
import com.br.androideventscheduler.core.schedule.data.mapper.toEntity
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleOccurrence
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.domain.recurrence.RecurrenceEngine
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.domain.search.SearchTextNormalizer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

internal class ScheduleRepositoryImpl(
    private val scheduleDao: ScheduleDao,
    private val recurrenceEngine: RecurrenceEngine,
    private val ioDispatcher: CoroutineDispatcher,
) : ScheduleRepository {
    override fun observeOccurrences(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<ScheduleOccurrence>> =
        scheduleDao
            .observeInRange(from.toEpochDay(), to.toEpochDay())
            .map { entities ->
                entities
                    .map { it.toDomain() }
                    .flatMap { item -> recurrenceEngine.expand(item, from, to) }
                    .sortedWith(occurrenceChronologicalOrder)
            }.flowOn(ioDispatcher)

    override fun search(
        query: String,
        type: ScheduleType?,
    ): Flow<List<ScheduleItem>> {
        val normalizedQuery = SearchTextNormalizer.normalize(query)
        if (normalizedQuery.isEmpty()) return flowOf(emptyList())
        return scheduleDao
            .search(escapeLikePattern(normalizedQuery), type?.name)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun getById(id: String): ScheduleItem? = withContext(ioDispatcher) { scheduleDao.getById(id)?.toDomain() }

    override fun observeById(id: String): Flow<ScheduleItem?> = scheduleDao.observeById(id).map { it?.toDomain() }.flowOn(ioDispatcher)

    override suspend fun getAll(): List<ScheduleItem> = withContext(ioDispatcher) { scheduleDao.getAll().map { it.toDomain() } }

    override suspend fun upsert(item: ScheduleItem) = withContext(ioDispatcher) { scheduleDao.upsert(item.toEntity()) }

    override suspend fun delete(id: String) = withContext(ioDispatcher) { scheduleDao.deleteById(id) }

    private companion object {
        /** Dia; no mesmo dia, "dia inteiro" primeiro e depois por horário. */
        val occurrenceChronologicalOrder: Comparator<ScheduleOccurrence> =
            compareBy<ScheduleOccurrence> { it.startDate }
                .thenBy { !it.item.isAllDay }
                .thenBy { it.item.startTime }
                .thenBy { it.item.id }

        /** Escapa curingas do LIKE para que `%`, `_` e `\` digitados sejam procurados literalmente. */
        fun escapeLikePattern(text: String): String =
            text
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
    }
}
