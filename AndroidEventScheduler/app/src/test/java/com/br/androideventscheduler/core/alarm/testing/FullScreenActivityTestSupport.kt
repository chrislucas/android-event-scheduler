package com.br.androideventscheduler.core.alarm.testing

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import com.br.androideventscheduler.core.alarm.data.AlarmIntents
import org.robolectric.Shadows.shadowOf

/** Simula a atividade de tela cheia (spec 14) instalada: o `resolveActivity` da ação passa a encontrá-la. */
fun installFullScreenActivity(context: Context) {
    val resolveInfo =
        ResolveInfo().apply {
            activityInfo =
                ActivityInfo().apply {
                    packageName = context.packageName
                    name = "com.br.androideventscheduler.FakeFullScreenActivity"
                }
        }
    shadowOf(context.packageManager).addResolveInfoForIntent(AlarmIntents.fullScreenIntent(context, "any"), resolveInfo)
}
