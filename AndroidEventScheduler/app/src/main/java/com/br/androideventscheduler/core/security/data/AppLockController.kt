package com.br.androideventscheduler.core.security.data

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import com.br.androideventscheduler.core.security.domain.AppLockPolicy
import com.br.androideventscheduler.core.security.domain.AppLockState
import com.br.androideventscheduler.core.security.domain.DeviceSecurityChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Estado do bloqueio do app, **só em memória**: matar o processo sempre volta bloqueado (R6.2). É controle de acesso à
 * interface, não criptografia dos dados. Observa o ciclo de vida do **processo** (`ProcessLifecycleOwner`), que ignora
 * rotação e troca de tema porque atrasa o `onStop` (R2.3), e usa relógio monotônico injetado (R2.4).
 */
class AppLockController(
    private val settingsRepository: UserSettingsRepository,
    private val deviceSecurity: DeviceSecurityChecker,
    private val elapsedRealtime: () -> Long,
    applicationScope: CoroutineScope,
) : DefaultLifecycleObserver {
    private val _lockState = MutableStateFlow(AppLockState.Pending)
    val lockState: StateFlow<AppLockState> = _lockState.asStateFlow()

    private val lockDisabledEvents = Channel<Unit>(Channel.BUFFERED)

    /** Evento one-shot: a proteção foi desligada porque o aparelho ficou sem bloqueio de tela (R3.6). */
    val lockDisabledBecauseDeviceInsecure: Flow<Unit> = lockDisabledEvents.receiveAsFlow()

    private var latestSettings: UserSettings? = null
    private var backgroundedAtMillis: Long? = null
    private var suppressedAtMillis: Long? = null

    init {
        applicationScope.launch {
            settingsRepository.observe().collect { settings -> onSettings(settings) }
        }
    }

    /** Desbloqueia após a confirmação da credencial. */
    fun unlock() {
        _lockState.value = AppLockState.Unlocked
    }

    /**
     * Antes de abrir uma UI externa (share sheet, seletor de arquivo, confirmação de credencial): a ida ao segundo plano
     * seguinte não bloqueia. Vale para **uma** volta e expira em [SUPPRESSION_TTL_MILLIS].
     */
    fun suppressNextLock() {
        suppressedAtMillis = elapsedRealtime()
    }

    override fun onStop(owner: LifecycleOwner) {
        backgroundedAtMillis = elapsedRealtime()
    }

    override fun onStart(owner: LifecycleOwner) {
        val now = elapsedRealtime()
        val suppressed = suppressedAtMillis?.let { now - it <= SUPPRESSION_TTL_MILLIS } == true
        val settings = latestSettings
        val shouldLock =
            settings != null &&
                _lockState.value == AppLockState.Unlocked &&
                AppLockPolicy.shouldLock(settings.appLockEnabled, backgroundedAtMillis, now, settings.appLockTimeout)
        if (shouldLock && !suppressed) _lockState.value = AppLockState.Locked
        // A supressão vale para uma volta só, tenha sido usada ou expirada.
        suppressedAtMillis = null
        backgroundedAtMillis = null
    }

    private suspend fun onSettings(settings: UserSettings) {
        val isFirstRead = latestSettings == null
        latestSettings = settings
        when {
            !settings.appLockEnabled -> _lockState.value = AppLockState.Unlocked
            isFirstRead && !deviceSecurity.isDeviceSecure() -> disableBecauseDeviceInsecure(settings)
            isFirstRead -> _lockState.value = AppLockState.Locked
        }
    }

    /** Sem bloqueio de tela não há como autenticar: desliga a proteção para não trancar o usuário fora dos próprios dados. */
    private suspend fun disableBecauseDeviceInsecure(settings: UserSettings) {
        _lockState.value = AppLockState.Unlocked
        settingsRepository.save(settings.copy(appLockEnabled = false))
        lockDisabledEvents.send(Unit)
    }

    private companion object {
        const val SUPPRESSION_TTL_MILLIS = 120_000L
    }
}
