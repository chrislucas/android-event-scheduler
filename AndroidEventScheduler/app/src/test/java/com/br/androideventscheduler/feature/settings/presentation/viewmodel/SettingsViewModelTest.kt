package com.br.androideventscheduler.feature.settings.presentation.viewmodel

import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.testing.FakeUserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.security.data.AppLockController
import com.br.androideventscheduler.core.security.domain.AppLockTimeout
import com.br.androideventscheduler.core.security.domain.AuthResult
import com.br.androideventscheduler.core.testing.MainDispatcherRule
import com.br.androideventscheduler.feature.settings.presentation.effect.SettingsUiEffect
import com.br.androideventscheduler.feature.settings.presentation.event.SettingsUiEvent
import com.br.androideventscheduler.feature.settings.presentation.state.SettingsUiState
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val savedSettings =
        UserSettings(
            activeAccountEmail = "ana@exemplo.com",
            defaultReminderOffset = ReminderOffset.THIRTY_MINUTES,
            defaultSnoozeMinutes = 5,
            fullScreenEventAlertEnabled = false,
        )

    private val appLockController = mockk<AppLockController>(relaxed = true)
    private var deviceSecure = true

    private fun newViewModel(repository: FakeUserSettingsRepository) =
        SettingsViewModel(
            userSettingsRepository = repository,
            appLockController = appLockController,
            deviceSecurity = { deviceSecure },
            dispatcher = mainRule.dispatcher,
        )

    private fun TestScope.createLoadedViewModel(repository: FakeUserSettingsRepository): SettingsViewModel {
        val viewModel = newViewModel(repository)
        advanceUntilIdle()
        return viewModel
    }

    private fun SettingsViewModel.content(): SettingsUiState.Content = uiState.value as SettingsUiState.Content

    @Test
    fun `given saved settings when view model starts then it loads them into the draft`() =
        runTest(mainRule.dispatcher) {
            val viewModel = newViewModel(FakeUserSettingsRepository(savedSettings))
            assertEquals(SettingsUiState.Loading, viewModel.uiState.value)

            advanceUntilIdle()

            assertEquals(savedSettings, viewModel.content().draft)
        }

    @Test
    fun `given user editing when repository re-emits then the draft is not overwritten`() =
        runTest(mainRule.dispatcher) {
            val repository = FakeUserSettingsRepository(savedSettings)
            val viewModel = createLoadedViewModel(repository)

            viewModel.onEvent(SettingsUiEvent.OnEmailChanged("novo@exemplo.com"))
            repository.settings.value = savedSettings.copy(activeAccountEmail = "externo@exemplo.com")
            advanceUntilIdle()

            assertEquals("novo@exemplo.com", viewModel.content().draft.activeAccountEmail)
        }

    @Test
    fun `given field events when handled then each one updates the draft`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createLoadedViewModel(FakeUserSettingsRepository())

            viewModel.onEvent(SettingsUiEvent.OnReminderSelected(ReminderOffset.ONE_HOUR))
            viewModel.onEvent(SettingsUiEvent.OnSnoozeSelected(15))
            viewModel.onEvent(SettingsUiEvent.OnFullScreenToggled(false))

            val draft = viewModel.content().draft
            assertEquals(ReminderOffset.ONE_HOUR, draft.defaultReminderOffset)
            assertEquals(15, draft.defaultSnoozeMinutes)
            assertEquals(false, draft.fullScreenEventAlertEnabled)
        }

    @Test
    fun `given invalid email when saving then error is shown and nothing is saved nor navigated`() =
        runTest(mainRule.dispatcher) {
            val repository = FakeUserSettingsRepository()
            val viewModel = createLoadedViewModel(repository)
            val effects = mutableListOf<SettingsUiEffect>()
            val collectJob = launch { viewModel.uiEffect.collect { effects += it } }

            viewModel.onEvent(SettingsUiEvent.OnEmailChanged("sem-arroba"))
            viewModel.onEvent(SettingsUiEvent.OnSaveClicked)
            advanceUntilIdle()

            assertTrue(viewModel.content().emailError)
            assertEquals(0, repository.saveCalls)
            assertTrue(effects.none { it == SettingsUiEffect.NavigateBack })
            collectJob.cancel()
        }

    @Test
    fun `given email error when user types again then the error is cleared`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createLoadedViewModel(FakeUserSettingsRepository())
            viewModel.onEvent(SettingsUiEvent.OnEmailChanged(""))
            viewModel.onEvent(SettingsUiEvent.OnSaveClicked)
            advanceUntilIdle()

            viewModel.onEvent(SettingsUiEvent.OnEmailChanged("a"))

            assertEquals(false, viewModel.content().emailError)
        }

    @Test
    fun `given valid draft when saving then it persists trimmed and confirms then navigates back`() =
        runTest(mainRule.dispatcher) {
            val repository = FakeUserSettingsRepository()
            val viewModel = createLoadedViewModel(repository)
            val effects = mutableListOf<SettingsUiEffect>()
            val collectJob = launch { viewModel.uiEffect.collect { effects += it } }

            viewModel.onEvent(SettingsUiEvent.OnEmailChanged("  bia@exemplo.com "))
            viewModel.onEvent(SettingsUiEvent.OnSaveClicked)
            advanceUntilIdle()

            assertEquals("bia@exemplo.com", repository.settings.value.activeAccountEmail)
            assertEquals(listOf(SettingsUiEffect.ShowSavedConfirmation, SettingsUiEffect.NavigateBack), effects)
            collectJob.cancel()
        }

    @Test
    fun `given save already in progress when tapping save again then it is saved only once`() =
        runTest(mainRule.dispatcher) {
            val repository = FakeUserSettingsRepository()
            val viewModel = createLoadedViewModel(repository)

            viewModel.onEvent(SettingsUiEvent.OnSaveClicked)
            viewModel.onEvent(SettingsUiEvent.OnSaveClicked)
            advanceUntilIdle()

            assertEquals(1, repository.saveCalls)
        }

    @Test
    fun `given storage failure when saving then error message is emitted and screen stays`() =
        runTest(mainRule.dispatcher) {
            val repository = FakeUserSettingsRepository().apply { failOnSave = true }
            val viewModel = createLoadedViewModel(repository)
            val effects = mutableListOf<SettingsUiEffect>()
            val collectJob = launch { viewModel.uiEffect.collect { effects += it } }

            viewModel.onEvent(SettingsUiEvent.OnSaveClicked)
            advanceUntilIdle()

            assertEquals(listOf<SettingsUiEffect>(SettingsUiEffect.ShowError(R.string.settings_save_error)), effects)
            assertEquals(false, viewModel.content().isSaving)
            collectJob.cancel()
        }

    @Test
    fun `given back clicked when handled then navigate back is emitted`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createLoadedViewModel(FakeUserSettingsRepository())
            val effects = mutableListOf<SettingsUiEffect>()
            val collectJob = launch { viewModel.uiEffect.collect { effects += it } }

            viewModel.onEvent(SettingsUiEvent.OnBackClicked)
            advanceUntilIdle()

            assertEquals(listOf<SettingsUiEffect>(SettingsUiEffect.NavigateBack), effects)
            collectJob.cancel()
        }

    @Test
    fun `given backup item clicked when handled then navigate to backup is emitted`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createLoadedViewModel(FakeUserSettingsRepository())
            val effects = mutableListOf<SettingsUiEffect>()
            val collectJob = launch { viewModel.uiEffect.collect { effects += it } }

            viewModel.onEvent(SettingsUiEvent.OnBackupClicked)
            advanceUntilIdle()

            assertEquals(listOf<SettingsUiEffect>(SettingsUiEffect.NavigateToBackup), effects)
            collectJob.cancel()
        }

    // Segurança (spec 16)
    private fun TestScope.collect(viewModel: SettingsViewModel): List<SettingsUiEffect> {
        val effects = mutableListOf<SettingsUiEffect>()
        backgroundScope.launch { viewModel.uiEffect.collect { effects += it } }
        runCurrent()
        return effects
    }

    @Test
    fun `given protection off when toggling the switch then authentication is requested and nothing is saved yet`() =
        runTest(mainRule.dispatcher) {
            val repository = FakeUserSettingsRepository()
            val viewModel = createLoadedViewModel(repository)
            val effects = collect(viewModel)

            viewModel.onEvent(SettingsUiEvent.AppLockEvent.OnToggled(enabled = true))
            advanceUntilIdle()
            runCurrent()

            assertEquals(listOf<SettingsUiEffect>(SettingsUiEffect.AppLockEffect.RequestAuthentication), effects)
            assertEquals(0, repository.saveCalls)
            assertEquals(false, viewModel.content().draft.appLockEnabled)
            verify(exactly = 1) { appLockController.suppressNextLock() }
        }

    @Test
    fun `given authentication requested to enable when it succeeds then the preference is saved and the app is unlocked`() =
        runTest(mainRule.dispatcher) {
            val repository = FakeUserSettingsRepository()
            val viewModel = createLoadedViewModel(repository)
            viewModel.onEvent(SettingsUiEvent.AppLockEvent.OnToggled(enabled = true))

            viewModel.onEvent(SettingsUiEvent.AppLockEvent.OnAuthResult(AuthResult.Success))
            advanceUntilIdle()
            runCurrent()

            assertEquals(true, repository.settings.value.appLockEnabled)
            assertEquals(true, viewModel.content().draft.appLockEnabled)
            verify(exactly = 1) { appLockController.unlock() }
        }

    @Test
    fun `given authentication requested to disable when it succeeds then the preference is turned off`() =
        runTest(mainRule.dispatcher) {
            val repository = FakeUserSettingsRepository(UserSettings(appLockEnabled = true))
            val viewModel = createLoadedViewModel(repository)
            viewModel.onEvent(SettingsUiEvent.AppLockEvent.OnToggled(enabled = false))

            viewModel.onEvent(SettingsUiEvent.AppLockEvent.OnAuthResult(AuthResult.Success))
            advanceUntilIdle()
            runCurrent()

            assertEquals(false, repository.settings.value.appLockEnabled)
            assertEquals(false, viewModel.content().draft.appLockEnabled)
        }

    @Test
    fun `given authentication requested when it is canceled then nothing is saved and the switch stays as it was`() =
        runTest(mainRule.dispatcher) {
            val repository = FakeUserSettingsRepository()
            val viewModel = createLoadedViewModel(repository)
            viewModel.onEvent(SettingsUiEvent.AppLockEvent.OnToggled(enabled = true))

            viewModel.onEvent(SettingsUiEvent.AppLockEvent.OnAuthResult(AuthResult.Canceled))
            advanceUntilIdle()
            runCurrent()

            assertEquals(0, repository.saveCalls)
            assertEquals(false, viewModel.content().draft.appLockEnabled)
        }

    @Test
    fun `given authentication requested when it fails then nothing is saved and an error is shown`() =
        runTest(mainRule.dispatcher) {
            val repository = FakeUserSettingsRepository()
            val viewModel = createLoadedViewModel(repository)
            val effects = collect(viewModel)
            viewModel.onEvent(SettingsUiEvent.AppLockEvent.OnToggled(enabled = true))

            viewModel.onEvent(SettingsUiEvent.AppLockEvent.OnAuthResult(AuthResult.LockedOut))
            advanceUntilIdle()
            runCurrent()

            assertEquals(0, repository.saveCalls)
            assertTrue(SettingsUiEffect.ShowError(R.string.lock_locked_out) in effects)
        }

    @Test
    fun `given an authentication result without a request when it succeeds then nothing is saved`() =
        runTest(mainRule.dispatcher) {
            val repository = FakeUserSettingsRepository()
            val viewModel = createLoadedViewModel(repository)

            viewModel.onEvent(SettingsUiEvent.AppLockEvent.OnAuthResult(AuthResult.Success))
            advanceUntilIdle()
            runCurrent()

            assertEquals(0, repository.saveCalls)
        }

    @Test
    fun `given a device without a secure screen lock when loaded then it is flagged and toggling requests nothing`() =
        runTest(mainRule.dispatcher) {
            deviceSecure = false
            val viewModel = createLoadedViewModel(FakeUserSettingsRepository())
            val effects = collect(viewModel)

            viewModel.onEvent(SettingsUiEvent.AppLockEvent.OnToggled(enabled = true))
            advanceUntilIdle()
            runCurrent()

            assertEquals(false, viewModel.content().isDeviceSecure)
            assertTrue(effects.isEmpty())
        }

    @Test
    fun `given the user configured a lock in the system settings when the screen resumes then the device becomes secure`() =
        runTest(mainRule.dispatcher) {
            deviceSecure = false
            val viewModel = createLoadedViewModel(FakeUserSettingsRepository())

            deviceSecure = true
            viewModel.onEvent(SettingsUiEvent.AppLockEvent.OnScreenResumed)

            assertEquals(true, viewModel.content().isDeviceSecure)
        }

    @Test
    fun `given the security shortcut when clicked then the open security settings effect is emitted`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createLoadedViewModel(FakeUserSettingsRepository())
            val effects = collect(viewModel)

            viewModel.onEvent(SettingsUiEvent.AppLockEvent.OnOpenSecuritySettingsClicked)
            advanceUntilIdle()
            runCurrent()

            assertEquals(listOf<SettingsUiEffect>(SettingsUiEffect.AppLockEffect.OpenSecuritySettings), effects)
        }

    @Test
    fun `given a timeout option when selected then it is saved without touching other unsaved edits`() =
        runTest(mainRule.dispatcher) {
            val repository = FakeUserSettingsRepository(savedSettings)
            val viewModel = createLoadedViewModel(repository)
            viewModel.onEvent(SettingsUiEvent.OnEmailChanged("rascunho@exemplo.com"))

            viewModel.onEvent(SettingsUiEvent.AppLockEvent.OnTimeoutSelected(AppLockTimeout.FIVE_MINUTES))
            advanceUntilIdle()
            runCurrent()

            assertEquals(savedSettings.copy(appLockTimeout = AppLockTimeout.FIVE_MINUTES), repository.settings.value)
            assertEquals("rascunho@exemplo.com", viewModel.content().draft.activeAccountEmail)
            assertEquals(AppLockTimeout.FIVE_MINUTES, viewModel.content().draft.appLockTimeout)
        }
}
