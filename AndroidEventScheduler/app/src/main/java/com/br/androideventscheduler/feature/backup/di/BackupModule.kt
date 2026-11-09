package com.br.androideventscheduler.feature.backup.di

import com.br.androideventscheduler.core.di.DispatcherNames
import com.br.androideventscheduler.core.navigation.AgendaBackup
import com.br.androideventscheduler.feature.backup.data.codec.AgendaJsonCodec
import com.br.androideventscheduler.feature.backup.data.file.DocumentStreams
import com.br.androideventscheduler.feature.backup.data.file.ExportFileStore
import com.br.androideventscheduler.feature.backup.data.file.appVersionName
import com.br.androideventscheduler.feature.backup.data.repository.AgendaBackupRepositoryImpl
import com.br.androideventscheduler.feature.backup.domain.repository.AgendaBackupRepository
import com.br.androideventscheduler.feature.backup.domain.usecase.ImportAgendaUseCase
import com.br.androideventscheduler.feature.backup.presentation.viewmodel.BackupViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.time.Clock

/** API pública da feature: módulo Koin agregado em `app.AppModules`. */
val backupModule =
    module {
        factory { AgendaJsonCodec() }
        factory { ExportFileStore(context = androidContext()) }
        factory { DocumentStreams(context = androidContext()) }
        single<AgendaBackupRepository> {
            AgendaBackupRepositoryImpl(
                scheduleRepository = get(),
                codec = get(),
                exportFileStore = get(),
                documentStreams = get(),
                clock = get(),
                appVersion = appVersionName(androidContext()),
                ioDispatcher = get(named(DispatcherNames.IO)),
            )
        }
        factory { ImportAgendaUseCase(scheduleRepository = get(), scheduleNextAlarm = get()) }

        // A rota (arquivo recebido, se houver) chega por `parametersOf(AgendaBackup(...))`.
        viewModel { parameters ->
            val route: AgendaBackup = parameters.get()
            BackupViewModel(
                pendingImportUri = route.pendingImportUri,
                backupRepository = get(),
                importAgenda = get(),
                strings = get(),
                clock = get(),
                dispatcher = get(named(DispatcherNames.MAIN)),
            )
        }
    }
