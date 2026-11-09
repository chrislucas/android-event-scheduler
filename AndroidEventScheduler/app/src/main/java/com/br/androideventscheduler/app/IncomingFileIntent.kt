package com.br.androideventscheduler.app

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

private const val JSON_MIME_TYPE = "application/json"
private const val CONTENT_SCHEME = "content"

/**
 * URI do arquivo de agenda carregado por este [Intent], ou `null` se ele não for um recebimento válido.
 *
 * A `MainActivity` é exportada (é a launcher), então o intent vem de qualquer app e é **não confiável**. Por isso:
 * só `ACTION_VIEW` (URI em `data`) e `ACTION_SEND` (URI em `EXTRA_STREAM`, lido com tipo verificado) são aceitos, o
 * MIME precisa ser exatamente `application/json` e o esquema `content://` (nunca `file://`). Nenhum outro extra é
 * lido e o intent não é repassado a ninguém: só a URI validada segue, e o conteúdo ainda passa por limite de tamanho,
 * validação completa e confirmação do usuário antes de gravar qualquer coisa.
 */
internal fun Intent.incomingAgendaFileUri(): String? {
    if (!type.equals(JSON_MIME_TYPE, ignoreCase = true)) return null
    val uri: Uri? =
        when (action) {
            Intent.ACTION_VIEW -> data
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }
    return uri?.takeIf { it.scheme == CONTENT_SCHEME }?.toString()
}
