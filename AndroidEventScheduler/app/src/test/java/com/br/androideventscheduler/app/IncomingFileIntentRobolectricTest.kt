package com.br.androideventscheduler.app

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** O intent que chega à `MainActivity` (exportada) é não confiável: só ação, MIME e esquema esperados passam. */
@RunWith(RobolectricTestRunner::class)
class IncomingFileIntentRobolectricTest {
    private val contentUri = "content://com.google.android.gm.sapi/attachments/agenda.json"

    private fun view(
        uri: String?,
        type: String? = "application/json",
    ) = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri?.let(Uri::parse), type) }

    private fun send(
        stream: Any?,
        type: String? = "application/json",
    ) = Intent(Intent.ACTION_SEND).apply {
        this.type = type
        when (stream) {
            is Uri -> putExtra(Intent.EXTRA_STREAM, stream)
            is String -> putExtra(Intent.EXTRA_STREAM, stream)
        }
    }

    @Test
    fun `given a view of a json content uri when parsing then the uri is returned`() {
        assertEquals(contentUri, view(contentUri).incomingAgendaFileUri())
    }

    @Test
    fun `given a send with a json stream when parsing then the uri is returned`() {
        assertEquals(contentUri, send(Uri.parse(contentUri)).incomingAgendaFileUri())
    }

    @Test
    fun `given the mime type with different casing when parsing then it is still accepted`() {
        assertEquals(contentUri, view(contentUri, type = "Application/JSON").incomingAgendaFileUri())
    }

    @Test
    fun `given another mime type or none when parsing then it is ignored`() {
        assertNull(view(contentUri, type = "text/plain").incomingAgendaFileUri())
        assertNull(view(contentUri, type = "application/octet-stream").incomingAgendaFileUri())
        assertNull(view(contentUri, type = null).incomingAgendaFileUri())
        assertNull(send(Uri.parse(contentUri), type = "image/png").incomingAgendaFileUri())
    }

    @Test
    fun `given a file or web scheme when parsing then it is ignored`() {
        assertNull(view("file:///sdcard/Download/agenda.json").incomingAgendaFileUri())
        assertNull(view("http://example.com/agenda.json").incomingAgendaFileUri())
        assertNull(send(Uri.parse("file:///data/data/com.br.androideventscheduler/databases/remember_me.db")).incomingAgendaFileUri())
    }

    @Test
    fun `given a stream extra of the wrong type or a missing uri when parsing then it is ignored`() {
        assertNull(send(contentUri).incomingAgendaFileUri())
        assertNull(send(null).incomingAgendaFileUri())
        assertNull(view(null).incomingAgendaFileUri())
    }

    @Test
    fun `given other actions when parsing then they are ignored even with a valid uri`() {
        val main = Intent(Intent.ACTION_MAIN).apply { setDataAndType(Uri.parse(contentUri), "application/json") }
        val edit = Intent(Intent.ACTION_EDIT).apply { setDataAndType(Uri.parse(contentUri), "application/json") }
        val sendTo = Intent(Intent.ACTION_SENDTO).apply { setDataAndType(Uri.parse(contentUri), "application/json") }

        assertNull(main.incomingAgendaFileUri())
        assertNull(edit.incomingAgendaFileUri())
        assertNull(sendTo.incomingAgendaFileUri())
        assertNull(Intent().incomingAgendaFileUri())
    }

    @Test
    fun `given a view with the uri only in extras when parsing then the extra is not trusted`() {
        val intent = Intent(Intent.ACTION_VIEW).apply { type = "application/json" }.putExtra(Intent.EXTRA_STREAM, Uri.parse(contentUri))

        assertNull(intent.incomingAgendaFileUri())
    }
}
