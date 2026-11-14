package com.br.androideventscheduler.app

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation3.runtime.NavKey
import com.br.androideventscheduler.core.navigation.Home
import com.br.androideventscheduler.core.navigation.ScheduleDetail
import com.br.androideventscheduler.core.navigation.Splash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeepLinkEffectRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val deepLinkState = DeepLinkState()

    @Test
    fun `given a pending id and home on top when the effect runs then the detail is pushed and the id consumed`() {
        val backStack = mutableStateListOf<NavKey>(Home)
        deepLinkState.set("abc")

        composeRule.setContent { DeepLinkEffect(backStack, deepLinkState) }
        composeRule.waitForIdle()

        assertEquals(listOf<NavKey>(Home, ScheduleDetail("abc")), backStack.toList())
        assertNull(deepLinkState.pendingScheduleId.value)
    }

    @Test
    fun `given a pending id during splash when home replaces it then the detail opens only after home is on top`() {
        val backStack = mutableStateListOf<NavKey>(Splash)
        deepLinkState.set("abc")
        composeRule.setContent { DeepLinkEffect(backStack, deepLinkState) }
        composeRule.waitForIdle()
        assertEquals(listOf<NavKey>(Splash), backStack.toList())
        assertEquals("abc", deepLinkState.pendingScheduleId.value)

        composeRule.runOnIdle { backStack.replaceSplashWithHome() }
        composeRule.waitForIdle()

        assertEquals(listOf<NavKey>(Home, ScheduleDetail("abc")), backStack.toList())
        assertNull(deepLinkState.pendingScheduleId.value)
    }

    @Test
    fun `given the id was consumed when the effect recomposes then the detail is not pushed again`() {
        val backStack = mutableStateListOf<NavKey>(Home)
        deepLinkState.set("abc")
        composeRule.setContent { DeepLinkEffect(backStack, deepLinkState) }
        composeRule.waitForIdle()

        composeRule.runOnIdle { backStack.removeLastOrNull() }
        composeRule.waitForIdle()

        assertEquals(listOf<NavKey>(Home), backStack.toList())
    }

    @Test
    fun `given the app is already on home when a new id arrives then the detail opens for it`() {
        val backStack = mutableStateListOf<NavKey>(Home)
        composeRule.setContent { DeepLinkEffect(backStack, deepLinkState) }
        composeRule.waitForIdle()

        composeRule.runOnIdle { deepLinkState.set("xyz") }
        composeRule.waitForIdle()

        assertEquals(listOf<NavKey>(Home, ScheduleDetail("xyz")), backStack.toList())
    }
}
