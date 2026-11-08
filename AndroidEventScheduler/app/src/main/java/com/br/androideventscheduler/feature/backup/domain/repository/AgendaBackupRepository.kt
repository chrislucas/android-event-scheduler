package com.br.androideventscheduler.feature.backup.domain.repository

import com.br.androideventscheduler.feature.backup.domain.model.BackupError
import com.br.androideventscheduler.feature.backup.domain.model.ExportResult
import com.br.androideventscheduler.feature.backup.domain.model.ExportTarget
import com.br.androideventscheduler.feature.backup.domain.model.ImportPreview

/** Exporta a agenda para arquivo e lê/valida arquivos para importação. As funções lançam [BackupError]. */
interface AgendaBackupRepository {
    /** Quantos agendamentos existem hoje (agenda vazia desabilita a exportação, R3.3). */
    suspend fun countItems(): Int

    /** Gera o arquivo da agenda inteira em [target]. */
    suspend fun export(target: ExportTarget): ExportResult

    /**
     * Lê o arquivo em [uri] (limite de 5 MB), valida por completo e conta novos/existentes/inválidos.
     * Não grava nada: a gravação é do `ImportAgendaUseCase`, depois da confirmação.
     */
    suspend fun readForImport(uri: String): ImportPreview
}
