package com.br.androideventscheduler.core.preferences.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.security.domain.AppLockTimeout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

internal class UserSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : UserSettingsRepository {
    override fun observe(): Flow<UserSettings> =
        dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { preferences -> preferences.toUserSettings() }
            .distinctUntilChanged()

    override suspend fun save(settings: UserSettings) {
        dataStore.edit { preferences ->
            preferences[Keys.ACTIVE_ACCOUNT_EMAIL] = settings.activeAccountEmail.trim()
            preferences[Keys.DEFAULT_REMINDER_MINUTES] = settings.defaultReminderOffset.minutes ?: NO_REMINDER_MINUTES
            preferences[Keys.DEFAULT_SNOOZE_MINUTES] = settings.defaultSnoozeMinutes
            preferences[Keys.FULL_SCREEN_EVENT_ALERT] = settings.fullScreenEventAlertEnabled
            preferences[Keys.APP_LOCK_ENABLED] = settings.appLockEnabled
            preferences[Keys.APP_LOCK_TIMEOUT] = settings.appLockTimeout.name
        }
    }

    private fun Preferences.toUserSettings(): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            activeAccountEmail = this[Keys.ACTIVE_ACCOUNT_EMAIL] ?: defaults.activeAccountEmail,
            defaultReminderOffset = this[Keys.DEFAULT_REMINDER_MINUTES]?.toReminderOffset() ?: defaults.defaultReminderOffset,
            defaultSnoozeMinutes =
                this[Keys.DEFAULT_SNOOZE_MINUTES]?.takeIf { it in UserSettings.ALLOWED_SNOOZE_MINUTES }
                    ?: defaults.defaultSnoozeMinutes,
            fullScreenEventAlertEnabled = this[Keys.FULL_SCREEN_EVENT_ALERT] ?: defaults.fullScreenEventAlertEnabled,
            appLockEnabled = this[Keys.APP_LOCK_ENABLED] ?: defaults.appLockEnabled,
            appLockTimeout =
                AppLockTimeout.entries.firstOrNull { it.name == this[Keys.APP_LOCK_TIMEOUT] } ?: defaults.appLockTimeout,
        )
    }

    private fun Int.toReminderOffset(): ReminderOffset? =
        if (this == NO_REMINDER_MINUTES) ReminderOffset.NONE else ReminderOffset.fromMinutes(this)

    /** Chaves estáveis: renomear quebra as preferências já salvas. */
    private object Keys {
        val ACTIVE_ACCOUNT_EMAIL = stringPreferencesKey("active_account_email")
        val DEFAULT_REMINDER_MINUTES = intPreferencesKey("default_reminder_minutes")
        val DEFAULT_SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
        val FULL_SCREEN_EVENT_ALERT = booleanPreferencesKey("full_screen_event_alert")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_TIMEOUT = stringPreferencesKey("app_lock_timeout")
    }

    private companion object {
        /** Persistência de [ReminderOffset.NONE] (o DataStore não guarda `null`). */
        const val NO_REMINDER_MINUTES = -1
    }
}
