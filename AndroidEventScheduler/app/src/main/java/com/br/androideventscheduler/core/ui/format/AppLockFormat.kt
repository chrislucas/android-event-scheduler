package com.br.androideventscheduler.core.ui.format

import android.content.res.Resources
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.security.domain.AppLockTimeout

/** Rótulo do tempo de bloqueio exibido em Configurações (o domínio não conhece `R`). */
fun AppLockTimeout.toDisplayLabel(resources: Resources): String =
    resources.getString(
        when (this) {
            AppLockTimeout.IMMEDIATE -> R.string.settings_app_lock_timeout_immediate
            AppLockTimeout.ONE_MINUTE -> R.string.settings_app_lock_timeout_one_minute
            AppLockTimeout.FIVE_MINUTES -> R.string.settings_app_lock_timeout_five_minutes
        },
    )
