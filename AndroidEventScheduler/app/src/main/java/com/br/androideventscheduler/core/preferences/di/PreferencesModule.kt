package com.br.androideventscheduler.core.preferences.di

import com.br.androideventscheduler.core.preferences.data.UserSettingsRepositoryImpl
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import org.koin.dsl.module

val preferencesModule =
    module {
        single<UserSettingsRepository> { UserSettingsRepositoryImpl(dataStore = get()) }
    }
