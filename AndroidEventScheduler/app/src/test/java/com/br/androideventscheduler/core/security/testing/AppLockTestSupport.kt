package com.br.androideventscheduler.core.security.testing

import androidx.fragment.app.FragmentActivity
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.testing.FakeUserSettingsRepository
import com.br.androideventscheduler.core.security.data.AppLockController
import com.br.androideventscheduler.core.security.domain.AuthResult
import com.br.androideventscheduler.core.security.domain.DeviceCredentialAuthenticator
import kotlinx.coroutines.CoroutineScope

/** Autenticador fake: devolve [result] sem abrir diálogo do sistema e conta as chamadas. */
class FakeDeviceCredentialAuthenticator(
    var result: AuthResult = AuthResult.Success,
) : DeviceCredentialAuthenticator {
    var calls: Int = 0
        private set

    override suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
    ): AuthResult {
        calls++
        return result
    }
}

/** Cria um [AppLockController] real sobre um repositório fake (relógio fixo, aparelho seguro por padrão). */
fun appLockController(
    scope: CoroutineScope,
    repository: FakeUserSettingsRepository = FakeUserSettingsRepository(UserSettings(appLockEnabled = true)),
    deviceSecure: Boolean = true,
    elapsedRealtime: () -> Long = { 0L },
): AppLockController =
    AppLockController(
        settingsRepository = repository,
        deviceSecurity = { deviceSecure },
        elapsedRealtime = elapsedRealtime,
        applicationScope = scope,
    )
