package com.br.androideventscheduler.core.security.di

import android.os.Build
import android.os.SystemClock
import com.br.androideventscheduler.core.security.data.AppLockController
import com.br.androideventscheduler.core.security.data.BiometricPromptAuthenticator
import com.br.androideventscheduler.core.security.data.CredentialLauncherRegistry
import com.br.androideventscheduler.core.security.data.KeyguardDeviceSecurityChecker
import com.br.androideventscheduler.core.security.data.KeyguardIntentAuthenticator
import com.br.androideventscheduler.core.security.domain.DeviceCredentialAuthenticator
import com.br.androideventscheduler.core.security.domain.DeviceSecurityChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val APPLICATION_SCOPE = "applicationScope"

val securityModule =
    module {
        single<DeviceSecurityChecker> { KeyguardDeviceSecurityChecker(androidContext()) }
        single { CredentialLauncherRegistry() }
        // Único `if` de versão do app (R5.3): API 30+ usa o BiometricPrompt; API 24–29, o KeyguardManager.
        single<DeviceCredentialAuthenticator> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BiometricPromptAuthenticator()
            } else {
                KeyguardIntentAuthenticator(context = androidContext(), launcherRegistry = get())
            }
        }
        // Escopo do processo (sobrevive às Activities): lê as preferências e acompanha mudanças sem depender de tela.
        single<CoroutineScope>(named(APPLICATION_SCOPE)) { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
        single {
            AppLockController(
                settingsRepository = get(),
                deviceSecurity = get(),
                elapsedRealtime = SystemClock::elapsedRealtime,
                applicationScope = get(named(APPLICATION_SCOPE)),
            )
        }
    }
