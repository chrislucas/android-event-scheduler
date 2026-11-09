package com.br.androideventscheduler.feature.backup.data.codec

/** Constantes do formato de arquivo e limites de segurança aplicados a arquivos não confiáveis (R4.3, R7.3). */
internal object AgendaFileFormat {
    const val FORMAT_ID = "rememberme-agenda"
    const val CURRENT_VERSION = 1
    const val MIME_TYPE = "application/json"

    /** Acima disso o arquivo é recusado antes de ser lido por completo. */
    const val MAX_FILE_BYTES = 5L * 1024 * 1024

    const val MAX_ID_LENGTH = 100
    const val MAX_TITLE_LENGTH = 200
    const val MAX_LOCATION_LENGTH = 200
    const val MAX_NOTES_LENGTH = 4000
    const val MAX_STICKER_LENGTH = 16

    /** Aninhamento máximo de objetos/listas aceito no JSON (o formato real usa 5); protege o parser recursivo. */
    const val MAX_NESTING_DEPTH = 16

    /** Valores + contêineres JSON aceitos (um arquivo real de 5 MB tem cerca de 250 mil); limita a memória da árvore. */
    const val MAX_JSON_NODES = 1_000_000

    /** Janela de anos aceita nas datas; evita valores absurdos que sobrecarregariam a expansão de recorrência. */
    const val MIN_YEAR = 1900
    const val MAX_YEAR = 2200
}
