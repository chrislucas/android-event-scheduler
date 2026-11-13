package com.br.androideventscheduler.core.alarm.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.alarm.domain.model.RescheduleSummary
import com.br.androideventscheduler.core.alarm.domain.usecase.RescheduleAllAlarmsUseCase
import com.br.androideventscheduler.core.di.DispatcherNames
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

/**
 * Chama o receiver com cada ação, com o Koin sobrescrito (use case mockado e dispatcher `Unconfined`).
 * O `PendingResult` é mockado e injetado para conferir o `finish()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BootCompletedReceiverRobolectricTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val rescheduleAllAlarms = mockk<RescheduleAllAlarmsUseCase>()
    private val pendingResult = mockk<BroadcastReceiver.PendingResult>(relaxed = true)
    private val receiver = BootCompletedReceiver()

    private val overrides =
        module {
            single<CoroutineDispatcher>(named(DispatcherNames.IO)) { UnconfinedTestDispatcher() }
            factory { rescheduleAllAlarms }
        }

    @Before
    fun setUp() {
        coEvery { rescheduleAllAlarms() } returns Result.success(RescheduleSummary(scheduled = 1, skipped = 0, failed = 0))
        loadKoinModules(overrides)
    }

    @After
    fun tearDown() {
        unloadKoinModules(overrides)
    }

    private fun receive(intent: Intent) {
        // `setPendingResult` é @hide: o sistema o chama antes de `onReceive`; aqui via reflexão para `goAsync()` devolver o mock.
        BroadcastReceiver::class.java
            .getDeclaredMethod("setPendingResult", BroadcastReceiver.PendingResult::class.java)
            .invoke(receiver, pendingResult)
        receiver.onReceive(context, intent)
    }

    @Test
    fun `given boot completed when received then all alarms are rescheduled`() {
        receive(Intent(Intent.ACTION_BOOT_COMPLETED))

        coVerify(exactly = 1) { rescheduleAllAlarms() }
        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun `given package replaced when received then all alarms are rescheduled`() {
        receive(Intent(Intent.ACTION_MY_PACKAGE_REPLACED))

        coVerify(exactly = 1) { rescheduleAllAlarms() }
    }

    @Test
    fun `given timezone changed when received then all alarms are rescheduled`() {
        receive(Intent(Intent.ACTION_TIMEZONE_CHANGED))

        coVerify(exactly = 1) { rescheduleAllAlarms() }
    }

    @Test
    fun `given time set when received then all alarms are rescheduled`() {
        receive(Intent("android.intent.action.TIME_SET"))

        coVerify(exactly = 1) { rescheduleAllAlarms() }
    }

    @Test
    fun `given exact alarm permission state changed when received then all alarms are rescheduled`() {
        receive(Intent("android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"))

        coVerify(exactly = 1) { rescheduleAllAlarms() }
    }

    @Test
    fun `given locked boot completed when received then it is ignored`() {
        receive(Intent("android.intent.action.LOCKED_BOOT_COMPLETED"))

        coVerify(exactly = 0) { rescheduleAllAlarms() }
    }

    @Test
    fun `given an unknown action when received then it is ignored`() {
        receive(Intent("com.example.UNKNOWN"))

        coVerify(exactly = 0) { rescheduleAllAlarms() }
    }

    @Test
    fun `given an intent without action when received then it is ignored`() {
        receive(Intent())

        coVerify(exactly = 0) { rescheduleAllAlarms() }
    }

    @Test
    fun `given the reschedule returns a failure when received then the pending result is still finished`() {
        coEvery { rescheduleAllAlarms() } returns Result.failure(IllegalStateException("database unavailable"))

        receive(Intent(Intent.ACTION_BOOT_COMPLETED))

        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun `given the reschedule throws when received then the pending result is still finished`() {
        coEvery { rescheduleAllAlarms() } throws IllegalStateException("unexpected")

        receive(Intent(Intent.ACTION_BOOT_COMPLETED))

        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun `given the supported actions when listing them then locked boot is not among them`() {
        assertEquals(
            setOf(
                "android.intent.action.BOOT_COMPLETED",
                "android.intent.action.MY_PACKAGE_REPLACED",
                "android.intent.action.TIMEZONE_CHANGED",
                "android.intent.action.TIME_SET",
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            ),
            BootCompletedReceiver.SUPPORTED_ACTIONS,
        )
    }
}
