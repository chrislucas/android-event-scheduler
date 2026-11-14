package com.br.androideventscheduler

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
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
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Ponta a ponta (specs 05 + 06 + 03) com a `MainActivity` e o grafo real (DataStore e Room): Home → gaveta →
 * ocultar `Family` some com o item da Home → atalho Configurações abre a tela de configurações; e o botão
 * voltar do sistema fecha a gaveta sem sair da Home.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR-w411dp-h891dp")
class HomeDrawerToSettingsRobolectricTest {
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
    fun `given a family item when hiding family in the drawer and opening settings then it disappears and settings opens`() {
        val repository = GlobalContext.get().get<ScheduleRepository>()
        runBlocking {
            repository.upsert(
                scheduleItem(
                    id = "family-lunch",
                    title = "Almoço",
                    startDate = LocalDate.now(),
                ).copy(calendarSource = CalendarSource.FAMILY),
            )
        }
        val itemTag = SemanticsTags.HOME_SCHEDULE_ITEM_PREFIX + "family-lunch"

        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilWithMainLooper { hasNode(itemTag) && hasNode(SemanticsTags.HOME_MENU_BUTTON) }
            composeRule.onNodeWithTag(SemanticsTags.HOME_MENU_BUTTON).performClick()

            val familyFilter = SemanticsTags.HOME_DRAWER_FILTER_PREFIX + CalendarSource.FAMILY.id
            waitUntilWithMainLooper { hasNode(familyFilter) }
            composeRule.onNodeWithTag(familyFilter).performClick()
            waitUntilWithMainLooper { !hasNode(itemTag) && hasNode(SemanticsTags.HOME_EMPTY_DAY) }

            composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SETTINGS_BUTTON).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.SETTINGS_BACK_BUTTON) && !hasNode(SemanticsTags.HOME_MENU_BUTTON) }
        }
    }

    @Test
    fun `given the drawer open when pressing system back then the drawer closes and the home stays`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_MENU_BUTTON) }
            composeRule.onNodeWithTag(SemanticsTags.HOME_MENU_BUTTON).performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SHEET).assertIsDisplayed()

            scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
            composeRule.waitForIdle()

            composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SHEET).assertIsNotDisplayed()
            composeRule.onNodeWithTag(SemanticsTags.HOME_MENU_BUTTON).assertIsDisplayed().performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SHEET).assertIsDisplayed()
        }
    }
}
