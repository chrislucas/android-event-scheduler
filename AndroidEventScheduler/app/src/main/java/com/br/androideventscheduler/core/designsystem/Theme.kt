package com.br.androideventscheduler.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

internal val RememberMeDarkColorScheme =
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

internal val RememberMeLightColorScheme =
    lightColorScheme(
        primary = AccentCyanLight,
        onPrimary = OnAccentLight,
        primaryContainer = AccentContainerLight,
        onPrimaryContainer = OnAccentContainerLight,
        secondary = AccentCyanLight,
        onSecondary = OnAccentLight,
        secondaryContainer = AccentContainerLight,
        onSecondaryContainer = OnAccentContainerLight,
        background = BackgroundLight,
        onBackground = TextPrimaryLight,
        surface = SurfaceLight,
        onSurface = TextPrimaryLight,
        surfaceVariant = SurfaceElevatedLight,
        onSurfaceVariant = TextSecondaryLight,
        surfaceContainerLowest = SurfaceLight,
        surfaceContainerLow = SurfaceLight,
        surfaceContainer = SurfaceLight,
        surfaceContainerHigh = SurfaceElevatedLight,
        surfaceContainerHighest = SurfaceContainerHighestLight,
        error = ErrorRedLight,
        onError = OnAccentLight,
        errorContainer = ErrorContainerLight,
        onErrorContainer = OnErrorContainerLight,
        outline = OutlineLight,
        outlineVariant = OutlineVariantLight,
        inverseSurface = InverseSurfaceLight,
        inverseOnSurface = InverseOnSurfaceLight,
        inversePrimary = InversePrimaryLight,
    )

// Sem preview: wrapper de tema, sem UI própria.

/**
 * Tema do app, **sem cores dinâmicas** (Material You), para manter a identidade visual do RememberMe.
 * [darkTheme] vem da escolha do usuário já resolvida (`ThemeMode.isDark`). O padrão `true` mantém as previews e os
 * testes existentes no tema escuro.
 */
@Composable
fun AndroidEventSchedulerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) RememberMeDarkColorScheme else RememberMeLightColorScheme,
        typography = Typography,
        content = content,
    )
}
