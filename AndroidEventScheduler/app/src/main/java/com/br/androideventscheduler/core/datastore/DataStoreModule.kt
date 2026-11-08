package com.br.androideventscheduler.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.br.androideventscheduler.core.di.DispatcherNames
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Um único `DataStore<Preferences>` por processo (o DataStore proíbe duas instâncias no mesmo arquivo). */
val dataStoreModule =
    module {
        single<DataStore<Preferences>> {
            val ioDispatcher = get<CoroutineDispatcher>(named(DispatcherNames.IO))
            createAppPreferencesDataStore(
                scope = CoroutineScope(SupervisorJob() + ioDispatcher),
                produceFile = { androidContext().preferencesDataStoreFile(APP_PREFERENCES_NAME) },
            )
        }
    }
