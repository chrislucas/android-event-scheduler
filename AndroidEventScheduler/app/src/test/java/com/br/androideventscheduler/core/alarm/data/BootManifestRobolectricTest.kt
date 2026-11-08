package com.br.androideventscheduler.core.alarm.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BootManifestRobolectricTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val receiverClassName = BootCompletedReceiver::class.java.name

    private fun resolvedReceiverNames(action: String): List<String> =
        context.packageManager
            .queryBroadcastReceivers(Intent(action).setPackage(context.packageName), 0)
            .map { it.activityInfo.name }

    @Test
    fun `given merged manifest when reading receivers then the boot receiver is enabled, not exported and not direct boot aware`() {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_RECEIVERS)

        val bootReceiver = packageInfo.receivers.orEmpty().single { it.name == receiverClassName }
        assertTrue(bootReceiver.enabled)
        assertFalse(bootReceiver.exported)
        assertFalse(bootReceiver.directBootAware)
    }

    @Test
    fun `given each supported action when resolving receivers then the boot receiver handles it`() {
        BootCompletedReceiver.SUPPORTED_ACTIONS.forEach { action ->
            assertTrue("$action deveria chegar ao receiver", receiverClassName in resolvedReceiverNames(action))
        }
    }

    @Test
    fun `given locked boot completed when resolving receivers then the boot receiver does not handle it`() {
        assertFalse(receiverClassName in resolvedReceiverNames("android.intent.action.LOCKED_BOOT_COMPLETED"))
    }

    @Test
    fun `given merged manifest when reading requested permissions then receive boot completed is declared`() {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)

        assertTrue("android.permission.RECEIVE_BOOT_COMPLETED" in packageInfo.requestedPermissions.orEmpty())
    }
}
