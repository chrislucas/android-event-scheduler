package com.br.androideventscheduler.core.preferences.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.br.androideventscheduler.core.preferences.domain.ThemeMode
import com.br.androideventscheduler.core.preferences.domain.ThemePreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Guarda o tema no mesmo `DataStore<Preferences>` do app (arquivo único), na chave `theme_mode`. */
internal class ThemePreferenceRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : ThemePreferenceRepository {
    override fun observe(): Flow<ThemeMode> =
        dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { preferences -> ThemeMode.fromStorageId(preferences[Keys.THEME_MODE]) }
            .distinctUntilChanged()

    override suspend fun set(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[Keys.THEME_MODE] = mode.storageId }
    }

    /** Chave estável: renomear perde a escolha já salva. */
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
