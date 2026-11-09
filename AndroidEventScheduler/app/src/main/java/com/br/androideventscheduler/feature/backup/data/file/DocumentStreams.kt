package com.br.androideventscheduler.feature.backup.data.file

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.br.androideventscheduler.feature.backup.domain.model.BackupError
import java.io.ByteArrayOutputStream
import java.io.IOException

private const val CONTENT_SCHEME = "content"
private const val BUFFER_SIZE = 8 * 1024
private const val UTF8_BOM = "\uFEFF"

/**
 * Leitura e gravação de documentos por `ContentResolver` (SAF). A URI de leitura é **não confiável**: só o esquema
 * `content://` é aceito e o tamanho é limitado durante a leitura, não apenas pelo valor informado pelo provedor.
 */
internal class DocumentStreams(
    private val context: Context,
) {
    /** Lê o texto UTF-8 de [uriText]. @throws BackupError.TooLarge acima de [maxBytes]; [BackupError.Io] em falha de leitura. */
    fun readText(
        uriText: String,
        maxBytes: Long,
    ): String {
        val uri = uriText.toUri()
        if (uri.scheme != CONTENT_SCHEME) throw BackupError.Io
        if ((declaredSize(uri) ?: 0L) > maxBytes) throw BackupError.TooLarge
        val bytes = readLimited(uri, maxBytes)
        return String(bytes, Charsets.UTF_8).removePrefix(UTF8_BOM)
    }

    /** Grava [text] (UTF-8) sobrescrevendo o documento. @throws BackupError.Io se o provedor não abrir o destino. */
    fun writeText(
        uriText: String,
        text: String,
    ) {
        val uri = uriText.toUri()
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw BackupError.Io
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    /** Tamanho informado pelo provedor, se houver. Pode faltar ou mentir: por isso a leitura também conta os bytes. */
    private fun declaredSize(uri: Uri): Long? =
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) cursor.getLong(index) else null
            }
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun readLimited(
        uri: Uri,
        maxBytes: Long,
    ): ByteArray {
        val input = context.contentResolver.openInputStream(uri) ?: throw BackupError.Io
        input.buffered().use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_SIZE)
            var total = 0L
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > maxBytes) throw BackupError.TooLarge
                output.write(buffer, 0, read)
            }
            return output.toByteArray()
        }
    }
}

/** Converte falhas de I/O e de permissão do provedor em [BackupError.Io]; erros de backup já tipados passam direto. */
internal inline fun <T> mapStorageErrors(block: () -> T): T =
    try {
        block()
    } catch (_: IOException) {
        throw BackupError.Io
    } catch (_: SecurityException) {
        throw BackupError.Io
    } catch (_: IllegalArgumentException) {
        throw BackupError.Io
    } catch (_: UnsupportedOperationException) {
        throw BackupError.Io
    }
