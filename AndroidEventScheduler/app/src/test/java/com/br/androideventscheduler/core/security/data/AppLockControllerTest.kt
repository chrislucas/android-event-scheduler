package com.br.androideventscheduler.core.security.data

import androidx.lifecycle.LifecycleOwner
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.testing.FakeUserSettingsRepository
import com.br.androideventscheduler.core.security.domain.AppLockState
import com.br.androideventscheduler.core.security.domain.AppLockTimeout
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppLockControllerTest {
    private val owner = mockk<LifecycleOwner>(relaxed = true)
    private var now = 1_000L
    private var deviceSecure = true

    private fun TestScope.controller(
        repository: FakeUserSettingsRepository,
    ): AppLockController =
        AppLockController(
            settingsRepository = repository,
            deviceSecurity = { deviceSecure },
            elapsedRealtime = { now },
            applicationScope = backgroundScope,
        ).also { runCurrent() }

    private fun enabled(timeout: AppLockTimeout = AppLockTimeout.ONE_MINUTE) =
        FakeUserSettingsRepository(UserSettings(appLockEnabled = true, appLockTimeout = timeout))

    private fun AppLockController.goToBackgroundFor(millis: Long) {
        onStop(owner)
        now += millis
        onStart(owner)
    }

    @Test
    fun `given preferences not read yet when created then state is pending`() =
        runTest {
            val controller =
                AppLockController(enabled(), { true }, { now }, backgroundScope)

            assertEquals(AppLockState.Pending, controller.lockState.value)
        }

    @Test
    fun `given protection disabled when the app starts then it starts unlocked`() =
        runTest {
            val controller = controller(FakeUserSettingsRepository())

            assertEquals(AppLockState.Unlocked, controller.lockState.value)
        }

    @Test
    fun `given protection enabled when the app starts then it starts locked`() =
        runTest {
            val controller = controller(enabled())

            assertEquals(AppLockState.Locked, controller.lockState.value)
        }

    @Test
    fun `given unlocked and one minute timeout when returning after 59 seconds then it stays unlocked`() =
        runTest {
            val controller = controller(enabled()).apply { unlock() }

            controller.goToBackgroundFor(59_000L)

            assertEquals(AppLockState.Unlocked, controller.lockState.value)
        }

    @Test
    fun `given unlocked and one minute timeout when returning exactly after one minute then it locks`() =
        runTest {
            val controller = controller(enabled()).apply { unlock() }

            controller.goToBackgroundFor(60_000L)

            assertEquals(AppLockState.Locked, controller.lockState.value)
        }

    @Test
    fun `given immediate timeout when going to background and back then it locks`() =
        runTest {
            val controller = controller(enabled(AppLockTimeout.IMMEDIATE)).apply { unlock() }

            controller.goToBackgroundFor(0L)

            assertEquals(AppLockState.Locked, controller.lockState.value)
        }

    @Test
    fun `given five minutes timeout when returning after four minutes then it stays unlocked`() =
        runTest {
            val controller = controller(enabled(AppLockTimeout.FIVE_MINUTES)).apply { unlock() }

            controller.goToBackgroundFor(240_000L)

            assertEquals(AppLockState.Unlocked, controller.lockState.value)
        }

    @Test
    fun `given protection disabled when returning from background then it never locks`() =
        runTest {
            val controller = controller(FakeUserSettingsRepository(UserSettings(appLockTimeout = AppLockTimeout.IMMEDIATE)))

            controller.goToBackgroundFor(10_000_000L)

            assertEquals(AppLockState.Unlocked, controller.lockState.value)
        }

    @Test
    fun `given the app never went to background when it starts then the first start does not lock`() =
        runTest {
            val controller = controller(enabled(AppLockTimeout.IMMEDIATE)).apply { unlock() }

            controller.onStart(owner)

            assertEquals(AppLockState.Unlocked, controller.lockState.value)
        }

    @Test
    fun `given suppress next lock when returning then it does not lock and the next return does`() =
        runTest {
            val controller = controller(enabled(AppLockTimeout.IMMEDIATE)).apply { unlock() }

            controller.suppressNextLock()
            controller.goToBackgroundFor(5_000L)
            val afterFirstReturn = controller.lockState.value
            controller.goToBackgroundFor(5_000L)

            assertEquals(AppLockState.Unlocked, afterFirstReturn)
            assertEquals(AppLockState.Locked, controller.lockState.value)
        }

    @Test
    fun `given suppress next lock when returning after more than two minutes then it locks`() =
        runTest {
            val controller = controller(enabled(AppLockTimeout.IMMEDIATE)).apply { unlock() }

            controller.suppressNextLock()
            controller.goToBackgroundFor(120_001L)

            assertEquals(AppLockState.Locked, controller.lockState.value)
        }

    @Test
    fun `given suppress next lock when returning exactly at two minutes then it does not lock`() =
        runTest {
            val controller = controller(enabled(AppLockTimeout.IMMEDIATE)).apply { unlock() }

            controller.suppressNextLock()
            controller.goToBackgroundFor(120_000L)

            assertEquals(AppLockState.Unlocked, controller.lockState.value)
        }

    @Test
    fun `given locked when the protection is turned off then it unlocks`() =
        runTest {
            val repository = enabled()
            val controller = controller(repository)

            repository.settings.value = repository.settings.value.copy(appLockEnabled = false)
            runCurrent()

            assertEquals(AppLockState.Unlocked, controller.lockState.value)
        }

    @Test
    fun `given the protection is turned on while unlocked when saved then it stays unlocked`() =
        runTest {
            val repository = FakeUserSettingsRepository()
            val controller = controller(repository)

            repository.settings.value = repository.settings.value.copy(appLockEnabled = true)
            runCurrent()

            assertEquals(AppLockState.Unlocked, controller.lockState.value)
        }

    @Test
    fun `given enabled on an insecure device when the app starts then it disables the protection and warns once`() =
        runTest {
            deviceSecure = false
            val repository = enabled()
            val controller = controller(repository)

            assertEquals(AppLockState.Unlocked, controller.lockState.value)
            assertFalse(repository.settings.value.appLockEnabled)
            controller.lockDisabledBecauseDeviceInsecure.first()
        }

    @Test
    fun `given enabled on a secure device when the app starts then no warning is emitted and the preference is kept`() =
        runTest {
            val repository = enabled()
            controller(repository)

            assertEquals(true, repository.settings.value.appLockEnabled)
            assertEquals(0, repository.saveCalls)
        }

    @Test
    fun `given the lock state changes when reading what was persisted then nothing about locked is saved`() =
        runTest {
            val repository = enabled()
            val controller = controller(repository)

            controller.unlock()
            controller.goToBackgroundFor(60_000L)

            assertEquals(0, repository.saveCalls)
        }
}
