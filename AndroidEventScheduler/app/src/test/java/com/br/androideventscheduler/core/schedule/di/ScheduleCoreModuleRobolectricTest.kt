package com.br.androideventscheduler.core.schedule.di

import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner

/** Prova a ligação real do grafo de produção: Koin → Room → repositório. */
@RunWith(RobolectricTestRunner::class)
class ScheduleCoreModuleRobolectricTest {
    @Test
    fun `given app koin graph when resolving schedule repository then it persists and reads items`() =
        runTest {
            val repository = GlobalContext.get().get<ScheduleRepository>()
            val item = scheduleItem(id = "koin-wired")

            repository.upsert(item)

            assertEquals(item, repository.getById("koin-wired"))
            repository.delete("koin-wired")
        }
}
