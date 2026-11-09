package com.br.androideventscheduler.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.dsl.module

/**
 * URI (texto) do arquivo de agenda recebido de outro app por `VIEW`/`SEND` e ainda não aberto. Mesmo padrão do
 * [DeepLinkState]: um `single` do Koin em que a Activity grava (`onCreate`/`onNewIntent`) e o `AppNavHost` consome
 * depois da Splash. A URI é consumida uma única vez. Só chegam aqui URIs já validadas por [incomingAgendaFileUri].
 */
class IncomingFileState {
    private val _pendingFileUri = MutableStateFlow<String?>(null)
    val pendingFileUri: StateFlow<String?> = _pendingFileUri.asStateFlow()

    /** Registra a URI a abrir; um novo arquivo substitui um ainda não consumido. `null`/vazio é ignorado. */
    fun set(fileUri: String?) {
        if (!fileUri.isNullOrBlank()) _pendingFileUri.value = fileUri
    }

    /** Limpa a URI pendente, mas só se ainda for [fileUri] (não descarta uma mais nova que chegou antes). */
    fun consume(fileUri: String) {
        _pendingFileUri.compareAndSet(fileUri, null)
    }
}

val incomingFileModule =
    module {
        single { IncomingFileState() }
    }
