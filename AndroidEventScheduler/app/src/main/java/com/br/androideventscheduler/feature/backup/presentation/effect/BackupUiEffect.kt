package com.br.androideventscheduler.feature.backup.presentation.effect

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

internal sealed interface BackupUiEffect {
    /** Abre o share sheet com o arquivo em [uri] (`FileProvider`), [subject] e [body] já formatados. */
    data class ShareFile(
        val uri: String,
        val subject: String,
        val body: String,
    ) : BackupUiEffect

    /** Abre o seletor do sistema para criar o documento (`ACTION_CREATE_DOCUMENT`). */
    data class LaunchCreateDocument(
        val suggestedName: String,
    ) : BackupUiEffect

    /** Abre o seletor do sistema para escolher um arquivo (`ACTION_OPEN_DOCUMENT`). */
    data object LaunchOpenDocument : BackupUiEffect

    data class ShowMessage(
        @param:StringRes val messageRes: Int,
    ) : BackupUiEffect

    /** Mensagem com contagem (`plurals`): o texto muda conforme [quantity]. */
    data class ShowQuantityMessage(
        @param:PluralsRes val messageRes: Int,
        val quantity: Int,
    ) : BackupUiEffect

    data object NavigateBack : BackupUiEffect
}
