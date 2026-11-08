package com.br.androideventscheduler.feature.detail.presentation.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/**
 * Abre o seletor de compartilhamento do sistema com [subject] e [body] (`ACTION_SEND`, `text/plain`).
 * É o ponto único de lançamento do Intent. Recebe a `Activity` atual como [context] (sem
 * `FLAG_ACTIVITY_NEW_TASK`) e não guarda referência a ela. Devolve `false` se nenhum app puder tratar o envio.
 */
internal fun launchShareChooser(
    context: Context,
    subject: String,
    body: String,
): Boolean {
    val sendIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
    return try {
        context.startActivity(Intent.createChooser(sendIntent, subject))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
