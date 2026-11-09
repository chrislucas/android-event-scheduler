package com.br.androideventscheduler

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.view.WindowManager
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.app.DeepLinkState
import com.br.androideventscheduler.app.IncomingFileState
import com.br.androideventscheduler.core.alarm.data.AlarmIntents
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.security.domain.AppLockState
import com.br.androideventscheduler.core.security.domain.AppLockTimeout
import com.br.androideventscheduler.core.security.domain.AuthResult
import com.br.androideventscheduler.core.security.testing.AppLockTestHarness
import com.br.androideventscheduler.core.ui.SemanticsTags
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
 * Ponta a ponta da spec 16 com a `MainActivity` e o grafo real, trocando só o autenticador (diálogo do sistema) por um
 * fake: o gate bloqueia sem compor a agenda, devolve à mesma tela, adia o deep link e o arquivo recebido e aplica `FLAG_SECURE`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR-w411dp-h891dp")
class AppLockGateRobolectricTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var harness: AppLockTestHarness

    private fun install(
        settings: UserSettings = UserSettings(appLockEnabled = true, appLockTimeout = AppLockTimeout.IMMEDIATE),
        authResult: AuthResult = AuthResult.Canceled,
    ) {
        harness = AppLockTestHarness(settings, authResult).also { it.install() }
    }

    @After
    fun tearDown() {
        if (::harness.isInitialized) harness.uninstall()
    }

    private fun waitUntilWithMainLooper(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    /** Espera o share sheet (`ACTION_CHOOSER`), ignorando outras Activities iniciadas antes (ex.: pedido de permissão). */
    private fun waitUntilChooserStarted(application: Application) {
        var chooserStarted = false
        waitUntilWithMainLooper {
            while (!chooserStarted) {
                val started = shadowOf(application).nextStartedActivity ?: break
                chooserStarted = started.action == Intent.ACTION_CHOOSER
            }
            chooserStarted
        }
    }

    private fun hasNode(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun unlockWith(result: AuthResult) {
        harness.authenticator.result = result
        composeRule.onNodeWithTag(SemanticsTags.LOCK_UNLOCK_BUTTON).performClick()
    }

    @Test
    fun `given protection on when the app opens then it starts locked and the home is not composed`() {
        install()

        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.LOCK_SCREEN) }

            assertTrue(hasNode(SemanticsTags.LOCK_UNLOCK_BUTTON))
            assertEquals(false, hasNode(SemanticsTags.HOME_CALENDAR_GRID))
            assertEquals(false, hasNode(SemanticsTags.SPLASH_LOGO))
            assertEquals(1, harness.authenticator.calls)
        }
    }

    @Test
    fun `given protection off when the app opens then the home is shown and no lock screen appears`() {
        install(settings = UserSettings(appLockEnabled = false))

        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_CALENDAR_GRID) }

            assertEquals(false, hasNode(SemanticsTags.LOCK_SCREEN))
            assertEquals(0, harness.authenticator.calls)
        }
    }

    @Test
    fun `given the app opens locked when the credential is confirmed then the home is shown`() {
        install(authResult = AuthResult.Success)

        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_CALENDAR_GRID) }

            assertEquals(false, hasNode(SemanticsTags.LOCK_SCREEN))
            assertEquals(AppLockState.Unlocked, harness.controller.lockState.value)
        }
    }

    @Test
    fun `given the user is on search when the app locks and is unlocked then the same screen returns with the back stack intact`() {
        install(authResult = AuthResult.Success)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_SEARCH_BUTTON) }
            composeRule.onNodeWithTag(SemanticsTags.HOME_SEARCH_BUTTON).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.SEARCH_QUERY_INPUT) }

            harness.authenticator.result = AuthResult.Canceled
            harness.backgroundFor(millis = 1_000L)
            waitUntilWithMainLooper { hasNode(SemanticsTags.LOCK_SCREEN) }
            assertEquals(false, hasNode(SemanticsTags.SEARCH_QUERY_INPUT))

            unlockWith(AuthResult.Success)
            waitUntilWithMainLooper { hasNode(SemanticsTags.SEARCH_QUERY_INPUT) }

            assertEquals(false, hasNode(SemanticsTags.LOCK_SCREEN))
            // O back stack foi preservado: voltar da pesquisa leva à Home, não à Splash.
            scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_CALENDAR_GRID) }
        }
    }

    @Test
    fun `given the app is locked when the system back is pressed then the activity finishes`() {
        install()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitUntilWithMainLooper { hasNode(SemanticsTags.LOCK_SCREEN) }

            scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
            shadowOf(Looper.getMainLooper()).idle()

            scenario.onActivity { activity -> assertTrue(activity.isFinishing) }
        }
    }

    @Test
    fun `given a notification deep link when the app is locked then it is consumed only after unlocking`() {
        install()
        runBlocking {
            GlobalContext.get().get<ScheduleRepository>().upsert(
                scheduleItem(id = "from-notification", title = "Consulta médica"),
            )
        }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationIntent = Intent(context, MainActivity::class.java).putExtra(AlarmIntents.EXTRA_SCHEDULE_ID, "from-notification")
        val deepLinkState = GlobalContext.get().get<DeepLinkState>()

        ActivityScenario.launch<MainActivity>(notificationIntent).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.LOCK_SCREEN) }
            assertEquals("from-notification", deepLinkState.pendingScheduleId.value)
            assertEquals(false, hasNode(SemanticsTags.DETAIL_TITLE))

            unlockWith(AuthResult.Success)
            waitUntilWithMainLooper { hasNode(SemanticsTags.DETAIL_TITLE) }

            assertNull(deepLinkState.pendingScheduleId.value)
        }
    }

    @Test
    fun `given a received agenda file when the app is locked then it is consumed only after unlocking`() {
        install()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileUri = "content://com.example.mail.provider/attachments/agenda.json"
        val viewIntent =
            Intent(
                context,
                MainActivity::class.java,
            ).setAction(Intent.ACTION_VIEW).setDataAndType(Uri.parse(fileUri), "application/json")
        val incomingFileState = GlobalContext.get().get<IncomingFileState>()

        ActivityScenario.launch<MainActivity>(viewIntent).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.LOCK_SCREEN) }
            assertEquals(fileUri, incomingFileState.pendingFileUri.value)
            assertEquals(false, hasNode(SemanticsTags.BACKUP_BACK_BUTTON))

            unlockWith(AuthResult.Success)
            waitUntilWithMainLooper { hasNode(SemanticsTags.BACKUP_BACK_BUTTON) }

            assertNull(incomingFileState.pendingFileUri.value)
        }
    }

    @Test
    fun `given protection on when the activity is shown then the window is secure`() {
        install()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitUntilWithMainLooper { hasNode(SemanticsTags.LOCK_SCREEN) }

            scenario.onActivity { activity -> assertTrue(activity.window.isSecure()) }
        }
    }

    @Test
    fun `given protection off when the activity is shown then the window is not secure`() {
        install(settings = UserSettings(appLockEnabled = false))

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_CALENDAR_GRID) }

            scenario.onActivity { activity -> assertEquals(false, activity.window.isSecure()) }
        }
    }

    @Test
    fun `given protection on when the user turns it off then the secure flag is removed`() {
        install()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitUntilWithMainLooper { hasNode(SemanticsTags.LOCK_SCREEN) }
            scenario.onActivity { activity -> assertTrue(activity.window.isSecure()) }

            harness.repository.settings.value =
                harness.repository.settings.value
                    .copy(appLockEnabled = false)
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_CALENDAR_GRID) }

            scenario.onActivity { activity -> assertEquals(false, activity.window.isSecure()) }
        }
    }

    @Test
    fun `given the device lost its screen lock when the app opens then protection is disabled and the home is shown`() {
        install(authResult = AuthResult.Canceled)
        harness.deviceSecure = false
        // O controller é criado (e lê as preferências) na primeira resolução: força agora, com o aparelho inseguro.
        assertNotNull(harness.controller)

        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_CALENDAR_GRID) }

            assertEquals(false, harness.repository.settings.value.appLockEnabled)
        }
    }

    @Test
    fun `given protection off when enabling it in settings then the credential is asked before saving and the app stays unlocked`() {
        install(settings = UserSettings(appLockEnabled = false), authResult = AuthResult.Success)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_MENU_BUTTON) }
            composeRule.onNodeWithTag(SemanticsTags.HOME_MENU_BUTTON).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_DRAWER_SETTINGS_BUTTON) }
            composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SETTINGS_BUTTON).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.SETTINGS_APP_LOCK_SWITCH) }

            composeRule.onNodeWithTag(SemanticsTags.SETTINGS_APP_LOCK_SWITCH).performScrollTo().performClick()
            waitUntilWithMainLooper { harness.repository.settings.value.appLockEnabled }

            assertEquals(1, harness.authenticator.calls)
            assertEquals(AppLockState.Unlocked, harness.controller.lockState.value)
            waitUntilWithMainLooper { hasNode(SemanticsTags.SETTINGS_APP_LOCK_TIMEOUT_OPTION_PREFIX + AppLockTimeout.ONE_MINUTE.name) }
            scenario.onActivity { activity -> assertTrue(activity.window.isSecure()) }
        }
    }

    @Test
    fun `given protection off when the credential is canceled in settings then the preference stays off`() {
        install(settings = UserSettings(appLockEnabled = false), authResult = AuthResult.Canceled)

        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_MENU_BUTTON) }
            composeRule.onNodeWithTag(SemanticsTags.HOME_MENU_BUTTON).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_DRAWER_SETTINGS_BUTTON) }
            composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SETTINGS_BUTTON).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.SETTINGS_APP_LOCK_SWITCH) }

            composeRule.onNodeWithTag(SemanticsTags.SETTINGS_APP_LOCK_SWITCH).performScrollTo().performClick()
            waitUntilWithMainLooper { harness.authenticator.calls == 1 }

            assertEquals(false, harness.repository.settings.value.appLockEnabled)
        }
    }

    @Test
    fun `given the share sheet is opened from the detail when the app returns then it does not lock`() {
        install(
            settings = UserSettings(appLockEnabled = true, appLockTimeout = AppLockTimeout.IMMEDIATE),
            authResult = AuthResult.Success,
        )
        val repository = GlobalContext.get().get<ScheduleRepository>()
        runBlocking { repository.upsert(scheduleItem(id = "to-share", title = "Reunião", startDate = LocalDate.now())) }
        val itemTag = SemanticsTags.HOME_SCHEDULE_ITEM_PREFIX + "to-share"
        val application = ApplicationProvider.getApplicationContext<Application>()

        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilWithMainLooper { hasNode(itemTag) }
            composeRule.onNodeWithTag(itemTag).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.DETAIL_SHARE_BUTTON) }
            composeRule.onNodeWithTag(SemanticsTags.DETAIL_SHARE_BUTTON).performClick()
            waitUntilChooserStarted(application)

            harness.backgroundFor(millis = 5_000L)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(AppLockState.Unlocked, harness.controller.lockState.value)
            assertEquals(false, hasNode(SemanticsTags.LOCK_SCREEN))

            // A supressão vale para uma volta só: a próxima ida ao segundo plano bloqueia.
            harness.backgroundFor(millis = 5_000L)
            assertEquals(AppLockState.Locked, harness.controller.lockState.value)
        }
    }

    @Test
    fun `given the export share sheet is opened from the backup screen when the app returns then it does not lock`() {
        install(
            settings = UserSettings(appLockEnabled = true, appLockTimeout = AppLockTimeout.IMMEDIATE),
            authResult = AuthResult.Success,
        )
        runBlocking { GlobalContext.get().get<ScheduleRepository>().upsert(scheduleItem(id = "to-export", title = "Reunião")) }
        val application = ApplicationProvider.getApplicationContext<Application>()

        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_MENU_BUTTON) }
            composeRule.onNodeWithTag(SemanticsTags.HOME_MENU_BUTTON).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_DRAWER_SETTINGS_BUTTON) }
            composeRule.onNodeWithTag(SemanticsTags.HOME_DRAWER_SETTINGS_BUTTON).performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.SETTINGS_BACKUP_ITEM) }
            composeRule.onNodeWithTag(SemanticsTags.SETTINGS_BACKUP_ITEM).performScrollTo().performClick()
            waitUntilWithMainLooper { hasNode(SemanticsTags.BACKUP_EXPORT_SHARE_BUTTON) }
            composeRule.onNodeWithTag(SemanticsTags.BACKUP_EXPORT_SHARE_BUTTON).performClick()
            waitUntilChooserStarted(application)

            harness.backgroundFor(millis = 5_000L)

            assertEquals(AppLockState.Unlocked, harness.controller.lockState.value)
        }
    }

    private fun android.view.Window.isSecure(): Boolean = attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
}
