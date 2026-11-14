package com.br.androideventscheduler.feature.backup

import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import com.br.androideventscheduler.MainActivity
import com.br.androideventscheduler.core.ui.SemanticsTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Ponta a ponta (specs 03 + 06 + 17): Home → gaveta → Configurações → "Importar e exportar agenda" → tela de backup → voltar. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR-w411dp-h891dp")
class OpenBackupFromSettingsRobolectricTest {
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
    fun `given the settings screen when tapping import and export then the backup screen opens and back returns to settings`() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_MENU_BUTTON) }
            composeRule.onNodeWithTag(SemanticsTags.HOME_MENU_BUTTON).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_DRAWER_SETTINGS_BUTTON) }
            composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SETTINGS_BUTTON).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.SETTINGS_BACKUP_ITEM) }

            composeRule.onNodeWithTag(SemanticsTags.SETTINGS_BACKUP_ITEM).performScrollTo().performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.BACKUP_EXPORT_SHARE_BUTTON) }

            composeRule.onNodeWithTag(SemanticsTags.BACKUP_BACK_BUTTON).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.SETTINGS_BACKUP_ITEM) && !hasNode(SemanticsTags.BACKUP_BACK_BUTTON) }
        }
    }
}
