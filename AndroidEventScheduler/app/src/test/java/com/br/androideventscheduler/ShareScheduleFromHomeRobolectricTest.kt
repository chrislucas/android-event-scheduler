package com.br.androideventscheduler

import android.app.Application
import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.ui.SemanticsTags
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Ponta a ponta (specs 05 + 10 + 12) com a `MainActivity` e o grafo real: Home → detalhe → Compartilhar →
 * seletor do sistema (`ACTION_CHOOSER` com `ACTION_SEND` `text/plain`) com o resumo do agendamento.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR-w411dp-h891dp")
class ShareScheduleFromHomeRobolectricTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private fun waitUntilWithMainLooper(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun hasNode(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `given a schedule today when sharing it from its detail then the system chooser carries its summary`() {
        val repository = GlobalContext.get().get<ScheduleRepository>()
        runBlocking {
            repository.upsert(
                scheduleItem(
                    id = "to-share",
                    title = "Reunião",
                    startDate = LocalDate.now(),
                    location = "Sala 3",
                    notes = "Levar o notebook",
                ),
            )
        }
        val itemTag = SemanticsTags.HOME_SCHEDULE_ITEM_PREFIX + "to-share"
        val application = ApplicationProvider.getApplicationContext<Application>()

        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilWithMainLooper { hasNode(itemTag) }
            composeRule.onNodeWithTag(itemTag).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.DETAIL_SHARE_BUTTON) }

            composeRule.onNodeWithTag(SemanticsTags.DETAIL_SHARE_BUTTON).performClick()
            waitUntilWithMainLooper { shadowOf(application).peekNextStartedActivity() != null }

            val chooser = shadowOf(application).nextStartedActivity

            @Suppress("DEPRECATION")
            val sendIntent = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            assertEquals(Intent.ACTION_CHOOSER, chooser.action)
            assertNotNull(sendIntent)
            assertEquals(Intent.ACTION_SEND, sendIntent!!.action)
            assertEquals("text/plain", sendIntent.type)
            assertEquals("RememberMe: Reunião", sendIntent.getStringExtra(Intent.EXTRA_SUBJECT))
            val body = sendIntent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            val lines = body.lines()
            assertEquals("📅 Reunião", lines.first())
            assertTrue(lines.contains("📍 Local: Sala 3"))
            assertTrue(lines.contains("📝 Notas: Levar o notebook"))
            assertFalse(body.contains("to-share"))
        }
    }
}
