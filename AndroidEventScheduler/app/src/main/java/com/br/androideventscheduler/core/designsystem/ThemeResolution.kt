package com.br.androideventscheduler.core.designsystem

import com.br.androideventscheduler.core.preferences.domain.ThemeMode

/** Resolve a escolha do usuário para claro/escuro. Regra pura, sem Compose. */
fun ThemeMode.isDark(systemInDarkTheme: Boolean): Boolean =
    when (this) {
        ThemeMode.SYSTEM -> systemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
