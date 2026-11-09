package com.br.androideventscheduler.feature.backup.testing

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.MainActivity
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayInputStream

/** Apoio dos testes ponta a ponta que abrem a `MainActivity` com um arquivo recebido de outro app. */
object IncomingFileTestSupport {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** Registra [bytes] como conteúdo da URI `content://` de um provedor de e-mail fictício e devolve a URI. */
    fun registerAttachment(
        bytes: ByteArray,
        name: String = "agenda.json",
    ): Uri {
        val uri = Uri.parse("content://com.example.mail.provider/attachments/$name")
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) { ByteArrayInputStream(bytes) }
        return uri
    }

    /** `ACTION_VIEW` explícito para a `MainActivity`, como o "Abrir com" de um anexo de e-mail. */
    fun viewIntent(uri: Uri): Intent =
        Intent(Intent.ACTION_VIEW)
            .setClass(context, MainActivity::class.java)
            .setDataAndType(uri, "application/json")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    /** `ACTION_SEND` explícito para a `MainActivity`, como o "Compartilhar com" de um app de arquivos. */
    fun sendIntent(uri: Uri): Intent =
        Intent(Intent.ACTION_SEND)
            .setClass(context, MainActivity::class.java)
            .setType("application/json")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
