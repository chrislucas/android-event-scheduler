package com.br.androideventscheduler.feature.backup.domain.model

/** Destino da exportação. A [uri] é texto para o domínio não conhecer `android.net.Uri`. */
sealed interface ExportTarget {
    /** Arquivo temporário em `cacheDir/exports/`, entregue por `FileProvider` ao share sheet. */
    data object ShareCache : ExportTarget

    /** Documento escolhido pelo usuário no seletor do sistema (SAF, `CreateDocument`). */
    data class Document(
        val uri: String,
    ) : ExportTarget
}

/** Arquivo gerado: [uri] para compartilhar/gravar, [fileName] sugerido e quantos itens foram exportados. */
data class ExportResult(
    val uri: String,
    val fileName: String,
    val itemCount: Int,
)
