package com.br.androideventscheduler.feature.splash.presentation.viewmodel

import com.br.androideventscheduler.core.testing.MainDispatcherRule
import com.br.androideventscheduler.feature.splash.presentation.effect.SplashUiEffect
import com.br.androideventscheduler.feature.splash.presentation.event.SplashUiEvent
import com.br.androideventscheduler.feature.splash.presentation.state.SplashUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `given splash started when delay has not elapsed then no effect is emitted`() =
        runTest(mainRule.dispatcher) {
            val viewModel = SplashViewModel(dispatcher = mainRule.dispatcher)
            val effects = mutableListOf<SplashUiEffect>()
            val collectJob = launch { viewModel.uiEffect.collect { effects += it } }

            viewModel.onEvent(SplashUiEvent.OnSplashStarted)
            advanceTimeBy(SplashViewModel.SPLASH_DELAY_MILLIS - 1)
            runCurrent()

            assertTrue(effects.isEmpty())
            collectJob.cancel()
        }

    @Test
    fun `given splash started when delay elapses then emits NavigateToHome`() =
        runTest(mainRule.dispatcher) {
            val viewModel = SplashViewModel(dispatcher = mainRule.dispatcher)
            val effects = mutableListOf<SplashUiEffect>()
            val collectJob = launch { viewModel.uiEffect.collect { effects += it } }

            viewModel.onEvent(SplashUiEvent.OnSplashStarted)
            advanceTimeBy(SplashViewModel.SPLASH_DELAY_MILLIS)
            runCurrent()

            assertEquals(listOf(SplashUiEffect.NavigateToHome), effects)
            collectJob.cancel()
        }

    @Test
    fun `given splash started twice when delay elapses then emits only one effect`() =
        runTest(mainRule.dispatcher) {
            val viewModel = SplashViewModel(dispatcher = mainRule.dispatcher)
            val effects = mutableListOf<SplashUiEffect>()
            val collectJob = launch { viewModel.uiEffect.collect { effects += it } }

            viewModel.onEvent(SplashUiEvent.OnSplashStarted)
            viewModel.onEvent(SplashUiEvent.OnSplashStarted)
            advanceUntilIdle()

            assertEquals(listOf(SplashUiEffect.NavigateToHome), effects)
            collectJob.cancel()
        }

    @Test
    fun `given no started event when time passes then no effect is emitted`() =
        runTest(mainRule.dispatcher) {
            val viewModel = SplashViewModel(dispatcher = mainRule.dispatcher)
            val effects = mutableListOf<SplashUiEffect>()
            val collectJob = launch { viewModel.uiEffect.collect { effects += it } }

            advanceTimeBy(SplashViewModel.SPLASH_DELAY_MILLIS * 2)
            runCurrent()

            assertTrue(effects.isEmpty())
            collectJob.cancel()
        }

    @Test
    fun `given new view model when reading state then it is Showing`() {
        val viewModel = SplashViewModel(dispatcher = mainRule.dispatcher)

        assertEquals(SplashUiState.Showing, viewModel.uiState.value)
    }
}
