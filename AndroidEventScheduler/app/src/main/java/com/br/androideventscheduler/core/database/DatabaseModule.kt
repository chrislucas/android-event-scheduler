package com.br.androideventscheduler.core.database

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule =
    module {
        single {
            Room
                .databaseBuilder(androidContext(), RememberMeDatabase::class.java, RememberMeDatabase.DATABASE_NAME)
                .build()
        }
        single { get<RememberMeDatabase>().scheduleDao() }
    }
