package com.br.androideventscheduler.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRuleTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `given main dispatcher rule when launching on Main then work runs only after advancing the test dispatcher`() =
        runTest(mainRule.dispatcher) {
            var executed = false

            launch(Dispatchers.Main) { executed = true }
            assertEquals(false, executed)
            advanceUntilIdle()

            assertEquals(true, executed)
        }
}
