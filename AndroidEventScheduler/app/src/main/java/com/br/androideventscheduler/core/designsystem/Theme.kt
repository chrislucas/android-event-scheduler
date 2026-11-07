package com.br.androideventscheduler.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RememberMeColorScheme =
    darkColorScheme(
        primary = AccentCyan,
        onPrimary = OnAccentBlack,
        secondary = AccentCyan,
        onSecondary = OnAccentBlack,
        background = BackgroundBlack,
        onBackground = TextPrimary,
        surface = SurfaceDark,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceElevatedDark,
        onSurfaceVariant = TextSecondary,
        surfaceContainer = SurfaceDark,
        surfaceContainerHigh = SurfaceElevatedDark,
        error = ErrorRed,
        outline = OutlineDark,
    )

// Sem preview: wrapper de tema, sem UI própria.

/**
 * Tema do app: **escuro fixo** (independe do modo do sistema) e **sem cores dinâmicas** (Material You),
 * para manter a identidade visual do RememberMe.
 */
@Composable
fun AndroidEventSchedulerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RememberMeColorScheme,
        typography = Typography,
        content = content,
    )
}
