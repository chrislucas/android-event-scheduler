package com.br.androideventscheduler.core.schedule.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    /** Itens sem recorrência que intersectam `[fromEpochDay, toEpochDay]` + recorrentes iniciados até `toEpochDay`. */
    @Query(
        """
        SELECT * FROM schedules
        WHERE startDateEpochDay <= :toEpochDay
          AND (recurrenceType != '$RECURRENCE_NONE' OR endDateEpochDay >= :fromEpochDay)
        ORDER BY startDateEpochDay, isAllDay DESC, startMinuteOfDay
        """,
    )
    fun observeInRange(
        fromEpochDay: Long,
        toEpochDay: Long,
    ): Flow<List<ScheduleEntity>>

    /** [escapedPattern] já normalizado e com `%`, `_` e `\` escapados (ver repositório). */
    @Query(
        """
        SELECT * FROM schedules
        WHERE searchText LIKE '%' || :escapedPattern || '%' ESCAPE '\'
          AND (:type IS NULL OR type = :type)
        ORDER BY startDateEpochDay, isAllDay DESC, startMinuteOfDay
        """,
    )
    fun search(
        escapedPattern: String,
        type: String?,
    ): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: String): ScheduleEntity?

    @Query("SELECT * FROM schedules WHERE id = :id")
    fun observeById(id: String): Flow<ScheduleEntity?>

    @Query("SELECT * FROM schedules ORDER BY startDateEpochDay, isAllDay DESC, startMinuteOfDay")
    suspend fun getAll(): List<ScheduleEntity>

    @Upsert
    suspend fun upsert(entity: ScheduleEntity)

    /** Grava todos numa única transação (o Room envolve o upsert em lote em uma): ou entram todos, ou nenhum. */
    @Upsert
    suspend fun upsertAll(entities: List<ScheduleEntity>)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteById(id: String)

    companion object {
        const val RECURRENCE_NONE = "NONE"
    }
}
