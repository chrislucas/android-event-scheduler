package com.br.androideventscheduler.core.security.data

import android.app.KeyguardManager
import android.content.Context
import com.br.androideventscheduler.core.security.domain.DeviceSecurityChecker

/** `KeyguardManager.isDeviceSecure` (API 23+). Relido a cada chamada: o usuário pode configurar ou remover o PIN a qualquer momento. */
internal class KeyguardDeviceSecurityChecker(
    private val context: Context,
) : DeviceSecurityChecker {
    override fun isDeviceSecure(): Boolean = context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
}
