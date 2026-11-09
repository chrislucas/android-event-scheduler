package com.br.androideventscheduler.feature.auth.presentation.viewmodel

import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.security.domain.AppLockState
import com.br.androideventscheduler.core.security.domain.AuthResult
import com.br.androideventscheduler.core.security.testing.appLockController
import com.br.androideventscheduler.core.testing.MainDispatcherRule
import com.br.androideventscheduler.feature.auth.presentation.effect.LockUiEffect
import com.br.androideventscheduler.feature.auth.presentation.event.LockUiEvent
import com.br.androideventscheduler.feature.auth.presentation.state.LockUiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LockViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun TestScope.viewModel(): LockViewModel {
        val controller = appLockController(scope = backgroundScope)
        runCurrent()
        return LockViewModel(appLockController = controller, dispatcher = mainRule.dispatcher)
    }

    private fun TestScope.collectEffects(viewModel: LockViewModel): List<LockUiEffect> {
        val effects = mutableListOf<LockUiEffect>()
        backgroundScope.launch { viewModel.uiEffect.collect { effects += it } }
        runCurrent()
        return effects
    }

    @Test
    fun `given the screen is shown twice when handling events then authentication is launched only once`() =
        runTest(mainRule.dispatcher) {
            val viewModel = viewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(LockUiEvent.OnScreenShown)
            viewModel.onEvent(LockUiEvent.OnScreenShown)
            runCurrent()

            assertEquals(listOf(LockUiEffect.LaunchAuthentication), effects)
        }

    @Test
    fun `given authentication was canceled when the unlock button is clicked then authentication is launched again`() =
        runTest(mainRule.dispatcher) {
            val viewModel = viewModel()
            val effects = collectEffects(viewModel)
            viewModel.onEvent(LockUiEvent.OnScreenShown)
            viewModel.onEvent(LockUiEvent.OnAuthResult(AuthResult.Canceled))

            viewModel.onEvent(LockUiEvent.OnUnlockClicked)
            runCurrent()

            assertEquals(2, effects.size)
        }

    @Test
    fun `given authentication launched when it is in progress then the state is authenticating`() =
        runTest(mainRule.dispatcher) {
            val viewModel = viewModel()

            viewModel.onEvent(LockUiEvent.OnScreenShown)

            assertEquals(LockUiState.Authenticating, viewModel.uiState.value)
        }

    @Test
    fun `given authentication succeeded when handling the result then the controller is unlocked`() =
        runTest(mainRule.dispatcher) {
            val controller = appLockController(scope = backgroundScope)
            runCurrent()
            val viewModel = LockViewModel(controller, mainRule.dispatcher)
            assertEquals(AppLockState.Locked, controller.lockState.value)

            viewModel.onEvent(LockUiEvent.OnAuthResult(AuthResult.Success))

            assertEquals(AppLockState.Unlocked, controller.lockState.value)
            assertEquals(LockUiState.Idle, viewModel.uiState.value)
        }

    @Test
    fun `given authentication succeeded when the app locks again then the screen launches authentication again`() =
        runTest(mainRule.dispatcher) {
            val viewModel = viewModel()
            val effects = collectEffects(viewModel)
            viewModel.onEvent(LockUiEvent.OnScreenShown)
            viewModel.onEvent(LockUiEvent.OnAuthResult(AuthResult.Success))

            viewModel.onEvent(LockUiEvent.OnScreenShown)
            runCurrent()

            assertEquals(2, effects.size)
        }

    @Test
    fun `given authentication was canceled when handling the result then it returns to idle without a message`() =
        runTest(mainRule.dispatcher) {
            val viewModel = viewModel()
            viewModel.onEvent(LockUiEvent.OnScreenShown)

            viewModel.onEvent(LockUiEvent.OnAuthResult(AuthResult.Canceled))

            assertEquals(LockUiState.Idle, viewModel.uiState.value)
        }

    @Test
    fun `given the system locked out the user when handling the result then the locked out message is shown`() =
        runTest(mainRule.dispatcher) {
            val viewModel = viewModel()

            viewModel.onEvent(LockUiEvent.OnAuthResult(AuthResult.LockedOut))

            assertEquals(LockUiState.Message(R.string.lock_locked_out), viewModel.uiState.value)
        }

    @Test
    fun `given no device credential when handling the result then the not available message is shown`() =
        runTest(mainRule.dispatcher) {
            val viewModel = viewModel()

            viewModel.onEvent(LockUiEvent.OnAuthResult(AuthResult.NotAvailable))

            assertEquals(LockUiState.Message(R.string.lock_not_available), viewModel.uiState.value)
        }

    @Test
    fun `given an unexpected error when handling the result then the generic error message is shown`() =
        runTest(mainRule.dispatcher) {
            val viewModel = viewModel()

            viewModel.onEvent(LockUiEvent.OnAuthResult(AuthResult.Error(code = 8)))

            assertEquals(LockUiState.Message(R.string.lock_error), viewModel.uiState.value)
        }
}
