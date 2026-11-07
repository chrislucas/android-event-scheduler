package com.br.androideventscheduler.app

import androidx.navigation3.runtime.NavKey
import com.br.androideventscheduler.core.navigation.Home
import com.br.androideventscheduler.core.navigation.Splash

/**
 * Troca a Splash pela Home: adiciona a Home **antes** de remover a Splash (o back stack nunca fica vazio)
 * e garante que voltar na Home saia do app em vez de reabrir a Splash.
 */
internal fun MutableList<NavKey>.replaceSplashWithHome() {
    if (lastOrNull() != Home) add(Home)
    removeAll { it == Splash }
}
