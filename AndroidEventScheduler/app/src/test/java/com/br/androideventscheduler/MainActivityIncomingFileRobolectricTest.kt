package com.br.androideventscheduler

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.app.IncomingFileState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** Registro, no [IncomingFileState], só de arquivos válidos recebidos por `VIEW`/`SEND` (o intent é não confiável). */
@RunWith(RobolectricTestRunner::class)
class MainActivityIncomingFileRobolectricTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val incomingFileState: IncomingFileState get() = GlobalContext.get().get()
    private val attachment = "content://com.example.mail.provider/attachments/agenda.json"

    private fun mainIntent(action: String? = null): Intent = Intent(context, MainActivity::class.java).also { it.action = action }

    private fun viewIntent(
        uri: String = attachment,
        type: String = "application/json",
    ): Intent = mainIntent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), type)

    @Test
    fun `given a view intent with a json attachment when the activity is created then the uri is registered`() {
        ActivityScenario.launch<MainActivity>(viewIntent()).use {
            assertEquals(attachment, incomingFileState.pendingFileUri.value)
        }
    }

    @Test
    fun `given a send intent with a json stream when the activity is created then the uri is registered`() {
        val send = mainIntent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM, Uri.parse(attachment))

        ActivityScenario.launch<MainActivity>(send).use {
            assertEquals(attachment, incomingFileState.pendingFileUri.value)
        }
    }

    @Test
    fun `given a plain launcher intent when the activity is created then no file is registered`() {
        ActivityScenario.launch<MainActivity>(mainIntent()).use {
            assertNull(incomingFileState.pendingFileUri.value)
        }
    }

    @Test
    fun `given untrusted intents with a wrong mime or scheme when the activity is created then no file is registered`() {
        listOf(
            viewIntent(type = "text/plain"),
            viewIntent(uri = "file:///data/data/com.br.androideventscheduler/databases/remember_me.db"),
            viewIntent(uri = "https://example.com/agenda.json"),
        ).forEach { intent ->
            ActivityScenario.launch<MainActivity>(intent).use {
                assertNull(incomingFileState.pendingFileUri.value)
            }
        }
    }

    @Test
    fun `given the activity is running when a new file intent arrives then the new uri is registered`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java, mainIntent()).setup()

        controller.newIntent(viewIntent())

        assertEquals(attachment, incomingFileState.pendingFileUri.value)
        controller.pause().stop().destroy()
    }

    @Test
    fun `given the uri was consumed when the activity is recreated then the same intent does not register it again`() {
        ActivityScenario.launch<MainActivity>(viewIntent()).use { scenario ->
            incomingFileState.consume(attachment)

            scenario.recreate()

            assertNull(incomingFileState.pendingFileUri.value)
        }
    }

    @Test
    fun `given an intent replayed from recents history when the activity is created then the stale file is ignored`() {
        val replayed = viewIntent().addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)

        ActivityScenario.launch<MainActivity>(replayed).use {
            assertNull(incomingFileState.pendingFileUri.value)
        }
    }
}
