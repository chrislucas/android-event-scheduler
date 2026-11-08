package com.br.androideventscheduler

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.app.DeepLinkState
import com.br.androideventscheduler.core.alarm.data.AlarmIntents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Registro do id do intent no [DeepLinkState]. A abertura efetiva do detalhe (`ScheduleDetail`) depende da
 * entry da spec 10 e é coberta por [com.br.androideventscheduler.app.DeepLinkEffectRobolectricTest] no nível da pilha.
 */
@RunWith(RobolectricTestRunner::class)
class MainActivityDeepLinkRobolectricTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val deepLinkState: DeepLinkState get() = GlobalContext.get().get()

    private fun mainIntent(scheduleId: String? = null): Intent =
        Intent(context, MainActivity::class.java).also { intent ->
            scheduleId?.let { intent.putExtra(AlarmIntents.EXTRA_SCHEDULE_ID, it) }
        }

    @Test
    fun `given a notification intent with schedule id when the activity is created then the id is registered`() {
        ActivityScenario.launch<MainActivity>(mainIntent("abc")).use {
            assertEquals("abc", deepLinkState.pendingScheduleId.value)
        }
    }

    @Test
    fun `given a plain launcher intent when the activity is created then no id is registered`() {
        ActivityScenario.launch<MainActivity>(mainIntent()).use {
            assertNull(deepLinkState.pendingScheduleId.value)
        }
    }

    @Test
    fun `given the activity is running when a new notification intent arrives then the new id is registered`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java, mainIntent()).setup()

        controller.newIntent(mainIntent("second"))

        assertEquals("second", deepLinkState.pendingScheduleId.value)
        controller.pause().stop().destroy()
    }

    @Test
    fun `given the id was consumed when the activity is recreated then the same intent does not register it again`() {
        ActivityScenario.launch<MainActivity>(mainIntent("abc")).use { scenario ->
            deepLinkState.consume("abc")

            scenario.recreate()

            assertNull(deepLinkState.pendingScheduleId.value)
        }
    }

    @Test
    fun `given an intent replayed from recents history when the activity is created then the stale id is ignored`() {
        val replayedIntent = mainIntent("stale").addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)

        ActivityScenario.launch<MainActivity>(replayedIntent).use {
            assertNull(deepLinkState.pendingScheduleId.value)
        }
    }
}
