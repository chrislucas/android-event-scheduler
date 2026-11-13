package com.br.androideventscheduler.feature.splash

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.br.androideventscheduler.MainActivity
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.testing.hasNode
import com.br.androideventscheduler.core.testing.waitUntilWithMainLooper
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.splash.presentation.viewmodel.SplashViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class SplashNavigationRobolectricTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `given app launched when splash delay elapses then home replaces splash and back leaves the app`() {
        // A Splash só é composta depois que o tema é lido do DataStore (thread real de I/O): esperar, não asserir direto.
        composeRule.waitUntilWithMainLooper { composeRule.hasNode(SemanticsTags.SPLASH_LOGO) }
        composeRule.onNodeWithTag(SemanticsTags.SPLASH_LOGO).assertIsDisplayed()

        // O tempo da Splash é avançado no relógio virtual, de forma explícita, em vez de depender de quantos quadros
        // o Robolectric desenha (cada quadro avança o relógio virtual em 15 ms).
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SplashViewModel.SPLASH_DELAY_MILLIS))
        composeRule.waitUntilWithMainLooper { composeRule.hasNode(SemanticsTags.HOME_CALENDAR_GRID) }
        composeRule.onNodeWithTag(SemanticsTags.HOME_CALENDAR_GRID).assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag(SemanticsTags.SPLASH_LOGO).fetchSemanticsNodes().size)

        composeRule.activityRule.scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals(0, composeRule.onAllNodesWithTag(SemanticsTags.SPLASH_LOGO).fetchSemanticsNodes().size)
        composeRule.activityRule.scenario.onActivity { activity -> assertTrue(activity.isFinishing) }
    }

    @Test
    fun `given main activity when inspecting its declared theme then it starts with the dark splash theme`() {
        val activity = composeRule.activity
        val activityInfo = activity.packageManager.getActivityInfo(activity.componentName, 0)

        assertEquals(R.style.Theme_AndroidEventScheduler_Starting, activityInfo.theme)

        val startingTheme = activity.resources.newTheme().apply { applyStyle(activityInfo.theme, true) }
        val splashAttributes =
            startingTheme.obtainStyledAttributes(
                intArrayOf(androidx.core.splashscreen.R.attr.windowSplashScreenBackground),
            )
        val splashBackground = splashAttributes.getColor(0, 0)
        splashAttributes.recycle()

        assertEquals(activity.getColor(R.color.background_dark), splashBackground)
    }
}
