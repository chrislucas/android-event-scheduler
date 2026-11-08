package com.br.androideventscheduler.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.br.androideventscheduler.core.schedule.data.local.ScheduleDao
import com.br.androideventscheduler.core.schedule.data.local.ScheduleEntity

/**
 * Banco local do app. Schema exportado em `app/schemas` e versionado: toda mudança de schema exige
 * incrementar [version] e uma `Migration` (sem migração destrutiva).
 */
@Database(
    entities = [ScheduleEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class RememberMeDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        const val DATABASE_NAME = "remember_me.db"
    }
}
