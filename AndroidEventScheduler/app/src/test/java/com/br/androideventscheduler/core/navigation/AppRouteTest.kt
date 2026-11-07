package com.br.androideventscheduler.core.navigation

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AppRouteTest {
    @Test
    fun `given detail route when serializing then it round trips`() {
        val route = ScheduleDetail(scheduleId = "abc-123")

        val decoded = Json.decodeFromString(ScheduleDetail.serializer(), Json.encodeToString(ScheduleDetail.serializer(), route))

        assertEquals(route, decoded)
    }

    @Test
    fun `given upsert route with optional args when serializing then nulls are preserved`() {
        val create = UpsertSchedule(initialDateEpochDay = 20_000L)
        val edit = UpsertSchedule(scheduleId = "abc")

        val decodedCreate = Json.decodeFromString(UpsertSchedule.serializer(), Json.encodeToString(UpsertSchedule.serializer(), create))
        val decodedEdit = Json.decodeFromString(UpsertSchedule.serializer(), Json.encodeToString(UpsertSchedule.serializer(), edit))

        assertEquals(create, decodedCreate)
        assertEquals(edit, decodedEdit)
    }
}
