package com.br.androideventscheduler.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import com.br.androideventscheduler.MainActivity
import com.br.androideventscheduler.core.preferences.data.ThemePreferenceRepositoryImpl
import com.br.androideventscheduler.core.preferences.domain.ThemeMode
import com.br.androideventscheduler.core.preferences.domain.ThemePreferenceRepository
import com.br.androideventscheduler.core.testing.hasNode
import com.br.androideventscheduler.core.testing.waitUntilWithMainLooper
import com.br.androideventscheduler.core.ui.SemanticsTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

/**
 * Regressão do teste instável da Splash. A `MainActivity` só compõe a primeira tela depois de ler o tema do
 * DataStore, numa thread real de I/O. No runner lento do CI essa leitura terminava **depois** da primeira asserção, e
 * `assertIsDisplayed()` logo após abrir a Activity falhava ("splash_logo is not displayed").
 *
 * Aqui a leitura do tema demora [THEME_READ_DELAY_MILLIS] reais, de propósito, para reproduzir o runner lento de
 * forma determinística: o teste prova que, sem esperar, a Splash ainda não existe e que o padrão de espera
 * (`waitUntilWithMainLooper`) a encontra.
 */
@RunWith(RobolectricTestRunner::class)
class SlowThemeReadStartupRobolectricTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    /** Lê o tema de verdade, mas só depois de bloquear uma thread de I/O por [THEME_READ_DELAY_MILLIS] (disco lento). */
    private class SlowThemePreferenceRepository(
        private val real: ThemePreferenceRepository,
    ) : ThemePreferenceRepository {
        override fun observe(): Flow<ThemeMode> =
            flow {
                withContext(Dispatchers.IO) { Thread.sleep(THEME_READ_DELAY_MILLIS) }
                emitAll(real.observe())
            }

        override suspend fun set(mode: ThemeMode) = real.set(mode)
    }

    private val slowThemeModule =
        module {
            single<ThemePreferenceRepository> { SlowThemePreferenceRepository(ThemePreferenceRepositoryImpl(dataStore = get())) }
        }

    @Before
    fun useSlowThemeRead() {
        loadKoinModules(slowThemeModule)
    }

    @After
    fun restoreThemeRepository() {
        // O próximo teste recarrega `appModules` (TestRememberMeApplication) e volta a ter o repositório real.
        unloadKoinModules(slowThemeModule)
    }

    @Test
    fun `given a slow theme read when the app starts then the splash only exists after waiting for it`() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitForIdle()
            // O que a asserção imediata via no CI: o Compose está ocioso, mas o tema ainda não chegou.
            assertFalse(composeRule.hasNode(SemanticsTags.SPLASH_LOGO))

            composeRule.waitUntilWithMainLooper { composeRule.hasNode(SemanticsTags.SPLASH_LOGO) }
            composeRule.onNodeWithTag(SemanticsTags.SPLASH_LOGO).assertIsDisplayed()
        }
    }

    private companion object {
        const val THEME_READ_DELAY_MILLIS = 500L
    }
}
