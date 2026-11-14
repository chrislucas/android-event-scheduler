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
    /*
     * Por que rememberUpdatedState:
     * `onEffect` costuma ser uma lambda criada na recomposição, capturando valores do chamador
     * (navController, snackbarHostState, strings, callbacks...). A cada recomposição chega uma
     * instância nova. O LaunchedEffect abaixo, porém, roda uma vez só por combinação de chaves,
     * e uma lambda capturada diretamente dentro dele ficaria congelada na versão da primeira
     * composição — efeitos posteriores chamariam um callback desatualizado (stale closure).
     *
     * A alternativa óbvia — usar `onEffect` como chave do LaunchedEffect — é pior: como a lambda
     * muda a cada recomposição, a coleta seria cancelada e reiniciada o tempo todo, e um efeito
     * poderia ser consumido do Channel justamente no meio de um reinício e se perder.
     *
     * rememberUpdatedState resolve os dois problemas: guarda a lambda num State que é atualizado
     * a cada recomposição sem reiniciar o efeito. A coleta continua estável (chaves só `effects`
     * e `lifecycleOwner`) e, ao receber um efeito, lê `currentOnEffect.value`, que é sempre a
     * versão mais recente do callback.
     *
     * Por que `lifecycleOwner` é chave (e não vai para um rememberUpdatedState como o callback):
     * o corpo do efeito usa o owner para *montar* a coleta, e não só para chamá-lo depois:
     * `repeatOnLifecycle` passa a observar o `Lifecycle` daquele owner específico. Se o
     * `LocalLifecycleOwner` mudar (outro provedor acima na árvore, como a entry de navegação, um
     * diálogo ou um `CompositionLocalProvider`), um efeito que não reinicia continua preso ao owner
     * antigo. Daí saem dois erros:
     *  - o owner antigo é destruído: `repeatOnLifecycle` retorna e a coleta para de vez, então
     *    nenhum efeito é mais entregue;
     *  - o owner antigo segue STARTED enquanto o novo está parado: os efeitos são entregues com a
     *    tela invisível e saem do Channel em vez de esperar no buffer.
     * Não dá para "trocar o owner" no meio de um `repeatOnLifecycle` em andamento; a coleta tem de
     * recomeçar no `Lifecycle` novo, e é isso que a chave faz. O custo do reinício é desprezível,
     * porque o owner quase nunca muda. A regra geral: valor que, ao mudar, exige **refazer** o
     * trabalho do efeito é chave; valor que só precisa estar **atualizado** quando for lido é
     * rememberUpdatedState. `effects` é chave pelo mesmo motivo: um Flow novo exige coletar o
     * novo, não o antigo.
     */
    val currentOnEffect = rememberUpdatedState(onEffect)
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Lê `.value` no momento da entrega (não antes), para pegar o callback mais recente.
            effects.collect { currentOnEffect.value(it) }
        }
    }
}
