package com.br.androideventscheduler.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.br.androideventscheduler.core.navigation.AgendaBackup
import com.br.androideventscheduler.core.navigation.Splash

// Sem preview: efeito de navegação sem UI própria.

/**
 * Abre a tela de backup já na pré-visualização do arquivo recebido: empilha `AgendaBackup(uri)` por cima da tela
 * atual, exceto durante a Splash (o arquivo espera a Home). Devolve `true` quando a URI foi usada (e deve ser consumida).
 */
internal fun MutableList<NavKey>.openPendingAgendaFile(pendingFileUri: String?): Boolean {
    val top = lastOrNull()
    if (pendingFileUri == null || top == null || top == Splash) return false
    add(AgendaBackup(pendingImportUri = pendingFileUri))
    return true
}

/** Observa o [IncomingFileState] e aplica [openPendingAgendaFile] sempre que a URI ou o topo da pilha mudam. */
@Composable
internal fun IncomingFileEffect(
    backStack: MutableList<NavKey>,
    incomingFileState: IncomingFileState,
) {
    val pendingFileUri by incomingFileState.pendingFileUri.collectAsStateWithLifecycle()
    val currentTop = backStack.lastOrNull()
    LaunchedEffect(pendingFileUri, currentTop) {
        val fileUri = pendingFileUri
        if (fileUri != null && backStack.openPendingAgendaFile(fileUri)) {
            incomingFileState.consume(fileUri)
        }
    }
}
