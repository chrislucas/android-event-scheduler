package com.br.androideventscheduler.app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.startup.AppInitializer
import androidx.startup.InitializationProvider
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.MainActivity
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.di.DispatcherNames
import com.br.androideventscheduler.core.ui.SemanticsTags
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.qualifier.named
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StartupAndSmokeRobolectricTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `given app launched when main activity starts then splash placeholder is shown`() {
        composeRule.onNodeWithTag(SemanticsTags.SPLASH_PLACEHOLDER).assertIsDisplayed()
        val appName = composeRule.activity.getString(R.string.app_name)
        composeRule.onNodeWithText(appName).assertIsDisplayed()
    }

    @Test
    fun `given main activity when created then edge to edge draws behind transparent system bars`() {
        val window = composeRule.activity.window

        @Suppress("DEPRECATION")
        assertEquals(Color.TRANSPARENT, window.statusBarColor)
        @Suppress("DEPRECATION")
        assertEquals(Color.TRANSPARENT, window.navigationBarColor)
    }

    @Test
    fun `given app launched when koin is queried then dispatchers are injectable by name`() {
        val io = GlobalContext.get().get<CoroutineDispatcher>(named(DispatcherNames.IO))

        assertSame(Dispatchers.IO, io)
    }

    @Test
    fun `given koin initializer when initialized again then the same koin application is reused`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val firstKoin = GlobalContext.get()

        AppInitializer.getInstance(context).initializeComponent(KoinInitializer::class.java)
        val secondKoin = KoinInitializer().create(context).koin

        assertSame(firstKoin, secondKoin)
    }
}

/**
 * Garante a ligação de produção: o `InitializationProvider` (que o Android instancia antes de
 * `Application.onCreate`, de Activities e de Receivers) está no manifest com o [KoinInitializer].
 */
@RunWith(RobolectricTestRunner::class)
class KoinStartupManifestRobolectricTest {
    @Test
    fun `given merged manifest when reading startup provider then koin initializer is registered`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val providerInfo =
            context.packageManager.getProviderInfo(
                ComponentName(context, InitializationProvider::class.java),
                PackageManager.GET_META_DATA,
            )

        assertEquals(
            "androidx.startup",
            providerInfo.metaData.getString(KoinInitializer::class.java.name),
        )
        assertEquals("${context.packageName}.androidx-startup", providerInfo.authority)
    }
}
