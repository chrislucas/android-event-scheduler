package com.br.androideventscheduler.app

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.NavKey
import com.br.androideventscheduler.core.navigation.AgendaBackup
import com.br.androideventscheduler.core.navigation.Home
import com.br.androideventscheduler.core.navigation.Search
import com.br.androideventscheduler.core.navigation.Splash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IncomingFileNavigationRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val incomingFileState = IncomingFileState()
    private val fileUri = "content://mail/agenda.json"

    @Test
    fun `given home on top and a pending uri when opening then the backup preview is pushed and reported as used`() {
        val backStack = mutableListOf<NavKey>(Home)

        val used = backStack.openPendingAgendaFile(fileUri)

        assertTrue(used)
        assertEquals(listOf<NavKey>(Home, AgendaBackup(fileUri)), backStack)
    }

    @Test
    fun `given another screen on top when opening then the backup preview is pushed over it`() {
        val backStack = mutableListOf<NavKey>(Home, Search)

        val used = backStack.openPendingAgendaFile(fileUri)

        assertTrue(used)
        assertEquals(listOf<NavKey>(Home, Search, AgendaBackup(fileUri)), backStack)
    }

    @Test
    fun `given splash on top or no uri when opening then nothing changes`() {
        val splash = mutableListOf<NavKey>(Splash)
        val home = mutableListOf<NavKey>(Home)

        assertFalse(splash.openPendingAgendaFile(fileUri))
        assertFalse(home.openPendingAgendaFile(null))

        assertEquals(listOf<NavKey>(Splash), splash)
        assertEquals(listOf<NavKey>(Home), home)
    }

    @Test
    fun `given a pending uri during splash when home replaces it then the preview opens only after home is on top`() {
        val backStack = mutableStateListOf<NavKey>(Splash)
        incomingFileState.set(fileUri)
        composeRule.setContent { IncomingFileEffect(backStack, incomingFileState) }
        composeRule.waitForIdle()
        assertEquals(listOf<NavKey>(Splash), backStack.toList())
        assertEquals(fileUri, incomingFileState.pendingFileUri.value)

        composeRule.runOnIdle { backStack.replaceSplashWithHome() }
        composeRule.waitForIdle()

        assertEquals(listOf<NavKey>(Home, AgendaBackup(fileUri)), backStack.toList())
        assertNull(incomingFileState.pendingFileUri.value)
    }

    @Test
    fun `given the uri was consumed when the stack changes then the preview is not pushed again`() {
        val backStack = mutableStateListOf<NavKey>(Home)
        incomingFileState.set(fileUri)
        composeRule.setContent { IncomingFileEffect(backStack, incomingFileState) }
        composeRule.waitForIdle()

        composeRule.runOnIdle { backStack.removeLastOrNull() }
        composeRule.waitForIdle()

        assertEquals(listOf<NavKey>(Home), backStack.toList())
    }

    @Test
    fun `given the app is already open when a new file arrives then the preview opens for it`() {
        val backStack = mutableStateListOf<NavKey>(Home)
        composeRule.setContent { IncomingFileEffect(backStack, incomingFileState) }
        composeRule.waitForIdle()

        composeRule.runOnIdle { incomingFileState.set(fileUri) }
        composeRule.waitForIdle()

        assertEquals(listOf<NavKey>(Home, AgendaBackup(fileUri)), backStack.toList())
    }
}
