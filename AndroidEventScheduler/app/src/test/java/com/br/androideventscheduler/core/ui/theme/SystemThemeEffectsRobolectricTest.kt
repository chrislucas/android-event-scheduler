package com.br.androideventscheduler.core.ui.theme

import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowInsetsControllerCompat
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR-w411dp-h891dp")
class SystemThemeEffectsRobolectricTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var dark by mutableStateOf(true)
    private var backgroundSeenByContent = 0

    private fun lightIcons(): Boolean {
        val window = composeRule.activity.window
        return WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars
    }

    private fun lightNavigationIcons(): Boolean {
        val window = composeRule.activity.window
        return WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars
    }

    private fun windowBackground(): Int = (composeRule.activity.window.decorView.background as ColorDrawable).color

    private fun setContent() {
        composeRule.setContent {
            AndroidEventSchedulerTheme(darkTheme = dark) {
                backgroundSeenByContent = MaterialTheme.colorScheme.background.toArgb()
                SystemThemeEffects(darkTheme = dark)
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `given the dark theme when applying effects then bar icons are light and the window matches the background`() {
        setContent()

        assertFalse(lightIcons())
        assertFalse(lightNavigationIcons())
        assertEquals(backgroundSeenByContent, windowBackground())
    }

    @Test
    fun `given the light theme when applying effects then bar icons are dark and the window matches the background`() {
        dark = false
        setContent()

        assertTrue(lightIcons())
        assertTrue(lightNavigationIcons())
        assertEquals(backgroundSeenByContent, windowBackground())
    }

    @Test
    fun `given the theme changes at runtime when recomposing then the same activity gets the new style`() {
        setContent()
        val activityBefore = composeRule.activity

        dark = false
        composeRule.waitForIdle()

        assertTrue(lightIcons())
        assertEquals(backgroundSeenByContent, windowBackground())
        assertTrue(activityBefore === composeRule.activity)

        dark = true
        composeRule.waitForIdle()

        assertFalse(lightIcons())
    }
}
