package com.br.androideventscheduler.feature.upsert.presentation.component

/** Atalho rápido: [id] estável (compõe a tag), [label] já formatado e [onClick]. */
internal data class QuickChip(
    val id: String,
    val label: String,
    val onClick: () -> Unit,
)
