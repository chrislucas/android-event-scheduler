package com.br.androideventscheduler.core.di

/**
 * Qualificadores dos dispatchers injetáveis (permitem trocar por dispatchers de teste).
 *
 * Regra: **ViewModels recebem [MAIN]** (`Main.immediate`) — estado e efeitos nascem na thread principal,
 * onde Toast, navegação e Snackbar precisam rodar. I/O e CPU pesada ficam nos repositórios/use cases,
 * que trocam para [IO]/[DEFAULT] internamente.
 */
object DispatcherNames {
    const val IO = "io"
    const val DEFAULT = "default"
    const val MAIN = "main"
}
