package com.br.androideventscheduler.core.di

/** Qualificadores dos dispatchers injetáveis (permitem trocar por dispatchers de teste). */
object DispatcherNames {
    const val IO = "io"
    const val DEFAULT = "default"
    const val MAIN = "main"
}
