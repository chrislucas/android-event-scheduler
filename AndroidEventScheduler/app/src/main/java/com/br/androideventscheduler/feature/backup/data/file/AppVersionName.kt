package com.br.androideventscheduler.feature.backup.data.file

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

private const val UNKNOWN_VERSION = "unknown"

/** `versionName` do app, gravado no cabeçalho do arquivo exportado (informativo). */
@Suppress("DEPRECATION")
internal fun appVersionName(context: Context): String =
    try {
        val packageManager = context.packageManager
        val info =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                packageManager.getPackageInfo(context.packageName, 0)
            }
        info.versionName ?: UNKNOWN_VERSION
    } catch (_: PackageManager.NameNotFoundException) {
        UNKNOWN_VERSION
    }
