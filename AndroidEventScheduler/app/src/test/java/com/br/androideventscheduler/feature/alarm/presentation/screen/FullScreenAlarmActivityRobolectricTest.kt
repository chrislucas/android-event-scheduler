package com.br.androideventscheduler.feature.alarm.presentation.screen

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Looper
import android.view.WindowManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.alarm.data.AlarmIntents
import com.br.androideventscheduler.core.alarm.data.notification.NotificationChannels
import com.br.androideventscheduler.core.alarm.domain.model.AlarmRequest
import com.br.androideventscheduler.core.alarm.domain.model.AlertPresentationMode
import com.br.androideventscheduler.core.alarm.domain.scheduler.AlarmScheduler
import com.br.androideventscheduler.core.alarm.domain.usecase.SnoozeAlarmUseCase
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import com.br.androideventscheduler.core.preferences.testing.FakeUserSettingsRepository
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.FakeClock
import com.br.androideventscheduler.core.ui.SemanticsTags
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Sobe a `FullScreenAlarmActivity` real (manifest mesclado) com o Koin sobrescrito por fakes. O que depende do
 * sistema de verdade (exibir sobre a tela de bloqueio, ligar a tela) não é verificável na JVM: aqui valem a
 * semântica, a navegação, o cancelamento da notificação e a configuração declarada.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR")
class FullScreenAlarmActivityRobolectricTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val application: Application = ApplicationProvider.getApplicationContext()
    private val context: Context = application
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // 2026-11-07 12:00 em São Paulo.
    private val clock = FakeClock(Instant.parse("2026-11-07T15:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val scheduleRepository = mockk<ScheduleRepository>()
    private val settingsRepository = FakeUserSettingsRepository(UserSettings(defaultSnoozeMinutes = 10))
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)

    private val overrides =
        module {
            single<Clock> { clock }
            single<ScheduleRepository> { scheduleRepository }
            single<UserSettingsRepository> { settingsRepository }
            single<AlarmScheduler> { alarmScheduler }
            single { SnoozeAlarmUseCase(scheduleRepository = get(), alarmScheduler = get(), clock = get()) }
        }

    private val upcomingEvent =
        scheduleItem(
            id = "item-1",
            title = "Aula de Kotlin",
            startDate = LocalDate.of(2026, 11, 7),
            startTime = LocalTime.of(12, 15),
            endTime = LocalTime.of(13, 45),
            location = "Sala 3",
            notes = "Levar o notebook",
        )

    @Before
    fun setUp() {
        loadKoinModules(overrides)
        coEvery { scheduleRepository.getById("item-1") } returns upcomingEvent
        NotificationChannels.ensureCreated(context)
    }

    @After
    fun tearDown() {
        unloadKoinModules(overrides)
    }

    private fun alarmIntent(scheduleId: String? = "item-1"): Intent =
        AlarmIntents
            .fullScreenIntent(context, scheduleId ?: "ignored")
            .setClass(context, FullScreenAlarmActivity::class.java)
            .also { intent -> if (scheduleId == null) intent.removeExtra(AlarmIntents.EXTRA_SCHEDULE_ID) }

    private fun launch(scheduleId: String? = "item-1"): ActivityScenario<FullScreenAlarmActivity> =
        ActivityScenario.launch<FullScreenAlarmActivity>(alarmIntent(scheduleId))

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ActivityScenario<FullScreenAlarmActivity>.isFinishing(): Boolean {
        var finishing = false
        onActivity { finishing = it.isFinishing }
        return finishing
    }

    private fun waitUntilFinishing(scenario: ActivityScenario<FullScreenAlarmActivity>) {
        composeRule.waitUntil(timeoutMillis = WAIT_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            scenario.isFinishing()
        }
    }

    private fun postNotification(scheduleId: String = "item-1") {
        val notification =
            NotificationCompat
                .Builder(context, NotificationChannels.EVENTS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle("Aula")
                .build()
        notificationManager.notify(AlarmIntents.notificationId(scheduleId), notification)
    }

    private fun postedNotification(scheduleId: String = "item-1"): Notification? =
        shadowOf(notificationManager).getNotification(AlarmIntents.notificationId(scheduleId))

    @Test
    fun `given the system intent when launched then the event is shown with badge title time location and notes`() {
        launch().use {
            waitForTag(SemanticsTags.ALARM_FULLSCREEN_TITLE)

            composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_BADGE).assertTextContains("Evento em 15 minutos")
            composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_TITLE).assertTextContains("Aula de Kotlin")
            composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_TIME).assertTextContains("12:15 – 13:45")
            composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_LOCATION).assertTextContains("Sala 3")
            composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_NOTES).assertTextContains("Levar o notebook")
        }
    }

    @Test
    fun `given default snooze of fifteen in settings when launched then the fifteen chip is selected`() {
        settingsRepository.settings.value = UserSettings(defaultSnoozeMinutes = 15)

        launch().use {
            waitForTag(SemanticsTags.ALARM_SNOOZE_CHIP_PREFIX + 15)

            composeRule.onNodeWithTag(SemanticsTags.ALARM_SNOOZE_CHIP_PREFIX + 15).assertIsSelected()
        }
    }

    @Test
    fun `given snooze tapped when processed then it schedules now plus the chosen minutes cancels the notification and finishes`() {
        postNotification()
        assertNotNull(postedNotification())

        launch().use { scenario ->
            waitForTag(SemanticsTags.ALARM_FULLSCREEN_SNOOZE_BUTTON)
            composeRule.onNodeWithTag(SemanticsTags.ALARM_SNOOZE_CHIP_PREFIX + 5).performScrollTo().performClick()
            composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_SNOOZE_BUTTON).performScrollTo().performClick()
            waitUntilFinishing(scenario)

            val requestSlot = slot<AlarmRequest>()
            verify(exactly = 1) { alarmScheduler.schedule(capture(requestSlot)) }
            assertEquals(clock.millis() + 5 * 60_000L, requestSlot.captured.triggerAtEpochMillis)
            assertEquals("item-1", requestSlot.captured.scheduleId)
            assertEquals(AlertPresentationMode.FULL_SCREEN_EVENT, requestSlot.captured.presentationMode)
            assertNull(postedNotification())
        }
    }

    @Test
    fun `given the scheduler fails when snoozing then the screen stays open with the error and the notification stays`() {
        postNotification()
        every { alarmScheduler.schedule(any()) } throws SecurityException("denied")

        launch().use { scenario ->
            waitForTag(SemanticsTags.ALARM_FULLSCREEN_SNOOZE_BUTTON)
            composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_SNOOZE_BUTTON).performScrollTo().performClick()
            waitForTag(SemanticsTags.ALARM_FULLSCREEN_ERROR)

            composeRule
                .onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_ERROR)
                .assertTextContains(context.getString(R.string.alarm_snooze_error))
            assertFalse(scenario.isFinishing())
            assertNotNull(postedNotification())
        }
    }

    @Test
    fun `given dismiss tapped when processed then the notification is cancelled nothing is scheduled and it finishes`() {
        postNotification()

        launch().use { scenario ->
            waitForTag(SemanticsTags.ALARM_FULLSCREEN_DISMISS_BUTTON)
            composeRule.onNodeWithTag(SemanticsTags.ALARM_FULLSCREEN_DISMISS_BUTTON).performScrollTo().performClick()
            waitUntilFinishing(scenario)

            assertNull(postedNotification())
            verify(exactly = 0) { alarmScheduler.schedule(any()) }
        }
    }

    @Test
    fun `given back pressed when processed then it finishes and the notification is kept`() {
        postNotification()

        launch().use { scenario ->
            waitForTag(SemanticsTags.ALARM_FULLSCREEN_TITLE)
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitUntilFinishing(scenario)

            assertNotNull(postedNotification())
            verify(exactly = 0) { alarmScheduler.schedule(any()) }
        }
    }

    @Test
    fun `given a schedule that no longer exists when launched then it finishes by itself`() {
        coEvery { scheduleRepository.getById("gone") } returns null

        launch("gone").use { scenario ->
            waitUntilFinishing(scenario)

            assertTrue(scenario.isFinishing())
        }
    }

    @Test
    fun `given an intent without schedule id when launched then it finishes immediately`() {
        launch(scheduleId = null).use { scenario ->
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }

    @Test
    fun `given a new alarm for another item when the screen is open then it shows the new item`() {
        coEvery { scheduleRepository.getById("item-2") } returns upcomingEvent.copy(id = "item-2", title = "Dentista")

        launch().use { scenario ->
            waitForTag(SemanticsTags.ALARM_FULLSCREEN_TITLE)
            scenario.onActivity { activity ->
                InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(activity, alarmIntent("item-2"))
            }
            composeRule.waitUntil(timeoutMillis = WAIT_MILLIS) {
                shadowOf(Looper.getMainLooper()).idle()
                titleIs("Dentista")
            }

            assertFalse(scenario.isFinishing())
        }
    }

    @Test
    fun `given a new alarm for the same item when the screen is open then the content is reloaded`() {
        launch().use { scenario ->
            waitForTag(SemanticsTags.ALARM_FULLSCREEN_TITLE)
            coEvery { scheduleRepository.getById("item-1") } returns upcomingEvent.copy(title = "Aula remarcada")
            scenario.onActivity { activity ->
                InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(activity, alarmIntent("item-1"))
            }
            composeRule.waitUntil(timeoutMillis = WAIT_MILLIS) {
                shadowOf(Looper.getMainLooper()).idle()
                titleIs("Aula remarcada")
            }
        }
    }

    @Test
    fun `given rotation after a new alarm when the activity is recreated then it keeps showing the latest item`() {
        coEvery { scheduleRepository.getById("item-2") } returns upcomingEvent.copy(id = "item-2", title = "Dentista")

        launch().use { scenario ->
            waitForTag(SemanticsTags.ALARM_FULLSCREEN_TITLE)
            scenario.onActivity { activity ->
                InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(activity, alarmIntent("item-2"))
            }
            composeRule.waitUntil(timeoutMillis = WAIT_MILLIS) {
                shadowOf(Looper.getMainLooper()).idle()
                titleIs("Dentista")
            }

            scenario.recreate()

            composeRule.waitUntil(timeoutMillis = WAIT_MILLIS) {
                shadowOf(Looper.getMainLooper()).idle()
                titleIs("Dentista")
            }
        }
    }

    @Test
    fun `given the screen is shown when inspecting the window then it keeps the screen on`() {
        launch().use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
            }
        }
    }

    @Test
    @Config(sdk = [26])
    @Suppress("DEPRECATION")
    fun `given api 26 when shown then the legacy window flags show it over the lock screen and turn the screen on`() {
        launch().use { scenario ->
            scenario.onActivity { activity ->
                val flags = activity.window.attributes.flags
                assertTrue(flags and WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED != 0)
                assertTrue(flags and WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON != 0)
                assertTrue(flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
            }
        }
    }

    @Test
    fun `given merged manifest when reading the activity then it is private single top in its own task and out of recents`() {
        val activityInfo = context.packageManager.getActivityInfo(ComponentName(context, FullScreenAlarmActivity::class.java), 0)

        assertFalse(activityInfo.exported)
        assertEquals(ActivityInfo.LAUNCH_SINGLE_TOP, activityInfo.launchMode)
        // `taskAffinity=""` (sem afinidade) chega como nulo/vazio; o padrão seria o nome do pacote.
        assertTrue(activityInfo.taskAffinity.isNullOrEmpty())
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
    }

    @Test
    fun `given merged manifest when resolving the full screen action then only the activity answers and the filter has no data`() {
        val fullScreenIntent = AlarmIntents.fullScreenIntent(context, "item-1")

        val resolved = context.packageManager.queryIntentActivities(fullScreenIntent, 0)
        val filters =
            shadowOf(context.packageManager).getIntentFiltersForActivity(ComponentName(context, FullScreenAlarmActivity::class.java))

        assertEquals(listOf(FullScreenAlarmActivity::class.java.name), resolved.map { it.activityInfo.name })
        assertEquals(1, filters.size)
        assertEquals(0, filters.single().countDataSchemes())
        assertEquals(0, filters.single().countDataTypes())
        assertTrue(filters.single().hasCategory(Intent.CATEGORY_DEFAULT))
    }

    private fun titleIs(expected: String): Boolean =
        composeRule
            .onAllNodes(hasTestTag(SemanticsTags.ALARM_FULLSCREEN_TITLE))
            .fetchSemanticsNodes()
            .any { node ->
                node.config[SemanticsProperties.Text].joinToString { it.text } == expected
            }

    private companion object {
        const val WAIT_MILLIS = 5_000L
    }
}
