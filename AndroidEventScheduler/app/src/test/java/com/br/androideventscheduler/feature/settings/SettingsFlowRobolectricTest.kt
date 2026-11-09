package com.br.androideventscheduler.feature.settings

import android.os.Looper
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.preferences.domain.UserSettingsRepository
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.settings.presentation.screen.SettingsScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Ponta a ponta com o grafo real: tela → ViewModel (Koin, dispatcher main) → repositório → DataStore.
 *
 * O DataStore conclui a gravação numa thread de I/O e retoma na main via `Handler.post`; no modo
 * PAUSED do Robolectric esses posts só rodam quando o looper principal é drenado — daí o `idle()`.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsFlowRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun waitUntilWithMainLooper(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    @Test
    fun `given settings screen when user changes email and saves then it is persisted and screen closes`() {
        var backCalls = 0
        composeRule.setContent { AndroidEventSchedulerTheme { SettingsScreen(onBack = { backCalls++ }, onOpenBackup = {}) } }
        waitUntilWithMainLooper {
            composeRule.onAllNodesWithTag(SemanticsTags.SETTINGS_ACCOUNT_EMAIL_INPUT).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(SemanticsTags.SETTINGS_ACCOUNT_EMAIL_INPUT).performTextReplacement("fluxo@exemplo.com")
        composeRule.onNodeWithTag(SemanticsTags.SETTINGS_SAVE_BUTTON).performScrollTo().performClick()
        waitUntilWithMainLooper { backCalls == 1 }

        val repository = GlobalContext.get().get<UserSettingsRepository>()
        assertEquals("fluxo@exemplo.com", runBlocking { repository.current().activeAccountEmail })
    }
}
