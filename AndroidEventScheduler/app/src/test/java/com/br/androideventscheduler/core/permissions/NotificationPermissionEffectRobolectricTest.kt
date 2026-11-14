package com.br.androideventscheduler.core.permissions

import android.Manifest
import android.app.Application
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Registro que só anota os pedidos de permissão e responde com [grantResult], como o diálogo do sistema. */
private class RecordingRegistry(
    private val grantResult: Boolean,
) : ActivityResultRegistry() {
    val requestedPermissions = mutableListOf<String>()

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        requestedPermissions += input as String
        dispatchResult(requestCode, grantResult)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationPermissionEffectRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val application: Application = ApplicationProvider.getApplicationContext()

    private fun registryOwner(registry: ActivityResultRegistry) =
        object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }

    @Test
    fun `given api 33 without permission when the effect enters composition then it requests post notifications once`() {
        val registry = RecordingRegistry(grantResult = false)

        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner(registry)) {
                NotificationPermissionEffect()
            }
        }
        composeRule.waitForIdle()

        assertEquals(listOf(Manifest.permission.POST_NOTIFICATIONS), registry.requestedPermissions)
    }

    @Test
    fun `given the request was denied when the screen recomposes then it does not ask again`() {
        val registry = RecordingRegistry(grantResult = false)
        var counter by mutableStateOf(0)

        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner(registry)) {
                NotificationPermissionEffect()
                @Suppress("UNUSED_EXPRESSION")
                counter
            }
        }
        composeRule.waitForIdle()
        counter++
        composeRule.waitForIdle()
        counter++
        composeRule.waitForIdle()

        assertEquals(1, registry.requestedPermissions.size)
    }

    @Test
    fun `given the request was denied when the state is restored after rotation then it does not ask again`() {
        val registry = RecordingRegistry(grantResult = false)
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner(registry)) {
                NotificationPermissionEffect()
            }
        }
        composeRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertEquals(1, registry.requestedPermissions.size)
    }

    @Test
    fun `given the user answers the prompt when the result arrives then the callback receives it`() {
        val results = mutableListOf<Boolean>()
        val registry = RecordingRegistry(grantResult = true)

        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner(registry)) {
                NotificationPermissionEffect(onResult = { granted -> results += granted })
            }
        }
        composeRule.waitForIdle()

        assertEquals(listOf(true), results)
    }

    @Test
    fun `given permission already granted when the effect enters composition then it does not ask`() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val registry = RecordingRegistry(grantResult = true)

        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner(registry)) {
                NotificationPermissionEffect()
            }
        }
        composeRule.waitForIdle()

        assertTrue(registry.requestedPermissions.isEmpty())
    }

    @Test
    @Config(sdk = [32])
    fun `given api 32 when the effect enters composition then it does not ask because there is no runtime permission`() {
        val registry = RecordingRegistry(grantResult = true)

        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner(registry)) {
                NotificationPermissionEffect()
            }
        }
        composeRule.waitForIdle()

        assertTrue(registry.requestedPermissions.isEmpty())
    }
}
