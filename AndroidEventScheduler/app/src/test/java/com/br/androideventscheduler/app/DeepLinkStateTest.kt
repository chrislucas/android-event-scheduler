package com.br.androideventscheduler.app

import androidx.navigation3.runtime.NavKey
import com.br.androideventscheduler.core.navigation.Home
import com.br.androideventscheduler.core.navigation.ScheduleDetail
import com.br.androideventscheduler.core.navigation.Search
import com.br.androideventscheduler.core.navigation.Splash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkStateTest {
    private val deepLinkState = DeepLinkState()

    @Test
    fun `given nothing registered when reading the pending id then it is null`() {
        assertNull(deepLinkState.pendingScheduleId.value)
    }

    @Test
    fun `given an id when setting then it becomes the pending id`() {
        deepLinkState.set("abc")

        assertEquals("abc", deepLinkState.pendingScheduleId.value)
    }

    @Test
    fun `given null or blank ids when setting then the pending id is unchanged`() {
        deepLinkState.set("abc")

        deepLinkState.set(null)
        deepLinkState.set("  ")

        assertEquals("abc", deepLinkState.pendingScheduleId.value)
    }

    @Test
    fun `given a pending id when consuming it then it is cleared and cannot be consumed twice`() {
        deepLinkState.set("abc")

        deepLinkState.consume("abc")
        deepLinkState.consume("abc")

        assertNull(deepLinkState.pendingScheduleId.value)
    }

    @Test
    fun `given a newer id arrived when consuming the older one then the newer id is kept`() {
        deepLinkState.set("old")
        deepLinkState.set("new")

        deepLinkState.consume("old")

        assertEquals("new", deepLinkState.pendingScheduleId.value)
    }
}

class DeepLinkNavigationTest {
    @Test
    fun `given home on top and a pending id when opening then schedule detail is pushed and reported as used`() {
        val backStack = mutableListOf<NavKey>(Home)

        val used = backStack.openPendingScheduleDetail("abc")

        assertTrue(used)
        assertEquals(listOf<NavKey>(Home, ScheduleDetail("abc")), backStack)
    }

    @Test
    fun `given splash still on top when opening then nothing changes so the id stays pending`() {
        val backStack = mutableListOf<NavKey>(Splash)

        val used = backStack.openPendingScheduleDetail("abc")

        assertFalse(used)
        assertEquals(listOf<NavKey>(Splash), backStack)
    }

    @Test
    fun `given another screen on top when opening then nothing changes`() {
        val backStack = mutableListOf<NavKey>(Home, Search)

        val used = backStack.openPendingScheduleDetail("abc")

        assertFalse(used)
        assertEquals(listOf<NavKey>(Home, Search), backStack)
    }

    @Test
    fun `given no pending id when opening then nothing changes`() {
        val backStack = mutableListOf<NavKey>(Home)

        val used = backStack.openPendingScheduleDetail(null)

        assertFalse(used)
        assertEquals(listOf<NavKey>(Home), backStack)
    }
}
