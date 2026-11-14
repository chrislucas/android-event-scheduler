package com.br.androideventscheduler.core.testing

import android.app.PendingIntent
import org.robolectric.shadows.ShadowAlarmManager

/**
 * O `PendingIntent` que o alarme agendado vai disparar.
 *
 * Único acesso ao campo deprecado `ScheduledAlarm.operation` (spec 21, R1.4): o Robolectric deprecou os campos
 * públicos de `ScheduledAlarm` em favor de getters, mas até a 4.18 não existe getter para `operation`
 * (`type` e `allowWhileIdle` já têm `getType()` e `isAllowWhileIdle()`). Remova este helper e o `@Suppress`
 * quando o Robolectric ganhar o acessor (ex.: `getOperation()`); o compilador com `allWarningsAsErrors` não
 * aponta isso sozinho, então confira a cada atualização do Robolectric.
 */
@Suppress("DEPRECATION")
fun ShadowAlarmManager.ScheduledAlarm.pendingOperation(): PendingIntent =
    checkNotNull(operation) {
        "Alarme sem PendingIntent (agendado com OnAlarmListener?)"
    }
