package com.br.androideventscheduler.core.testing

import androidx.activity.ComponentDialog
import org.robolectric.shadows.ShadowDialog

/**
 * Aperta "voltar" no diálogo aberto mais recente, como o sistema faz: pelo `OnBackPressedDispatcher` do diálogo.
 *
 * O `Dialog` do Compose é um [ComponentDialog], e o `Dialog.onBackPressed()` direto é deprecado. Chame na thread
 * de UI (`composeRule.runOnUiThread { pressBackOnLatestDialog() }`).
 */
fun pressBackOnLatestDialog() {
    val dialog = checkNotNull(ShadowDialog.getLatestDialog()) { "Nenhum diálogo aberto" }
    check(dialog is ComponentDialog) { "O diálogo aberto não é um ComponentDialog: ${dialog.javaClass.name}" }
    dialog.onBackPressedDispatcher.onBackPressed()
}
