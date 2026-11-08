package com.br.androideventscheduler.feature.backup.domain.model

import java.time.LocalDate

/** Nome do arquivo exportado: `rememberme-agenda-AAAA-MM-DD.json` (data local da exportação). */
object BackupFileName {
    private const val PREFIX = "rememberme-agenda-"
    private const val SUFFIX = ".json"

    fun forDate(date: LocalDate): String = "$PREFIX$date$SUFFIX"
}
