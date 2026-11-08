package com.br.androideventscheduler.feature.splash

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.br.androideventscheduler.MainActivity
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.ui.SemanticsTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SplashNavigationRobolectricTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `given app launched when splash delay elapses then home replaces splash and back leaves the app`() {
        composeRule.onNodeWithTag(SemanticsTags.SPLASH_LOGO).assertIsDisplayed()

        composeRule.waitUntil(timeoutMillis = NAVIGATION_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(SemanticsTags.HOME_CALENDAR_GRID).fetchSemanticsNodes().isNotEmpty()
        }
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

    private companion object {
        const val NAVIGATION_TIMEOUT_MILLIS = 5_000L
    }
}
