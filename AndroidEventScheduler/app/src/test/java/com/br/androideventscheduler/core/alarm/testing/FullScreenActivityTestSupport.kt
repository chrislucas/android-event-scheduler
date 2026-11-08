package com.br.androideventscheduler.core.alarm.testing

import android.content.ComponentName
import android.content.Context
import com.br.androideventscheduler.feature.alarm.presentation.screen.FullScreenAlarmActivity
import org.robolectric.Shadows.shadowOf

/**
 * A `FullScreenAlarmActivity` (spec 14) já está no manifest mesclado, então o `resolveActivity` da ação a encontra.
 * Este helper a remove do `PackageManager` simulado para testar a degradação "atividade não instalada".
 */
fun uninstallFullScreenActivity(context: Context) {
    shadowOf(context.packageManager).removeActivity(ComponentName(context, FullScreenAlarmActivity::class.java))
}
