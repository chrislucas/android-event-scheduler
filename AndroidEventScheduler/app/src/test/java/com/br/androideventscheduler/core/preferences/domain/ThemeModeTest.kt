package com.br.androideventscheduler.core.preferences.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun `given each stable id when parsing then it maps to its mode`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageId("system"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorageId("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageId("dark"))
    }

    @Test
    fun `given null when parsing then it falls back to dark`() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageId(null))
    }

    @Test
    fun `given an unknown id when parsing then it falls back to dark`() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageId("sepia"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageId(""))
    }

    @Test
    fun `given ids with different case when parsing then they are not accepted and fall back to dark`() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageId("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageId("System"))
    }

    @Test
    fun `given the enum names when parsing then they are not the stored ids`() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageId(ThemeMode.LIGHT.name))
    }

    @Test
    fun `given the persisted contract then storage ids are stable`() {
        assertEquals("system", ThemeMode.SYSTEM.storageId)
        assertEquals("light", ThemeMode.LIGHT.storageId)
        assertEquals("dark", ThemeMode.DARK.storageId)
        assertEquals(listOf("system", "light", "dark"), ThemeMode.entries.map { it.storageId })
    }

    @Test
    fun `given no preference then the default is dark`() {
        assertEquals(ThemeMode.DARK, ThemeMode.DEFAULT)
    }
}
