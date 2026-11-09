package com.br.androideventscheduler.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** Contraste WCAG 2.x dos pares texto/fundo nas duas paletas. Falha abaixo de 4,5:1 (AA para texto normal). */
class ColorContrastTest {
    private fun channel(value: Int): Double {
        val s = value / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double {
        val argb = color.toArgb()
        return 0.2126 * channel((argb shr 16) and 0xFF) + 0.7152 * channel((argb shr 8) and 0xFF) + 0.0722 * channel(argb and 0xFF)
    }

    private fun contrast(
        a: Color,
        b: Color,
    ): Double {
        val (light, dark) = luminance(a).coerceAtLeast(luminance(b)) to luminance(a).coerceAtMost(luminance(b))
        return (light + 0.05) / (dark + 0.05)
    }

    /** Pares do R4.1 (obrigatórios) e dos papéis extras que os componentes do app realmente usam. */
    private fun pairs(scheme: ColorScheme): Map<String, Pair<Color, Color>> =
        mapOf(
            "onBackground/background" to (scheme.onBackground to scheme.background),
            "onSurface/surface" to (scheme.onSurface to scheme.surface),
            "onSurfaceVariant/surface" to (scheme.onSurfaceVariant to scheme.surface),
            "onSurfaceVariant/surfaceVariant" to (scheme.onSurfaceVariant to scheme.surfaceVariant),
            "primary/background" to (scheme.primary to scheme.background),
            "onPrimary/primary" to (scheme.onPrimary to scheme.primary),
            "error/surface" to (scheme.error to scheme.surface),
            "primary/surface" to (scheme.primary to scheme.surface),
            "onSecondary/secondary" to (scheme.onSecondary to scheme.secondary),
            "onSurface/surfaceContainerHigh" to (scheme.onSurface to scheme.surfaceContainerHigh),
            "onSurface/surfaceContainerHighest" to (scheme.onSurface to scheme.surfaceContainerHighest),
        )

    private fun assertAllPairsPass(scheme: ColorScheme) {
        val failures =
            pairs(scheme)
                .filter { (_, colors) -> contrast(colors.first, colors.second) < MIN_CONTRAST }
                .map { (name, colors) -> "$name = ${contrast(colors.first, colors.second)}" }
        assertTrue("Pares abaixo de $MIN_CONTRAST:1: $failures", failures.isEmpty())
    }

    @Test
    fun `given the light palette when computing contrast then every pair is at least 4_5 to 1`() {
        assertAllPairsPass(RememberMeLightColorScheme)
    }

    @Test
    fun `given the dark palette when computing contrast then every pair is at least 4_5 to 1`() {
        assertAllPairsPass(RememberMeDarkColorScheme)
    }

    @Test
    fun `given the light container roles when computing contrast then their content is readable`() {
        val scheme = RememberMeLightColorScheme
        assertTrue(contrast(scheme.onPrimaryContainer, scheme.primaryContainer) >= MIN_CONTRAST)
        assertTrue(contrast(scheme.onSecondaryContainer, scheme.secondaryContainer) >= MIN_CONTRAST)
        assertTrue(contrast(scheme.onErrorContainer, scheme.errorContainer) >= MIN_CONTRAST)
        assertTrue(contrast(scheme.inverseOnSurface, scheme.inverseSurface) >= MIN_CONTRAST)
        assertTrue(contrast(scheme.inversePrimary, scheme.inverseSurface) >= MIN_CONTRAST)
        assertTrue(contrast(scheme.onError, scheme.error) >= MIN_CONTRAST)
    }

    @Test
    fun `given the original cyan on white when computing contrast then the helper reports the known 2_06`() {
        assertEquals(2.06, contrast(AccentCyan, Color.White), 0.01)
    }

    @Test
    fun `given black on white when computing contrast then the helper reports 21`() {
        assertEquals(21.0, contrast(Color.Black, Color.White), 0.001)
    }

    @Test
    fun `given the dark scheme then it keeps the original colors`() {
        val scheme = RememberMeDarkColorScheme
        assertEquals(Color(0xFF0A0A0A), scheme.background)
        assertEquals(Color(0xFF171717), scheme.surface)
        assertEquals(Color(0xFF1C1C1E), scheme.surfaceVariant)
        assertEquals(Color(0xFF26C6DA), scheme.primary)
        assertEquals(Color(0xFFFFFFFF), scheme.onBackground)
        assertEquals(Color(0xFFB0B0B0), scheme.onSurfaceVariant)
        assertEquals(Color(0xFFEF5350), scheme.error)
        assertEquals(Color(0xFF3A3A3C), scheme.outline)
    }

    private companion object {
        const val MIN_CONTRAST = 4.5
    }
}
