package com.br.androideventscheduler.feature.backup.data.file

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File

/**
 * Arquivos temporários de compartilhamento em `cacheDir/exports/` (a **única** pasta exposta pelo `FileProvider`,
 * ver `res/xml/file_paths.xml`). A cada nova exportação os arquivos anteriores são apagados (R3.5).
 */
internal class ExportFileStore(
    private val context: Context,
) {
    private val exportsDir: File get() = File(context.cacheDir, EXPORTS_DIR_NAME)

    /** Grava [content] em [fileName] (nome sem diretórios) e devolve a URI do `FileProvider`, com leitura concedida por URI. */
    fun writeShareFile(
        fileName: String,
        content: String,
    ): String {
        require(fileName == File(fileName).name) { "fileName must not contain directories" }
        clear()
        val dir = exportsDir.also { it.mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content, Charsets.UTF_8)
        return FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file).toString()
    }

    /** Apaga os arquivos exportados anteriormente. */
    fun clear() {
        exportsDir.listFiles()?.forEach { it.delete() }
    }

    companion object {
        const val EXPORTS_DIR_NAME = "exports"
        const val AUTHORITY_SUFFIX = ".fileprovider"
    }
}
