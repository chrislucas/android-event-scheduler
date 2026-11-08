package com.br.androideventscheduler.feature.search.di

import com.br.androideventscheduler.core.di.DispatcherNames
import com.br.androideventscheduler.feature.search.presentation.mapper.SearchResultMapper
import com.br.androideventscheduler.feature.search.presentation.viewmodel.SearchViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** API pública da feature: módulo Koin agregado em `app.AppModules`. */
val searchModule =
    module {
        factory { SearchResultMapper(resources = androidContext().resources) }
        viewModel {
            SearchViewModel(
                scheduleRepository = get(),
                userSettingsRepository = get(),
                mapper = get(),
                savedStateHandle = get(),
                dispatcher = get(named(DispatcherNames.MAIN)),
            )
        }
    }
