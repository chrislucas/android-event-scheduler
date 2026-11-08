package com.br.androideventscheduler.feature.detail.presentation.screen

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ShareLauncherRobolectricTest {
    // Activity real: é o que a tela passa; o contexto da aplicação exigiria NEW_TASK, que o design proíbe.
    private val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

    @Test
    fun `given text when launching then a chooser wraps a plain text send intent with subject and body`() {
        val launched = launchShareChooser(activity, subject = "RememberMe: Aula", body = "📅 Aula\n🕒 hoje")

        val chooser = shadowOf(activity).nextStartedActivity

        @Suppress("DEPRECATION")
        val sendIntent = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertTrue(launched)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertEquals(Intent.ACTION_SEND, sendIntent.action)
        assertEquals("text/plain", sendIntent.type)
        assertEquals("RememberMe: Aula", sendIntent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals("📅 Aula\n🕒 hoje", sendIntent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun `given any text when launching then the chooser does not force a new task`() {
        launchShareChooser(activity, subject = "s", body = "b")

        val chooser = shadowOf(activity).nextStartedActivity
        assertEquals(0, chooser.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun `given no app able to handle the send when launching then it returns false without crashing`() {
        val failingContext =
            object : ContextWrapper(activity) {
                override fun startActivity(intent: Intent) = throw ActivityNotFoundException("no handler")
            }

        val launched = launchShareChooser(failingContext, subject = "s", body = "b")

        assertFalse(launched)
        assertNull(shadowOf(activity).nextStartedActivity)
    }
}
