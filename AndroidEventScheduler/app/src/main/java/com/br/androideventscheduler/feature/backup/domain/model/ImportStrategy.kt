package com.br.androideventscheduler.feature.backup.domain.model

/** O que fazer com itens do arquivo cujo `id` já existe na agenda (R5.5). */
enum class ImportStrategy {
    /** Padrão: o item do arquivo substitui o atual (idempotente ao reimportar o mesmo arquivo). */
    REPLACE_EXISTING,

    /** Importa só os itens novos; os que já existem ficam como estão. */
    KEEP_EXISTING,

    /** Os que já existem entram como cópias: novo `id` e título com sufixo. */
    IMPORT_AS_COPIES,
}
