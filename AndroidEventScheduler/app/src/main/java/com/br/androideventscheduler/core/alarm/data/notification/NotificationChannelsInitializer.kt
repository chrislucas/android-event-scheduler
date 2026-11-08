package com.br.androideventscheduler.core.alarm.data.notification

import android.content.Context
import androidx.startup.Initializer

/** Cria os canais de notificação na inicialização do app (App Startup, antes de Activities e receivers). */
class NotificationChannelsInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        NotificationChannels.ensureCreated(context.applicationContext)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
