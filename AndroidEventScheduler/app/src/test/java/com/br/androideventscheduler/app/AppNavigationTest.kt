package com.br.androideventscheduler.app

import androidx.navigation3.runtime.NavKey
import com.br.androideventscheduler.core.navigation.Home
import com.br.androideventscheduler.core.navigation.Splash
import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavigationTest {
    @Test
    fun `given splash on back stack when replacing with home then only home remains`() {
        val backStack = mutableListOf<NavKey>(Splash)

        backStack.replaceSplashWithHome()

        assertEquals(listOf<NavKey>(Home), backStack)
    }

    @Test
    fun `given home already shown when replacing again then back stack is unchanged`() {
        val backStack = mutableListOf<NavKey>(Home)

        backStack.replaceSplashWithHome()

        assertEquals(listOf<NavKey>(Home), backStack)
    }
}
