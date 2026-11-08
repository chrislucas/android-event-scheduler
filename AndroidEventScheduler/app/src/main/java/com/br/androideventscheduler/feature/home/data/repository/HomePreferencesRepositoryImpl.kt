package com.br.androideventscheduler.feature.home.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.feature.home.domain.repository.HomePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

internal class HomePreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : HomePreferencesRepository {
    override fun observeEnabledCalendars(): Flow<Set<CalendarSource>> =
        dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { preferences -> preferences.enabledCalendars() }
            .distinctUntilChanged()

    override suspend fun setCalendarEnabled(
        source: CalendarSource,
        enabled: Boolean,
    ) {
        dataStore.edit { preferences ->
            val current = preferences.enabledCalendars()
            val updated = if (enabled) current + source else current - source
            preferences[Keys.ENABLED_CALENDAR_IDS] = updated.map { it.id }.toSet()
        }
    }

    override fun observeDaySticker(epochDay: Long): Flow<String?> =
        dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { preferences -> preferences[Keys.daySticker(epochDay)]?.takeIf { it.isNotBlank() } }
            .distinctUntilChanged()

    override suspend fun setDaySticker(
        epochDay: Long,
        emoji: String?,
    ) {
        dataStore.edit { preferences ->
            if (emoji.isNullOrBlank()) {
                preferences.remove(Keys.daySticker(epochDay))
            } else {
                preferences[Keys.daySticker(epochDay)] = emoji
            }
        }
    }

    /** Ausente = todos visíveis; ids desconhecidos (removidos numa versão futura) são ignorados. */
    private fun Preferences.enabledCalendars(): Set<CalendarSource> =
        this[Keys.ENABLED_CALENDAR_IDS]
            ?.mapNotNull { id -> CalendarSource.fromId(id) }
            ?.toSet()
            ?: CalendarSource.entries.toSet()

    /** Chaves estáveis: renomear quebra as preferências já salvas. */
    private object Keys {
        val ENABLED_CALENDAR_IDS = stringSetPreferencesKey("enabled_calendar_ids")

        /** Uma chave por dia (`day_sticker_<epochDay>`): volume baixo e escrita sem reler o mapa inteiro. */
        fun daySticker(epochDay: Long) = stringPreferencesKey("day_sticker_$epochDay")
    }
}
