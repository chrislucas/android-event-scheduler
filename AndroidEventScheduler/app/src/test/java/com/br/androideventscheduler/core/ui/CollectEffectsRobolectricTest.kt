package com.br.androideventscheduler.core.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CollectEffectsRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `given effect sent while stopped when lifecycle starts then it is delivered exactly once`() {
        val owner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val channel = Channel<String>(Channel.BUFFERED)
        val received = mutableListOf<String>()
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                CollectEffects(channel.receiveAsFlow()) { received += it }
            }
        }

        channel.trySend("navigate")
        composeRule.waitForIdle()
        assertEquals(emptyList<String>(), received)

        owner.currentState = Lifecycle.State.STARTED
        composeRule.waitForIdle()
        owner.currentState = Lifecycle.State.CREATED
        owner.currentState = Lifecycle.State.STARTED
        composeRule.waitForIdle()

        assertEquals(listOf("navigate"), received)
    }

    @Test
    fun `given started screen when effect is sent then it is delivered immediately`() {
        val owner = TestLifecycleOwner(Lifecycle.State.STARTED)
        val channel = Channel<Int>(Channel.BUFFERED)
        val received = mutableListOf<Int>()
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                CollectEffects(channel.receiveAsFlow()) { received += it }
            }
        }

        channel.trySend(1)
        channel.trySend(2)
        composeRule.waitForIdle()

        assertEquals(listOf(1, 2), received)
    }
}
