package com.br.androideventscheduler.feature.settings.presentation.viewmodel

import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.preferences.domain.ThemeMode
import com.br.androideventscheduler.core.preferences.domain.ThemePreferenceRepository
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.testing.FakeThemePreferenceRepository
import com.br.androideventscheduler.core.preferences.testing.FakeUserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.testing.MainDispatcherRule
import com.br.androideventscheduler.feature.settings.presentation.effect.SettingsUiEffect
import com.br.androideventscheduler.feature.settings.presentation.event.SettingsUiEvent
import com.br.androideventscheduler.feature.settings.presentation.state.SettingsUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

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

    private fun TestScope.createLoadedViewModel(
        repository: FakeUserSettingsRepository,
        themeRepository: ThemePreferenceRepository = FakeThemePreferenceRepository(),
    ): SettingsViewModel {
        val viewModel = SettingsViewModel(repository, themeRepository, mainRule.dispatcher)
        advanceUntilIdle()
        return viewModel
    }

    private fun SettingsViewModel.content(): SettingsUiState.Content = uiState.value as SettingsUiState.Content

    @Test
    fun `given saved settings when view model starts then it loads them into the draft`() =
        runTest(mainRule.dispatcher) {
            val viewModel =
                SettingsViewModel(FakeUserSettingsRepository(savedSettings), FakeThemePreferenceRepository(), mainRule.dispatcher)
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

    @Test
    fun `given a saved theme when view model starts then the state exposes it outside the draft`() =
        runTest(mainRule.dispatcher) {
            val viewModel =
                createLoadedViewModel(FakeUserSettingsRepository(savedSettings), FakeThemePreferenceRepository(ThemeMode.SYSTEM))

            assertEquals(ThemeMode.SYSTEM, viewModel.content().themeMode)
            assertEquals(savedSettings, viewModel.content().draft)
        }

    @Test
    fun `given light selected when handled then it is saved in the theme repository and the state becomes light`() =
        runTest(mainRule.dispatcher) {
            val themeRepository = FakeThemePreferenceRepository()
            val viewModel = createLoadedViewModel(FakeUserSettingsRepository(), themeRepository)

            viewModel.onEvent(SettingsUiEvent.AppearanceEvent.OnThemeModeSelected(ThemeMode.LIGHT))
            advanceUntilIdle()

            assertEquals(listOf(ThemeMode.LIGHT), themeRepository.setCalls)
            assertEquals(ThemeMode.LIGHT, viewModel.content().themeMode)
        }

    @Test
    fun `given an unsaved email edit when choosing a theme then the draft is kept and nothing from the form is saved`() =
        runTest(mainRule.dispatcher) {
            val userRepository = FakeUserSettingsRepository(savedSettings)
            val viewModel = createLoadedViewModel(userRepository)
            viewModel.onEvent(SettingsUiEvent.OnEmailChanged("novo@exemplo.com"))
            viewModel.onEvent(SettingsUiEvent.OnSnoozeSelected(15))

            viewModel.onEvent(SettingsUiEvent.AppearanceEvent.OnThemeModeSelected(ThemeMode.LIGHT))
            advanceUntilIdle()

            assertEquals("novo@exemplo.com", viewModel.content().draft.activeAccountEmail)
            assertEquals(15, viewModel.content().draft.defaultSnoozeMinutes)
            assertEquals(0, userRepository.saveCalls)
            assertEquals(savedSettings, userRepository.settings.value)
        }

    @Test
    fun `given the theme changes elsewhere when observing then the state follows and the draft is untouched`() =
        runTest(mainRule.dispatcher) {
            val themeRepository = FakeThemePreferenceRepository()
            val viewModel = createLoadedViewModel(FakeUserSettingsRepository(savedSettings), themeRepository)
            viewModel.onEvent(SettingsUiEvent.OnEmailChanged("novo@exemplo.com"))

            themeRepository.mode.value = ThemeMode.SYSTEM
            advanceUntilIdle()

            assertEquals(ThemeMode.SYSTEM, viewModel.content().themeMode)
            assertEquals("novo@exemplo.com", viewModel.content().draft.activeAccountEmail)
        }

    @Test
    fun `given a theme choice when the draft is saved then the theme stays selected`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createLoadedViewModel(FakeUserSettingsRepository())
            viewModel.onEvent(SettingsUiEvent.AppearanceEvent.OnThemeModeSelected(ThemeMode.LIGHT))
            advanceUntilIdle()

            viewModel.onEvent(SettingsUiEvent.OnSaveClicked)
            advanceUntilIdle()

            assertEquals(ThemeMode.LIGHT, viewModel.content().themeMode)
        }

    @Test
    fun `given a failure writing the theme when choosing one then an error is emitted and the state keeps the old theme`() =
        runTest(mainRule.dispatcher) {
            val failing =
                object : ThemePreferenceRepository {
                    override fun observe() = flowOf(ThemeMode.DARK)

                    override suspend fun set(mode: ThemeMode) = throw IOException("disk full")
                }
            val viewModel = createLoadedViewModel(FakeUserSettingsRepository(), failing)
            val effects = mutableListOf<SettingsUiEffect>()
            val collectJob = launch { viewModel.uiEffect.collect { effects += it } }

            viewModel.onEvent(SettingsUiEvent.AppearanceEvent.OnThemeModeSelected(ThemeMode.LIGHT))
            advanceUntilIdle()

            assertEquals(listOf<SettingsUiEffect>(SettingsUiEffect.ShowError(R.string.settings_theme_save_error)), effects)
            assertEquals(ThemeMode.DARK, viewModel.content().themeMode)
            collectJob.cancel()
        }
}
