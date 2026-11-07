package com.br.androideventscheduler.feature.splash.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.br.androideventscheduler.core.navigation.Splash
import com.br.androideventscheduler.feature.splash.presentation.screen.SplashScreen

/** API pública da feature: registra a rota [Splash] no `entryProvider` do host. */
fun EntryProviderScope<NavKey>.splashEntry(onNavigateToHome: () -> Unit) {
    entry<Splash> { SplashScreen(onNavigateToHome = onNavigateToHome) }
}
