package com.br.androideventscheduler.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ThemeRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "notnight")
    fun `given light system mode when applying theme then palette stays dark and ignores dynamic color`() {
        var background = Color.Unspecified
        var primary = Color.Unspecified
        var surface = Color.Unspecified

        composeRule.setContent {
            AndroidEventSchedulerTheme {
                background = MaterialTheme.colorScheme.background
                primary = MaterialTheme.colorScheme.primary
                surface = MaterialTheme.colorScheme.surface
            }
        }
        composeRule.waitForIdle()

        assertEquals(Color(0xFF0A0A0A), background)
        assertEquals(Color(0xFF26C6DA), primary)
        assertEquals(Color(0xFF171717), surface)
    }

    @Test
    @Config(qualifiers = "night")
    fun `given darkTheme false when applying theme then palette is light even with the system in night mode`() {
        var background = Color.Unspecified
        var primary = Color.Unspecified

        composeRule.setContent {
            AndroidEventSchedulerTheme(darkTheme = false) {
                background = MaterialTheme.colorScheme.background
                primary = MaterialTheme.colorScheme.primary
            }
        }
        composeRule.waitForIdle()

        assertEquals(Color(0xFFFAFAFA), background)
        assertEquals(Color(0xFF00727F), primary)
    }
}
