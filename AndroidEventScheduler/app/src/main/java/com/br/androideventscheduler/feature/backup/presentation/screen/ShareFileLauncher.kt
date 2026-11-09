package com.br.androideventscheduler.feature.backup.presentation.screen

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

internal const val BACKUP_MIME_TYPE = "application/json"

/**
 * Abre o share sheet com o arquivo em [fileUri] (URI do `FileProvider`): `ACTION_SEND`, `application/json`,
 * `EXTRA_STREAM`, [subject] e [body] (apps de e-mail já recebem assunto, texto e anexo) e permissão de leitura **só
 * para essa URI**. O `ClipData` com a URI é o que faz a permissão chegar a alguns apps de e-mail. É o ponto único
 * de lançamento do Intent; recebe a `Activity` como [context] (sem `FLAG_ACTIVITY_NEW_TASK`) e não guarda referência.
 * Devolve `false` se nenhum app puder tratar o envio.
 */
internal fun launchShareFileChooser(
    context: Context,
    fileUri: String,
    subject: String,
    body: String,
): Boolean {
    val uri = fileUri.toUri()
    val sendIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = BACKUP_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            clipData = ClipData.newRawUri(subject, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    return try {
        context.startActivity(Intent.createChooser(sendIntent, subject))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
