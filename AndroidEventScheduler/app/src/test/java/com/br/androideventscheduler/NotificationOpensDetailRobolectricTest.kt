package com.br.androideventscheduler

import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.alarm.data.AlarmIntents
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.ui.SemanticsTags
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Ponta a ponta (specs 04 + 10): o intent da notificação abre a `MainActivity`, a Splash termina e o
 * detalhe do agendamento aparece sobre a Home.
 */
@RunWith(RobolectricTestRunner::class)
class NotificationOpensDetailRobolectricTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `given a notification intent for an existing schedule when the app opens then the detail is shown after the splash`() {
        val repository = GlobalContext.get().get<ScheduleRepository>()
        runBlocking { repository.upsert(scheduleItem(id = "from-notification", title = "Consulta médica")) }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationIntent =
            Intent(context, MainActivity::class.java).putExtra(AlarmIntents.EXTRA_SCHEDULE_ID, "from-notification")

        ActivityScenario.launch<MainActivity>(notificationIntent).use {
            composeRule.waitUntil(timeoutMillis = 10_000) {
                shadowOf(Looper.getMainLooper()).idle()
                composeRule.onAllNodesWithTag(SemanticsTags.DETAIL_TITLE).fetchSemanticsNodes().isNotEmpty()
            }

            composeRule.onNodeWithTag(SemanticsTags.DETAIL_TITLE).assertTextEquals("Consulta médica")
        }
    }
}
