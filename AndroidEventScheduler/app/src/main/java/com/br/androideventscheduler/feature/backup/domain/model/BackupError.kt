package com.br.androideventscheduler.feature.backup.domain.model

/** Motivos de falha ao exportar ou ler um arquivo. A apresentação mapeia cada um para um texto; nunca exibe `message`. */
sealed class BackupError : Exception() {
    /** JSON válido, mas não é um backup do RememberMe (`format` ausente ou diferente). */
    data object NotRememberMeFile : BackupError()

    /** Arquivo de uma versão mais nova do formato que este app não entende. */
    data class UnsupportedVersion(
        val version: Int,
    ) : BackupError()

    /** Não é JSON, está truncado ou o cabeçalho está malformado. */
    data object Corrupted : BackupError()

    /** Acima de 5 MB. */
    data object TooLarge : BackupError()

    /** Arquivo válido, mas sem nenhum agendamento a importar. */
    data object EmptyAgenda : BackupError()

    /** Falha de leitura/gravação (provedor indisponível, disco cheio, URI sem permissão...). */
    data object Io : BackupError()
}
