package com.br.androideventscheduler.feature.upsert.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.br.androideventscheduler.core.navigation.UpsertSchedule
import com.br.androideventscheduler.feature.upsert.presentation.screen.UpsertScheduleScreen

/** API pública da feature: registra a rota [UpsertSchedule] (criar sem `scheduleId`, editar com ele). */
fun EntryProviderScope<NavKey>.upsertEntry(onBack: () -> Unit) {
    entry<UpsertSchedule> { key ->
        UpsertScheduleScreen(
            scheduleId = key.scheduleId,
            initialDateEpochDay = key.initialDateEpochDay,
            onBack = onBack,
        )
    }
}
