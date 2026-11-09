package com.br.androideventscheduler.core.security.testing

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import com.br.androideventscheduler.core.preferences.testing.FakeUserSettingsRepository
import com.br.androideventscheduler.core.security.data.AppLockController
import com.br.androideventscheduler.core.security.domain.AuthResult
import com.br.androideventscheduler.core.security.domain.DeviceCredentialAuthenticator
import com.br.androideventscheduler.core.security.domain.DeviceSecurityChecker
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module

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

/**
 * Troca, no Koin global dos testes Robolectric, o repositório de preferências, o autenticador e o controller por
 * versões controláveis (relógio manual, credencial fake). Chame [install] antes de abrir a `MainActivity` e
 * [uninstall] ao fim do teste (o Koin é recarregado por teste pela `TestRememberMeApplication`).
 */
class AppLockTestHarness(
    initialSettings: UserSettings = UserSettings(appLockEnabled = true),
    authResult: AuthResult = AuthResult.Success,
) {
    val repository = FakeUserSettingsRepository(initialSettings)
    val authenticator = FakeDeviceCredentialAuthenticator(authResult)
    var nowMillis: Long = 0L
    var deviceSecure: Boolean = true

    private val module =
        module {
            single<UserSettingsRepository> { repository }
            single<DeviceCredentialAuthenticator> { authenticator }
            single<DeviceSecurityChecker> { DeviceSecurityChecker { deviceSecure } }
            single {
                AppLockController(
                    settingsRepository = repository,
                    deviceSecurity = get(),
                    elapsedRealtime = { nowMillis },
                    applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
                )
            }
        }

    val controller: AppLockController get() = GlobalContext.get().get()

    fun install() = loadKoinModules(module)

    fun uninstall() = unloadKoinModules(module)

    /** Simula ida ao segundo plano por [millis] e a volta (o que o `ProcessLifecycleOwner` faz em produção). */
    fun backgroundFor(millis: Long) {
        val owner = mockk<LifecycleOwner>(relaxed = true)
        controller.onStop(owner)
        nowMillis += millis
        controller.onStart(owner)
    }
}
