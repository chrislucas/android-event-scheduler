package com.br.androideventscheduler.core.designsystem

import com.br.androideventscheduler.core.preferences.domain.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeResolutionTest {
    @Test
    fun `given system mode when resolving then it follows the system`() {
        assertEquals(true, ThemeMode.SYSTEM.isDark(systemInDarkTheme = true))
        assertEquals(false, ThemeMode.SYSTEM.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `given light mode when resolving then it is never dark`() {
        assertEquals(false, ThemeMode.LIGHT.isDark(systemInDarkTheme = true))
        assertEquals(false, ThemeMode.LIGHT.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `given dark mode when resolving then it is always dark`() {
        assertEquals(true, ThemeMode.DARK.isDark(systemInDarkTheme = true))
        assertEquals(true, ThemeMode.DARK.isDark(systemInDarkTheme = false))
    }
}
