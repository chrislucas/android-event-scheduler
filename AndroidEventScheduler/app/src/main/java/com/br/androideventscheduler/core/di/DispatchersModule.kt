package com.br.androideventscheduler.core.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.module

val dispatchersModule =
    module {
        single<CoroutineDispatcher>(named(DispatcherNames.IO)) { Dispatchers.IO }
        single<CoroutineDispatcher>(named(DispatcherNames.DEFAULT)) { Dispatchers.Default }
        single<CoroutineDispatcher>(named(DispatcherNames.MAIN)) { Dispatchers.Main }
    }
