package com.br.androideventscheduler.feature.backup.presentation.screen

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ShareFileIntentRobolectricTest {
    // Activity real: é o que a tela passa; o contexto da aplicação exigiria NEW_TASK, que o design proíbe.
    private val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    private val fileUri = "content://com.br.androideventscheduler.fileprovider/exports/rememberme-agenda-2026-11-08.json"

    @Suppress("DEPRECATION")
    private fun launchAndGetSendIntent(): Pair<Intent, Intent> {
        launchShareFileChooser(activity, fileUri, subject = "Agenda RememberMe — 08/11/2026", body = "Backup da agenda com 3 agendamentos.")
        val chooser = shadowOf(activity).nextStartedActivity
        return chooser to chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
    }

    @Test
    fun `given an exported file when launching then a chooser wraps a json send intent with the stream subject and text`() {
        val (chooser, sendIntent) = launchAndGetSendIntent()

        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertEquals(Intent.ACTION_SEND, sendIntent.action)
        assertEquals("application/json", sendIntent.type)
        assertEquals("Agenda RememberMe — 08/11/2026", sendIntent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals("Backup da agenda com 3 agendamentos.", sendIntent.getStringExtra(Intent.EXTRA_TEXT))
        @Suppress("DEPRECATION")
        assertEquals(Uri.parse(fileUri), sendIntent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
    }

    @Test
    fun `given an exported file when launching then read permission is granted for that uri only`() {
        val (chooser, sendIntent) = launchAndGetSendIntent()

        assertNotEquals(0, sendIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        assertEquals(0, sendIntent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        assertEquals(Uri.parse(fileUri), sendIntent.clipData!!.getItemAt(0).uri)
        // createChooser repassa a concessão ao chooser que o sistema abre.
        assertNotEquals(0, chooser.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        assertEquals(Uri.parse(fileUri), chooser.clipData!!.getItemAt(0).uri)
    }

    @Test
    fun `given any file when launching then the chooser does not force a new task`() {
        val (chooser, _) = launchAndGetSendIntent()

        assertEquals(0, chooser.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun `given no app able to handle the send when launching then it returns false without crashing`() {
        val failingContext =
            object : ContextWrapper(activity) {
                override fun startActivity(intent: Intent) = throw ActivityNotFoundException("no handler")
            }

        val launched = launchShareFileChooser(failingContext, fileUri, subject = "s", body = "b")

        assertFalse(launched)
        assertNull(shadowOf(activity).nextStartedActivity)
    }

    @Test
    fun `given a handler available when launching then it returns true`() {
        assertTrue(launchShareFileChooser(activity, fileUri, subject = "s", body = "b"))
    }
}
