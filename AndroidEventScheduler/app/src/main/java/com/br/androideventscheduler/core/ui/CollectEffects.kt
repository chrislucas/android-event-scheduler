package com.br.androideventscheduler.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

// Sem preview: helper sem UI própria.

/**
 * Coleta os efeitos one-shot de um ViewModel **uma única vez** e apenas enquanto a tela está
 * [Lifecycle.State.STARTED] ou acima. Como os efeitos vêm de um `Channel`, os emitidos com a tela
 * parada ficam no buffer e são entregues ao voltar, exatamente uma vez.
 *
 * É um helper de UI; não é uma abstração de ViewModel.
 */
@Composable
fun <T> CollectEffects(
    effects: Flow<T>,
    onEffect: suspend (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEffect = rememberUpdatedState(onEffect)
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            effects.collect { currentOnEffect.value(it) }
        }
    }
}
