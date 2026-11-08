package com.br.androideventscheduler.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import java.io.File

/** Nome do arquivo do DataStore único do app. */
const val APP_PREFERENCES_NAME = "remember_me_prefs"

/**
 * Cria o DataStore de preferências. Arquivo corrompido é substituído por preferências vazias
 * (o app volta aos padrões em vez de quebrar).
 */
fun createAppPreferencesDataStore(
    scope: CoroutineScope,
    produceFile: () -> File,
): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope,
        produceFile = produceFile,
    )
