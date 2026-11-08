package com.br.androideventscheduler.core.alarm.data

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AlarmManifestRobolectricTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `given merged manifest when reading requested permissions then alarm and notification permissions are declared`() {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)

        val requested = packageInfo.requestedPermissions.orEmpty().toSet()
        assertTrue("android.permission.POST_NOTIFICATIONS" in requested)
        assertTrue("android.permission.USE_EXACT_ALARM" in requested)
        assertTrue("android.permission.USE_FULL_SCREEN_INTENT" in requested)
        // maxSdkVersion=32: no API 33+ vale só a USE_EXACT_ALARM.
        assertFalse("android.permission.SCHEDULE_EXACT_ALARM" in requested)
    }

    @Test
    @Config(sdk = [32])
    fun `given api 32 when reading requested permissions then schedule exact alarm is requested`() {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)

        assertTrue("android.permission.SCHEDULE_EXACT_ALARM" in packageInfo.requestedPermissions.orEmpty())
    }

    @Test
    fun `given merged manifest when reading receivers then the alarm receiver exists and is not exported`() {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_RECEIVERS)

        val alarmReceiver = packageInfo.receivers.orEmpty().single { it.name == ScheduleAlarmReceiver::class.java.name }
        assertFalse(alarmReceiver.exported)
        assertEquals(context.packageName, alarmReceiver.packageName)
    }
}
