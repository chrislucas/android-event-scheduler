package com.br.androideventscheduler

import android.graphics.drawable.ColorDrawable
import android.os.Looper
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.core.app.ActivityScenario
import com.br.androideventscheduler.core.preferences.domain.ThemeMode
import com.br.androideventscheduler.core.preferences.domain.ThemePreferenceRepository
import com.br.androideventscheduler.core.ui.SemanticsTags
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Ponta a ponta da spec 19 com a `MainActivity` e o grafo real (DataStore): o tema é observado pelo estilo das barras
 * do sistema (`isAppearanceLightStatusBars`) e pelo fundo da janela, porque `captureToImage` expira no Robolectric
 * deste projeto. Qualificadores na ordem do Android: idioma, `w`, `h`, depois `night`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR-w411dp-h891dp")
class AppThemeRobolectricTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val themeRepository get() = GlobalContext.get().get<ThemePreferenceRepository>()

    private fun waitUntilWithMainLooper(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun hasNode(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun ActivityScenario<MainActivity>.hasLightStatusBarIcons(): Boolean {
        var light = false
        onActivity { activity ->
            light = WindowInsetsControllerCompat(activity.window, activity.window.decorView).isAppearanceLightStatusBars
        }
        return light
    }

    private fun ActivityScenario<MainActivity>.windowBackground(): Int {
        var color = 0
        onActivity { activity -> color = (activity.window.decorView.background as ColorDrawable).color }
        return color
    }

    private fun ActivityScenario<MainActivity>.currentActivity(): MainActivity {
        lateinit var current: MainActivity
        onActivity { current = it }
        return current
    }

    private fun ActivityScenario<MainActivity>.openSettingsFromHome() {
        waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_MENU_BUTTON) }
        composeRule.onNodeWithTag(SemanticsTags.HOME_MENU_BUTTON).performClick()
        waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_DRAWER_SETTINGS_BUTTON) }
        composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SETTINGS_BUTTON).performClick()
        waitUntilWithMainLooper { hasNode(SemanticsTags.SETTINGS_BACK_BUTTON) && !hasNode(SemanticsTags.HOME_MENU_BUTTON) }
    }

    private fun chooseThemeInSettings(mode: ThemeMode) {
        composeRule.onNodeWithTag(SemanticsTags.SETTINGS_THEME_OPTION_PREFIX + mode.name).performScrollTo().performClick()
    }

    @Test
    fun `given no saved preference when launched then the app is dark with light status bar icons`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_MENU_BUTTON) }

            assertFalse(scenario.hasLightStatusBarIcons())
            assertEquals(DARK_BACKGROUND, scenario.windowBackground())
        }
    }

    @Test
    fun `given the dark default when choosing light in settings then the same activity turns light and the back stack is intact`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.openSettingsFromHome()
            val activityBefore = scenario.currentActivity()
            composeRule.onNodeWithTag(SemanticsTags.SETTINGS_THEME_OPTION_PREFIX + ThemeMode.DARK.name).assertIsSelected()

            chooseThemeInSettings(ThemeMode.LIGHT)
            waitUntilWithMainLooper { scenario.hasLightStatusBarIcons() }

            assertTrue(scenario.hasLightStatusBarIcons())
            assertEquals(LIGHT_BACKGROUND, scenario.windowBackground())
            assertSame("a troca de tema não pode recriar a Activity", activityBefore, scenario.currentActivity())
            composeRule.onNodeWithTag(SemanticsTags.SETTINGS_THEME_OPTION_PREFIX + ThemeMode.LIGHT.name).assertIsSelected()
            assertEquals(ThemeMode.LIGHT, runBlocking { themeRepository.observe().first() })

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_MENU_BUTTON) }
            assertTrue(scenario.hasLightStatusBarIcons())
            assertSame(activityBefore, scenario.currentActivity())
        }
    }

    @Test
    fun `given an unsaved email edit when choosing a theme then the typed text stays in the field`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.openSettingsFromHome()
            composeRule
                .onNodeWithTag(
                    SemanticsTags.SETTINGS_ACCOUNT_EMAIL_INPUT,
                ).performScrollTo()
                .performTextReplacement("bia@exemplo.com")

            chooseThemeInSettings(ThemeMode.LIGHT)
            waitUntilWithMainLooper { scenario.hasLightStatusBarIcons() }

            composeRule.onNodeWithTag(SemanticsTags.SETTINGS_ACCOUNT_EMAIL_INPUT).assertTextContains("bia@exemplo.com")
        }
    }

    @Test
    fun `given light chosen when the activity is recreated then it stays light`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.openSettingsFromHome()
            chooseThemeInSettings(ThemeMode.LIGHT)
            waitUntilWithMainLooper { scenario.hasLightStatusBarIcons() }

            scenario.recreate()

            waitUntilWithMainLooper { hasNode(SemanticsTags.SETTINGS_BACK_BUTTON) && scenario.hasLightStatusBarIcons() }
            assertEquals(LIGHT_BACKGROUND, scenario.windowBackground())
        }
    }

    @Test
    fun `given light chosen when closing and opening a new activity then it starts light`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.openSettingsFromHome()
            chooseThemeInSettings(ThemeMode.LIGHT)
            waitUntilWithMainLooper { scenario.hasLightStatusBarIcons() }
        }

        ActivityScenario.launch(MainActivity::class.java).use { reopened ->
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_MENU_BUTTON) && reopened.hasLightStatusBarIcons() }

            assertEquals(LIGHT_BACKGROUND, reopened.windowBackground())
        }
    }

    @Test
    fun `given light saved before launch when the app starts then the first content is already light`() {
        runBlocking { themeRepository.set(ThemeMode.LIGHT) }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_MENU_BUTTON) }

            assertTrue(scenario.hasLightStatusBarIcons())
            assertEquals(LIGHT_BACKGROUND, scenario.windowBackground())
        }
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h891dp-night")
    fun `given follow system and the system in night mode when launched then the app is dark`() {
        runBlocking { themeRepository.set(ThemeMode.SYSTEM) }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_MENU_BUTTON) }

            assertFalse(scenario.hasLightStatusBarIcons())
            assertEquals(DARK_BACKGROUND, scenario.windowBackground())
        }
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h891dp-notnight")
    fun `given follow system and the system in day mode when launched then the app is light`() {
        runBlocking { themeRepository.set(ThemeMode.SYSTEM) }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_MENU_BUTTON) && scenario.hasLightStatusBarIcons() }

            assertEquals(LIGHT_BACKGROUND, scenario.windowBackground())
        }
    }

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h891dp-notnight")
    fun `given no saved preference and the system in day mode when launched then the app is still dark`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_MENU_BUTTON) }

            assertFalse(scenario.hasLightStatusBarIcons())
            assertEquals(DARK_BACKGROUND, scenario.windowBackground())
        }
    }

    @Test
    fun `given the preference is still being read when launched then no content is composed until it arrives`() {
        val gate = CompletableDeferred<Unit>()
        val gatedRepository =
            object : ThemePreferenceRepository {
                override fun observe(): Flow<ThemeMode> =
                    flow {
                        gate.await()
                        emit(ThemeMode.LIGHT)
                    }

                override suspend fun set(mode: ThemeMode) = Unit
            }
        val overrides = module { single<ThemePreferenceRepository> { gatedRepository } }
        loadKoinModules(overrides)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                repeat(20) { shadowOf(Looper.getMainLooper()).idle() }
                // Sem tema resolvido, nada é composto: a Splash do sistema ainda cobre a tela.
                assertTrue(composeRule.onAllNodesWithTag(SemanticsTags.HOME_MENU_BUTTON).fetchSemanticsNodes().isEmpty())
                assertFalse(scenario.hasLightStatusBarIcons())

                gate.complete(Unit)

                waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_MENU_BUTTON) }
                assertTrue(scenario.hasLightStatusBarIcons())
                assertEquals(LIGHT_BACKGROUND, scenario.windowBackground())
            }
        } finally {
            unloadKoinModules(overrides)
        }
    }

    private companion object {
        const val DARK_BACKGROUND = 0xFF0A0A0A.toInt()
        const val LIGHT_BACKGROUND = 0xFFFAFAFA.toInt()
    }
}
