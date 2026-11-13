package com.br.androideventscheduler.core.testing

import android.os.Looper
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import org.robolectric.Shadows.shadowOf

/** Tempo real máximo de espera nos testes Robolectric com Activity (o CI é bem mais lento que a máquina local). */
const val ACTIVITY_WAIT_MILLIS = 10_000L

/**
 * Espera [condition] ficar verdadeira, drenando o looper principal a cada volta.
 *
 * **Use antes da primeira asserção sobre uma Activity real** (`MainActivity`, `FullScreenAlarmActivity`): o que a
 * tela mostra depende de trabalho em threads reais (DataStore e Room no `io`). O `waitForIdle` do Compose só
 * sincroniza a main thread e o Compose; ele não espera essas threads. Uma asserção imediata (`assertIsDisplayed`,
 * `assertExists`) passa na máquina rápida e falha no runner lento. Ver steering `arquitetura.md` §5.2.
 *
 * Drenar o looper (`idle()`) executa o que outras threads postaram na main, sem avançar o relógio virtual.
 */
fun ComposeTestRule.waitUntilWithMainLooper(
    timeoutMillis: Long = ACTIVITY_WAIT_MILLIS,
    condition: () -> Boolean,
) {
    waitUntil(timeoutMillis) {
        shadowOf(Looper.getMainLooper()).idle()
        condition()
    }
}

/** `true` se existe ao menos um nó com [tag] (sem lançar exceção quando não existe, ao contrário de `assertExists`). */
fun ComposeTestRule.hasNode(
    tag: String,
    useUnmergedTree: Boolean = false,
): Boolean = onAllNodesWithTag(tag, useUnmergedTree).fetchSemanticsNodes().isNotEmpty()
