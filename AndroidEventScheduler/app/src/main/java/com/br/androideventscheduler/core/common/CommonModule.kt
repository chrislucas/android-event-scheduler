package com.br.androideventscheduler.core.common

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val commonModule =
    module {
        single<StringProvider> { AndroidStringProvider(androidContext()) }
    }
